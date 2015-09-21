/*
 * Copyright 2013-2014 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 22.11.2014 by mikesh
 */


package es.unav.oncofuse.legacy

import groovyx.gpars.GParsPool

import java.util.concurrent.atomic.AtomicInteger

def A_DEFAULT = "hg19", A_ALLOWED = ["hg18", "hg19", "hg38"]

def cli = new CliBuilder(usage:
        "Oncofuse.jar [options] input_file input_type [tissue_type or -] output_file\n" +
                "Supported input types: " +
                "coord, fcatcher, fcatcher-N-M, tophat, tophat-N-M, tophat-post, rnastar, rnastar-N-M\n" +
                "Running with input type args: " +
                "replace N by number of spanning reads and M by number of total supporting read pairs\n" +
                "Supported tissue types: " +
                "EPI, HEM, MES, AVG or -\n" +
                "Version 1.0.9b2, 6May2015\n")
cli.h("display help message")
cli.a(argName: "hgXX", args: 1, "Genome assembly version, default is $A_DEFAULT. " +
        "Allowed values: ${A_ALLOWED.join(", ")}")
cli.p(argName: "integer", args: 1, "Number of threads, uses all available processors by default")

cli.width = 100

def opt = cli.parse(args)

if (opt == null)
    System.exit(-1)

if (opt.h || opt.arguments().size() != 4) {
    cli.usage()
    System.exit(-1)
}

def inputFileName = opt.arguments()[0],
    inputType = opt.arguments()[1].toUpperCase(),
    tissueType = opt.arguments()[2],
    outputFileName = opt.arguments()[3]
def THREADS = (opt.p ?: "${Runtime.getRuntime().availableProcessors()}").toInteger()
def hgX = opt.a ?: A_DEFAULT

// General checks

if (!new File(inputFileName).exists()) {
    println "[ERROR] Input file not found, $inputFileName"
    System.exit(1)
}

if (!A_ALLOWED.any { it == hgX }) {
    println "[ERROR] Unrecognized genome assembly, $hgX"
    System.exit(1)
}

// Check if library is in place

def homeDir = new File(getClass().protectionDomain.codeSource.location.path).parent.replaceAll("%20", " ")
def canonicalTranscriptsFileName = "$homeDir/common/refseq_canonical.txt",
    refseqFileName = "$homeDir/common/refseq_full_${hgX}.txt"
def libFolderName = "$homeDir/libs"
def domainsFileName = "$homeDir/common/domains.txt", domainAnnotFileName = "$homeDir/common/interpro_annot.txt",
    piisFileName = "$homeDir/common/piis.txt"
def gene2DavidIdFileName = "$homeDir/common/gene_symbol2id.txt", davidIdExpandFileName = "$homeDir/common/id_expand.txt",
    davidId2GOIdFileName = "$homeDir/common/id2go.txt", ffasRangesFileName = "$homeDir/common/ffas_range.txt",
    go2themeFileName = "$homeDir/common/go2theme.txt"
def schemaFileName = "$homeDir/common/nbn_schema", classifierFileName = "$homeDir/common/class"

def missing = [canonicalTranscriptsFileName, refseqFileName, libFolderName,
               domainsFileName, domainAnnotFileName, piisFileName,
               gene2DavidIdFileName, davidIdExpandFileName, davidId2GOIdFileName, ffasRangesFileName, go2themeFileName,
               schemaFileName, classifierFileName].findAll { !new File(it).exists() }

if (missing.size() > 0) {
    println "[ERROR] The following resources are missing:\n"
    println missing.join("\n")
    System.exit(1)
}

// Tissue type checks

def libs = new File(libFolderName).listFiles().findAll { it.isDirectory() && !it.isHidden() }.collect { it.name }

def allowedTissueTypes = [libs.collect { it.toUpperCase() }, '-'].flatten()
if (!allowedTissueTypes.any { tissueType == it }) {
    println "[ERROR] Unrecognized tissue type, $tissueType. " +
            "Allowed tissue types are: ${allowedTissueTypes.join(", ")}"
    System.exit(1)
}

/////////////////////////////////////////
// LOADING BKPT POSITIONS AND MAPPING //
///////////////////////////////////////
println "[${new Date()}] Loading RefSeq data, assuming $hgX"

// Data for mapping
// Refseq canonical data schema
// 0        1       2       3       4       5           6       7           8           9
// #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonStarts	exonEnds	name2
def exonStartCoords = new HashMap<String, List<Integer>>(), exonEndCoords = new HashMap<String, List<Integer>>()
def gene2Strand = new HashMap<String, Integer>()
def geneByChrom = new HashMap<String, List<String>>()
def transcriptStartCoords = new HashMap<String, Integer>(), transcriptEndCoords = new HashMap<String, Integer>()
def cdsStartCoords = new HashMap<String, Integer>(), cdsEndCoords = new HashMap<String, Integer>()

def canonicalTranscripts = new HashSet<String>(new File(canonicalTranscriptsFileName).readLines())
def gene2chr = new HashMap<String, String>()

def convertStrand = { String strand ->
    switch (strand.toLowerCase()) {
        case "+":
        case "f":
            return 1
        case "-":
        case "r":
            return -1
        default:
            return 0
    }
}

new File(refseqFileName).splitEachLine('\t') { splitLine ->
    if (canonicalTranscripts.contains(splitLine[0])) {
        def geneName = splitLine[9]
        int cdsStart = Integer.parseInt(splitLine[5]), cdsEnd = Integer.parseInt(splitLine[6])
        if (cdsEnd > cdsStart) {
            exonStartCoords.put(geneName, ((String) splitLine[7]).split(',').collect { Integer.parseInt(it) }.asList())
            exonEndCoords.put(geneName, ((String) splitLine[8]).split(',').collect { Integer.parseInt(it) }.asList())
            def chrom = splitLine[1]
            def geneList = geneByChrom[splitLine[1]]
            if (geneList == null)
                geneList = new ArrayList<String>()
            geneList.add(geneName)
            geneByChrom.put(chrom, geneList)
            gene2chr.put(geneName, chrom)
            transcriptStartCoords.put(geneName, Integer.parseInt(splitLine[3]))
            transcriptEndCoords.put(geneName, Integer.parseInt(splitLine[4]))
            cdsStartCoords.put(geneName, cdsStart)
            cdsEndCoords.put(geneName, cdsEnd)
            gene2Strand.put(geneName, convertStrand(splitLine[2]))
        }
    }
}

// Routines for mapping
def fetchGene = { String chrom, int coord ->
    def geneList = geneByChrom[chrom]
    def gene = geneList.find {
        transcriptStartCoords.get(it) <= (coord + 1) &&
                transcriptEndCoords.get(it) >= (coord - 1) // +/-1 is to allow "empty" fpgs
    }
    gene
}

// Map from coord to gene segment & segment-based coord
def map = { Boolean fivePrime, String gene, Integer coord ->
    // Exon coords are given in + strand. So first exon in gene in (-) orientation will be last by coords
    // and last in the list.
    def exonsStarts = exonStartCoords[gene]
    if (exonsStarts == null || exonsStarts.size() == 0)
        return null
    def exonsEnds = exonEndCoords[gene]
    def cdsStart = cdsStartCoords[gene], cdsEnd = cdsEndCoords[gene]

    int n = exonsStarts.size()
    boolean s = gene2Strand[gene] > 0
    def exonLen = { int exonId ->
        Math.max(0, Math.min(exonsEnds[exonId], cdsEnd) - Math.max(exonsStarts[exonId], cdsStart))
    }
    def truncExonLen = { int exonId ->
        s ? Math.max(0, Math.min(Math.min(cdsEnd, exonsEnds[exonId]), coord - 1) - Math.max(exonsStarts[exonId], cdsStart)) : // bkpt marks first nt deleted, so -1 for 5' and +1 for 3'
                Math.max(0, Math.min(exonsEnds[exonId], cdsEnd) - Math.max(Math.max(exonsStarts[exonId], cdsStart), coord + 1))
    }
    int fullLen = (int) (0..(n - 1)).collect { exonLen(it) }.sum(), fullAALen = fullLen / 3

    if (coord < cdsStart) {
        return new FpgPart(gene2chr[gene], coord, gene, fivePrime, false,
                -1, -1,
                s ? 0 : fullAALen,
                0, fullAALen)
    } else if (coord > cdsEnd) {
        return new FpgPart(gene2chr[gene], coord, gene, fivePrime, false,
                -1, -1,
                s ? fullAALen : 0,
                0, fullAALen)
    }

    for (int i = 0; i < n; i++) { // From head to tail
        if (coord <= exonsStarts[i]) // intron
        {
            int cc = s ? (int) (0..(i - 1)).collect { exonLen(it) }.sum() : (int) (i..(n - 1)).collect {
                exonLen(it)
            }.sum()
            return new FpgPart(gene2chr[gene], coord, gene, fivePrime, false,
                    s ? i : n - i,
                    s ? coord - exonsEnds[i - 1] : exonsStarts[i] - coord,
                    (int) (cc / 3), fivePrime ? cc % 3 : (fullLen - cc) % 3, fullAALen)
        } else if (coord >= exonsStarts[i] && coord <= exonsEnds[i]) // exon
        {
            int cc = (s ?
                    (int) (i > 0 ? (0..(i - 1)).collect { exonLen(it) }.sum() : 0) :
                    (int) (i < n - 1 ? ((i + 1)..(n - 1)).collect { exonLen(it) }.sum() : 0)) +
                    truncExonLen(i)
            return new FpgPart(gene2chr[gene], coord, gene, fivePrime, true,
                    s ? i + 1 : n - i,
                    s ? coord - exonsStarts[i] : exonsEnds[i] - coord,
                    (int) (cc / 3), fivePrime ? cc % 3 : (fullLen - cc) % 3, fullAALen)
        }
    }
}

// Read and convert to common format
// Input data format:
// chr1 coord1 chr2 coord2 tissueType sampleName strand1 strand2 nSpan nSupport
def inputData = new ArrayList<String>()

println "[${new Date()}] Reading input file, assuming $inputType format"

def inputTypeArgs = inputType.split("-")

inputType = inputTypeArgs[0]

int minSpan = 1, minSum = 2

if (inputTypeArgs.length == 3) {
    minSpan = Integer.parseInt(inputTypeArgs[1])
    minSum = Integer.parseInt(inputTypeArgs[2])
}

switch (inputType) {
    case 'FCATCHER':
        def inputFile = new File(inputFileName)
        inputFile.splitEachLine("\t") { line ->
            try {
                int nSpan = Integer.parseInt(line[5]), nSupport = Integer.parseInt(line[4])
                if (nSpan >= minSpan && (nSpan + nSupport) >= minSum) {
                    def chrLine1 = line[8].split(":"), chrLine2 = line[9].split(":")
                    inputData.add(["chr" + chrLine1[0..1].collect { it.trim() }.join("\t"),
                                   "chr" + chrLine2[0..1].collect { it.trim() }.join("\t"),
                                   tissueType, inputFileName,
                                   convertStrand(chrLine1[2]), convertStrand(chrLine2[2]),
                                   nSpan, nSupport].join("\t"))
                }
            } catch (Exception e) {
                println "Ignoring line ${line.join("\t")}"
            }
        }
        break

    case 'TOPHAT':
        def inputFile = new File(inputFileName)

        if (inputTypeArgs.length == 2 && inputTypeArgs[1] == 'post') {
            inputFile.splitEachLine("[ \t]") { line ->
                try {
                    if (line.size() > 3 && line[1].startsWith("chr")) {
                        def chrs = line[1].split("-")
                        inputData.add([chrs[0], line[2], chrs[1], line[3],
                                       tissueType, line[0],
                                       convertStrand(line[4][0]), convertStrand(line[4][1]),
                                       0, 0].join("\t"))
                    }
                }
                catch (Exception e) {
                    //println "Ignoring line ${line.join("\t")}" too many lines to ignore
                }
            }
        } else
            inputFile.splitEachLine("\t") { line ->
                try {
                    def chrs = line[0].split("-")
                    int nSpan = Integer.parseInt(line[4]), nSupport = Integer.parseInt(line[5])
                    if (nSpan >= minSpan && (nSpan + nSupport) >= minSum)
                        inputData.add([chrs[0], line[1], chrs[1], line[2], tissueType, inputFileName,
                                       convertStrand(line[3][0]), convertStrand(line[3][1]),
                                       nSpan, nSupport].join("\t"))
                }
                catch (Exception e) {
                    println "Ignoring line ${line.join("\t")}"
                }
            }
        break

    case 'RNASTAR':
        def inputFile = new File(inputFileName)

        def sign2counter = new HashMap<String, int[]>()
        def sign2coord = new HashMap<String, String>()
        inputData = inputFile.readLines()

        def n = 0, k = 0, m = 0
        inputData.each { String line ->
            splitLine = line.split("\t")

            // Here we collapse reads
            try {
                int type = Integer.parseInt(splitLine[6])
                String chrom1 = splitLine[0], chrom2 = splitLine[3]
                int coord1 = Integer.parseInt(splitLine[1]), coord2 = Integer.parseInt(splitLine[4])
                int strand1 = convertStrand(splitLine[2]), strand2 = convertStrand(splitLine[5])

                String gene1 = fetchGene(chrom1, coord1)
                String gene2 = fetchGene(chrom2, coord2)

                if (gene1 != null && gene2 != null) {
                    def map1 = map(false, gene1, coord1 - strand1),
                        map2 = map(false, gene2, coord2 + strand1)
                    if (map1 != null && map1.exon && map2 != null && map2.exon) {
                        exon1 = map1.segmentId
                        exon2 = map2.segmentId
                        String signature = [gene1, exon1, gene2, exon2].join("\t")

                        def counter
                        sign2counter.put(signature, counter = (sign2counter[signature] ?: new int[2]))

                        if (type < 0) {
                            counter[1]++
                            k++
                        } else {
                            sign2coord.put(signature, [chrom1, coord1, chrom2, coord2,
                                                       tissueType, inputFileName,
                                                       strand1, strand2].join("\t"))
                            counter[0]++
                            m++
                        }
                    }
                }
                if (++n % 100000 == 0)
                    println "[${new Date()}] [Post-processing RNASTAR input] $n reads analyzed"
            }
            catch (Exception e) {
                println "Ignoring line ${splitLine.join("\t")}"
            }
        }

        inputData.clear()

        sign2counter.each {
            int nSpan = it.value[0].intValue(), nEncomp = it.value[1].intValue()
            if (nSpan >= minSpan && (nSpan + nEncomp) >= minSum && sign2coord[it.key] != null)
                inputData.add(sign2coord[it.key] + "\t" + nSpan + "\t" + nEncomp)
        }
        println "[${new Date()}] [Post-processing RNASTAR input] Taken $n reads, of them mapped to canonical RefSeq: $k as encompassing, $m as spanning. Total ${sign2coord.size()} exon pairs, of them ${inputData.size()} passed junction coverage filter."
        break

    case 'COORD':
        def coordMap = new HashMap<String, int[]>()
        def inputFile = new File(inputFileName)
        inputFile.splitEachLine("\t") { List<String> splitLine ->
            if (!splitLine[0].startsWith("#")) {
                def signature = splitLine[0..4].join("\t") + "\t" + inputFileName
                if (splitLine.size() > 6)
                    signature += "\t" + convertStrand(splitLine[5]) +
                            "\t" + convertStrand(splitLine[6])
                else
                    signature += "\t0\t0"

                def counters = coordMap[signature]
                if (counters == null)
                    coordMap.put(signature, counters = new int[2])

                if (splitLine.size() > 8) {
                    counters[0] += splitLine[7].toInteger()
                    counters[1] += splitLine[8].toInteger()
                }
            }
        }
        coordMap.each {
            inputData.add(it.key + "\t" + it.value.collect().join("\t"))
        }
        break

    default:
        println "[ERROR] Unknown input format, $inputType"
        System.exit(-1)
}
if (inputData.size() == 0) {
    println "[WARNING] No valid fusions in input file!"
    //System.exit(-1)
}

// Make a list of fusions
println "[${new Date()}] Mapping breakpoints to known genes (this is going to filter a lot)"
def i = 0, j = 0
def fusionList = new ArrayList<Fusion>()
def tissue2FpgMap = new HashMap<String, Set<FpgPart>>()
def fpgSet = new HashSet<FpgPart>()
int ff = 0
int m1 = 0, m2 = 0, m3 = 0, m4 = 0
int FAILURES_TO_REPORT = 20
inputData.each { line ->
    j++
    def splitLine = line.split("\t").collect { it.trim() }

    def coord5 = Integer.parseInt(splitLine[1]), coord3 = Integer.parseInt(splitLine[3])
    def tissue = splitLine[4].toUpperCase(),
        sample = splitLine[5]
    def strand1 = splitLine[6].toInteger(),
        strand2 = splitLine[7].toInteger()
    def nSpan = splitLine[8].toInteger(),
        nEncomp = splitLine[9].toInteger()

    def gene5, gene3
    def fpgPart5, fpgPart3

    if (!(gene5 = fetchGene(splitLine[0], coord5)) || !(fpgPart5 = map(true, gene5, coord5))) {
        if (ff++ < FAILURES_TO_REPORT)
            println "[${new Date()}] FILTER (reporting first $FAILURES_TO_REPORT only): " +
                    "Not mapped to any acceptable transcript: 5' \"${splitLine[0]}:${coord5}\" at fusion #${j} in input"
        m1++
    }

    if (!(gene3 = fetchGene(splitLine[2], coord3)) || !(fpgPart3 = map(false, gene3, coord3))) {
        println gene3
        println fpgPart3
        if (ff++ < FAILURES_TO_REPORT)
            println "[${new Date()}] FILTER (reporting first $FAILURES_TO_REPORT only): " +
                    "Not mapped to any acceptable transcript: 3' \"${splitLine[2]}:${coord3}\" at fusion #${j} in input"
        m2++
    }

    if (gene5 == gene3) {
        if (ff++ < FAILURES_TO_REPORT)
            println "[${new Date()}] FILTER (reporting first $FAILURES_TO_REPORT only): " +
                    "Same gene for 5' and 3' FPG at fusion #${j} in input"
        fpgPart5 = null
        m3++
    }

    if (strand1 != 0 && strand2 != 0 &&
            ((gene2Strand[gene5] == strand1) != (gene2Strand[gene3] == strand2))) {
        if (ff++ < 20)
            println "[${new Date()}] FILTER (reporting first 20 only): Head to head / tail to tail #${j} in input"
        fpgPart5 = null
        m4++
    }

    if (fpgPart5 && fpgPart3) {
        fusionList.add(new Fusion(j, nSpan, nEncomp, fpgPart5, fpgPart3, tissue, sample))
        def tFpgSet = tissue2FpgMap[tissue]
        if (tFpgSet == null)
            tissue2FpgMap.put(tissue, tFpgSet = new HashSet<FpgPart>())
        tFpgSet.add(fpgPart5)
        tFpgSet.add(fpgPart3)
        fpgSet.add(fpgPart5)
        fpgSet.add(fpgPart3)
        i++
    }
}
println "[${new Date()}] $i fusions mapped out of $j. Dropped fusion candidates: " +
        "$m1 - 5'FPG not mapped, $m2 - 3'FPG not mapped, $m3 - mapped to same gene, $m4 - discordant FPG directions"

////////////////////////
// FEATURES FOR FPGS //
//////////////////////
println "[${new Date()}] Getting features for FPG parts"

// Expression data
println "[${new Date()}] Loading expression-related data"
def promData = new HashMap<String, Map<String, List<Double>>>(),
    exprData = new HashMap<String, Map<String, Double>>(),
    utrData = new HashMap<String, Map<String, List<Double>>>()
libs.each { lib ->
    lib = lib.toUpperCase()
    def promFileName = "$homeDir/libs/$lib/prom.txt",
        exprFileName = "$homeDir/libs/$lib/expr.txt",
        utrFileName = "$homeDir/libs/$lib/utr.txt"

    missing = [promFileName, exprFileName, utrFileName].findAll { !new File(it).exists() }

    if (missing.size() > 0) {
        println "[ERROR] Library $lib is incomplete, the following files are missing:\n"
        println missing.join("\n")
        System.exit(-1)
    }

    promData.put(lib, new HashMap<String, List<Double>>())
    new File(promFileName).splitEachLine("\t") { List<String> line ->
        promData[lib].put(line[0], line[1..(line.size() - 1)].collect { Double.parseDouble(it) })
    }
    exprData.put(lib, new HashMap<String, Double>())
    new File(exprFileName).splitEachLine("\t") { List<String> line ->
        exprData[lib].put(line[0], Double.parseDouble(line[1]))
    }
    utrData.put(lib, new HashMap<String, List<Double>>())
    new File(utrFileName).splitEachLine("\t") { List<String> line ->
        utrData[lib].put(line[0], line[1..(line.size() - 1)].collect { Double.parseDouble(it) })
    }
}
int nPromFeatures = promData.iterator().next().value.iterator().next().value.size(),
    nUTRFeatures = utrData.iterator().next().value.iterator().next().value.size()

// Domain and PII data
println "[${new Date()}] Loading domain and protein interaction interface-related data"

def domainData = new HashMap<String, List<Feature>>(), piiData = new HashMap<String, List<Feature>>()
def domain2genes = new HashMap<String, List<String>>() // for GO mapping
new File(domainsFileName).splitEachLine("\t") { line ->
    def features = domainData[line[0]]
    if (features == null)
        domainData.put(line[0], features = new ArrayList<Feature>())
    features.add(new Feature(line[0], Integer.parseInt(line[2]), Integer.parseInt(line[3]), line[1]))

    def geneList = domain2genes[line[1]]
    if (geneList == null)
        domain2genes.put(line[1], geneList = new ArrayList<String>())
    geneList.add(line[0])
}
new File(piisFileName).splitEachLine("\t") { line ->
    def features = piiData[line[0]]
    if (features == null)
        piiData.put(line[0], features = new ArrayList<Feature>())
    features.add(new Feature(line[0], Integer.parseInt(line[2]), Integer.parseInt(line[3]), line[1]))
}
def domainAnnotation = new HashMap<String, String>()
new File(domainAnnotFileName).splitEachLine("\t") { line ->
    domainAnnotation.put(line[0], line[1] + "[" + line[2] + "]")
}

/////////////////////////
// Gene ontology data //
///////////////////////
println "[${new Date()}] Loading gene ontology data (time-consuming)"

// Id as in DAVID
def davidId2GeneMap = new HashMap<String, String>()
new File(gene2DavidIdFileName).splitEachLine("\t") { line ->
    davidId2GeneMap.put(line[1], line[0]) // !
}

// A list to expand Id set by homology
def davidIdExpandMap = new HashMap<String, Set<String>>()
new File(davidIdExpandFileName).splitEachLine("\t") { line ->
    def geneSet = davidIdExpandMap[line[0]]
    if (geneSet == null)
        davidIdExpandMap.put(line[0], geneSet = new HashSet<String>())
    geneSet.add(line[1])

    geneSet = davidIdExpandMap[line[1]]
    if (geneSet == null)
        davidIdExpandMap.put(line[1], geneSet = new HashSet<String>())
    geneSet.add(line[0])
}

// Gene symbol -> GO category
def gene2GOSetMap = new HashMap<String, Set<String>>()
new File(davidId2GOIdFileName).splitEachLine("\t") { line ->
    String geneName = davidId2GeneMap[line[0]]
    def expandedGenes = davidIdExpandMap[line[0]].collect { davidId2GeneMap[it] }
    [geneName, expandedGenes].flatten().each { String gene ->
        def goSet = gene2GOSetMap[gene]
        if (goSet == null)
            gene2GOSetMap.put(gene, goSet = new HashSet<String>())
        goSet.add(line[1])
    }
}

// FFAS: theme ids and ranges
def ffasRangesRaw = new File(ffasRangesFileName).readLines()
def theme2IdMap = new HashMap<String, Integer>()
def themeNames = new ArrayList<String>()
ffasRangesRaw[0].split("\t").eachWithIndex { it, ind ->
    theme2IdMap.put(it, ind)
    themeNames.add(it)
}
int nThemes = theme2IdMap.size()
def ffasRanges = ffasRangesRaw[1].split("\t").collect { Double.parseDouble(it) }

// GO -> FFAS theme -> FFAS themeId
def go2ThemeMap = new HashMap<String, Integer>()
new File(go2themeFileName).splitEachLine("\t") { line ->
    go2ThemeMap.put(line[0], theme2IdMap[line[1]])
}

/////////////////////////
// Annotate FPG parts //
///////////////////////
println "[${new Date()}] Annotating FPG parts"

def tissue2fpg2TSFeaturesMap = new HashMap<String, Map<FpgPart, TissueSpecificFeatures>>()
def fpg2DomainFeaturesMap = Collections.synchronizedMap(new HashMap<FpgPart, DomainFeatures>())

println "[${new Date()}] =Stage #1: raw data"

println "[${new Date()}] ==Domain & PII related"
GParsPool.withPool THREADS, {
    fpgSet.eachParallel { FpgPart fpgPart ->
        def domainFeatures = new DomainFeatures()
        domainFeatures.domainProfileRetained = new double[nThemes]
        domainFeatures.domainProfileLost = new double[nThemes]
        domainFeatures.domainProfileBroken = new double[nThemes]
        def domains = domainData[fpgPart.geneName]
        domains.each {
            if ((it.aaTo <= fpgPart.aaPos) == fpgPart.fivePrimeFpg)
                domainFeatures.domainsRetained.add(it.featureId)
            else if ((it.aaFrom >= fpgPart.aaPos) == fpgPart.fivePrimeFpg)
                domainFeatures.domainsLost.add(it.featureId)
            else
                domainFeatures.domainsBroken.add(it.featureId)
        }
        def piis = piiData[fpgPart.geneName]
        piis.each {
            if ((it.aaTo <= fpgPart.aaPos) == fpgPart.fivePrimeFpg)
                domainFeatures.piisRetained.add(it.featureId)
            else if ((it.aaFrom >= fpgPart.aaPos) == fpgPart.fivePrimeFpg)
                domainFeatures.piisLost.add(it.featureId)
        }
        fpg2DomainFeaturesMap.put(fpgPart, domainFeatures)
    }
}

def promFeatsUndefList = (0..(nPromFeatures - 1)).collect { Double.NaN }.asList(),
    utrFeatsUndefList = (0..(nUTRFeatures - 1)).collect { Double.NaN }.asList()

println "[${new Date()}] ==Tissue specific"
tissue2FpgMap.each { Map.Entry<String, Set<FpgPart>> entry ->
    def tissue = entry.key
    def fpg2TSFeaturesMap = Collections.synchronizedMap(new HashMap<FpgPart, TissueSpecificFeatures>())

    GParsPool.withPool THREADS, {
        entry.value.eachParallel { FpgPart fpgPart ->
            def fpgTSFeatures = new TissueSpecificFeatures()
            if (promData[tissue] == null) {
                println "[${new Date()}] ERROR: bad (or no) tissue specified, \"$tissue\""
                System.exit(-1)
            }
            fpgTSFeatures.promFeatures = promData[tissue][fpgPart.geneName] ?: promFeatsUndefList
            fpgTSFeatures.utrFeatures = utrData[tissue][fpgPart.geneName] ?: utrFeatsUndefList
            fpg2TSFeaturesMap.put(fpgPart, fpgTSFeatures)
        }
    }
    tissue2fpg2TSFeaturesMap.put(tissue, fpg2TSFeaturesMap)
}

//

println "[${new Date()}] =Stage #2: some computations"

println "[${new Date()}] ==Domain & PII related (time-consuming)"
def counter = new AtomicInteger(0), cc = 0
GParsPool.withPool THREADS, {
    fpg2DomainFeaturesMap.eachParallel { Map.Entry<FpgPart, DomainFeatures> entry ->
        def geneName = entry.key.geneName
        def domainFeatures = entry.value
        domainFeatures.piisRetained.each {
            if (it == geneName)
                domainFeatures.nSelfPIIsRetained++
        }
        domainFeatures.nPIIsRetained = domainFeatures.piisRetained.size() - domainFeatures.nSelfPIIsRetained
        domainFeatures.piisLost.each {
            if (it == geneName)
                domainFeatures.nSelfPIIsLost++
        }
        domainFeatures.nPIIsLost = domainFeatures.piisLost.size() - domainFeatures.nSelfPIIsLost

        // Domain mapping FFAS
        // gene -> domains -> genes -> go categories for domain (weighted by gene set size per domain) -> theme
        ["Retained", "Lost", "Broken"].each { state ->
            domainFeatures."domains$state".each { domain ->
                def geneSet = domain2genes[domain] // genes with this domain
                if (geneSet != null) {
                    geneSet.each { gene ->
                        def goCategories = gene2GOSetMap[gene]  // categories with given gene
                        if (goCategories != null) {
                            goCategories.each { goCategory ->
                                def theme = go2ThemeMap[goCategory] // theme for the category
                                if (theme != null)
                                // increment, weighted to reduce bias for domains found in many genes
                                    domainFeatures."domainProfile$state"[theme] += 1.0 / geneSet.size()
                            }
                        }
                    }
                }
            }
        }

        if ((cc = counter.incrementAndGet()) % 1000 == 0)
            println "[${new Date()}] $cc FPG parts processed"
    }
}

println "[${new Date()}] ==Tissue specific"
tissue2fpg2TSFeaturesMap.each { tissueEntry ->
    def tissue = tissueEntry.key
    GParsPool.withPool THREADS, {
        tissueEntry.value.eachParallel { Map.Entry<FpgPart, TissueSpecificFeatures> entry ->
            def geneName = entry.key.geneName
            def tsFeatures = entry.value
            def domainFeatures = fpg2DomainFeaturesMap[entry.key]
            domainFeatures.piisRetained.each {
                if (it != geneName && exprData[tissue][it] != null)
                    tsFeatures.piiExprRetained += exprData[tissue][it]
            }
            if (domainFeatures.nPIIsRetained > 0)
                tsFeatures.piiExprRetained /= domainFeatures.nPIIsRetained
            domainFeatures.piisLost.each {
                if (it != geneName && exprData[tissue][it] != null)
                    tsFeatures.piiExprLost += exprData[tissue][it]
            }
            if (domainFeatures.nPIIsLost > 0)
                tsFeatures.piiExprLost /= domainFeatures.nPIIsLost
        }
    }
}

/////////////////////////
// Prepare classifier //
///////////////////////
println "[${new Date()}] Initializing classifier"
def classifier = new ClassifierWrapper(schemaFileName, classifierFileName)

// No parsing of schema, this is fixed to layout of oncofuseV3
def getFeatureVector = { Fusion fusion ->
    def tissueSpecificFeatures5 = tissue2fpg2TSFeaturesMap[fusion.tissue][fusion.fpgPart5],
        tissueSpecificFeatures3 = tissue2fpg2TSFeaturesMap[fusion.tissue][fusion.fpgPart3],
        domainSpecificFeatures5 = fpg2DomainFeaturesMap[fusion.fpgPart5],
        domainSpecificFeatures3 = fpg2DomainFeaturesMap[fusion.fpgPart3]

    [
            // Retained:
            tissueSpecificFeatures5.promFeatures,
            [
                    domainSpecificFeatures5.nSelfPIIsRetained + domainSpecificFeatures3.nSelfPIIsRetained,
                    domainSpecificFeatures5.nPIIsRetained + domainSpecificFeatures3.nPIIsRetained,
                    tissueSpecificFeatures5.piiExprRetained + tissueSpecificFeatures3.piiExprRetained,
                    (0..(nThemes - 1)).collect {
                        domainSpecificFeatures5.domainProfileRetained[it] +
                                domainSpecificFeatures3.domainProfileRetained[it]
                    }
            ].flatten().collect { Math.log10((Double) it + 1.0) },
            tissueSpecificFeatures3.utrFeatures,
            // Broken
            (0..(nThemes - 1)).collect {
                Math.log10((Double) domainSpecificFeatures5.domainProfileBroken[it] +
                        domainSpecificFeatures3.domainProfileBroken[it] + 1.0)
            },
            // Lost:
            tissueSpecificFeatures3.promFeatures,
            [
                    domainSpecificFeatures5.nSelfPIIsLost + domainSpecificFeatures3.nSelfPIIsLost,
                    domainSpecificFeatures5.nPIIsLost + domainSpecificFeatures3.nPIIsLost,
                    tissueSpecificFeatures5.piiExprLost + tissueSpecificFeatures3.piiExprLost,
                    (0..(nThemes - 1)).collect {
                        domainSpecificFeatures5.domainProfileLost[it] +
                                domainSpecificFeatures3.domainProfileRetained[it]
                    }
            ].flatten().collect { Math.log10((Double) it + 1.0) },
            tissueSpecificFeatures5.utrFeatures,
            0.0].flatten().collect { (Double) it }.asList()
}

///////////////////////
// Classify fusions //
/////////////////////
println "[${new Date()}] Classifying fusions"

// Run classifier
counter = new AtomicInteger(0)
def pValList = Collections.synchronizedList(new ArrayList<Double>())
def classifierResults = Collections.synchronizedMap(new HashMap<String, double[]>())
GParsPool.withPool THREADS, {
    fusionList.each { Fusion fusion ->
        double[] pp = classifier.getPValue(getFeatureVector(fusion))
        pValList.add(pp[0])
        classifierResults.put(fusion.id.toString(), pp)

        if ((cc = counter.incrementAndGet()) % 1000 == 0)
            println "[${new Date()}] $cc fusions processed"
    }
}

// Correct p-values
Collections.sort(pValList)
double n = pValList.size()
classifierResults.each { result ->
    double p = result.value[0]
    int k = Collections.binarySearch(pValList, p) + 1
    result.value[0] = Math.min(1.0, p * n / (double) k)
}

// Write results table
println "[${new Date()}] Writing output"
new File(outputFileName).withPrintWriter { pw ->
    // Header
    pw.println(["SAMPLE_ID", "FUSION_ID", "TISSUE",
                inputType == 'fcatcher' ? "SPANNING_READS_UNIQUE" : "SPANNING_READS",
                "ENCOMPASSING_READS",
                "GENOMIC",
                "5_FPG_GENE_NAME", "5_IN_CDS?", "5_SEGMENT_TYPE", "5_SEGMENT_ID",
                "5_COORD_IN_SEGMENT", "5_FULL_AA", "5_FRAME",
                "3_FPG_GENE_NAME", "3_IN_CDS?", "3_SEGMENT_TYPE", "3_SEGMENT_ID",
                "3_COORD_IN_SEGMENT", "3_FULL_AA", "3_FRAME",
                "FPG_FRAME_DIFFERENCE",
                "P_VAL_CORR", "DRIVER_PROB",
                "EXPRESSION_GAIN",
                "5_DOMAINS_RETAINED", "3_DOMAINS_RETAINED",
                "5_DOMAINS_BROKEN", "3_DOMAINS_BROKEN",
                "5_PII_RETAINED", "3_PII_RETAINED",
                themeNames
    ].flatten().join("\t"))

    // Body
    fusionList.each { Fusion fusion ->
        def p = classifierResults[(String) fusion.id]
        def domainSpecificFeatures5 = fpg2DomainFeaturesMap[fusion.fpgPart5],
            domainSpecificFeatures3 = fpg2DomainFeaturesMap[fusion.fpgPart3]
        Double expr5 = exprData[fusion.tissue][fusion.fpgPart5.geneName],
               expr3 = exprData[fusion.tissue][fusion.fpgPart3.geneName]
        pw.println([fusion.sample, fusion.id, fusion.tissue, fusion.nSpan, fusion.nEncomp,
                    fusion.fpgPart5.chrom + ":" + fusion.fpgPart5.chrCoord + ">" + fusion.fpgPart3.chrom + ":" + fusion.fpgPart3.chrCoord,
                    fusion.fpgPart5.toPrettyString(), fusion.fpgPart3.toPrettyString(),
                    (fusion.fpgPart5.frame + fusion.fpgPart3.frame) % 3,
                    p,
                    (expr5 != null && expr3 != null) ? (expr5 / expr3 - 1.0) : "NaN",
                    domainSpecificFeatures5.domainsRetained.collect { (domainAnnotation[it] ?: it).trim() }.join(";"),
                    domainSpecificFeatures3.domainsRetained.collect { (domainAnnotation[it] ?: it).trim() }.join(";"),
                    domainSpecificFeatures5.domainsBroken.collect { (domainAnnotation[it] ?: it).trim() }.join(";"),
                    domainSpecificFeatures3.domainsBroken.collect { (domainAnnotation[it] ?: it).trim() }.join(";"),
                    domainSpecificFeatures5.piisRetained.unique().join(";"),
                    domainSpecificFeatures3.piisRetained.unique().join(";"),
                    (0..(nThemes - 1)).collect {
                        Math.log10(domainSpecificFeatures5.domainProfileRetained[it] +
                                domainSpecificFeatures3.domainProfileRetained[it] + 1) / ffasRanges[it]
                    }
        ].flatten().join("\t"))
    }
}