/**
 Copyright 2013-14 Mikhail Shugay (mikhail.shugay@gmail.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package es.unav.oncofuse.legacy

@Grapes(
        @Grab(group = 'nz.ac.waikato.cms.weka', module = 'weka-stable', version = '3.6.6')
        @Grab(group = 'org.codehaus.gpars', module = 'gpars', version = '1.2.1')
)

import groovyx.gpars.GParsPool
import weka.classifiers.misc.SerializedClassifier
import weka.core.Instance
import weka.core.Instances
import weka.core.converters.ArffLoader

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

def homeDir = new File(getClass().protectionDomain.codeSource.location.path).parent.replaceAll("%20", " ")
if (args.length < 4) {
    println "Usage: java -jar Oncofuse.jar input_file input_type tissue_type output_file"
    println "Note: run Java with at least 1Gb of memory (set using -Xmx1G)"
    println "Supported input types: coord, fcatcher, tophat, tophat-N-M, tophat-post, rnastar, rnastar-N-M"
    println "Running with input type args: replace N by number of spanning reads and M by number of total supporting read pairs"
    println "Supported tissue types: EPI, HEM, MES, AVG or -"
    println "Version 1.0.7, 29Jun2014"
    System.exit(0)
}
int THREADS = Runtime.getRuntime().availableProcessors()
def inputFileName = args[0], inputType = args[1], tissueType = args[2], outputFileName = args[3]
if (!new File(inputFileName).exists()) {
    println "[ERROR] Input file not found"
    System.exit(1)
}
if (!['EPI', 'HEM', 'MES', 'AVG', '-'].any { tissueType == it }) {
    println "[ERROR] Unrecognized tissue type"
    System.exit(1)
}

/////////////////////////////////////////
// LOADING BKPT POSITIONS AND MAPPING //
///////////////////////////////////////
println "[${new Date()}] Mapping fusion breakpoints"

// Data for mapping
// Refseq canonical data schema
// 0        1       2       3       4       5           6       7           8           9
// #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonStarts	exonEnds	name2
def exonStartCoords = new HashMap<String, List<Integer>>(), exonEndCoords = new HashMap<String, List<Integer>>()
def strand = new HashMap<String, Boolean>()
def geneByChrom = new HashMap<String, List<String>>()
def transcriptStartCoords = new HashMap<String, Integer>(), transcriptEndCoords = new HashMap<String, Integer>()
def cdsStartCoords = new HashMap<String, Integer>(), cdsEndCoords = new HashMap<String, Integer>()

def canonicalTranscriptsFileName = "$homeDir/common/refseq_canonical.txt", refseqFileName = "$homeDir/common/refseq_full.txt"
def canonicalTranscripts = new HashSet<String>(new File(canonicalTranscriptsFileName).readLines())
def gene2chr = new HashMap<String, String>()

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
            strand.put(geneName, splitLine[2] == '+')
        }
    }
}

// Routines for mapping
class FpgPart {
    String chrom
    int chrCoord
    String geneName
    boolean fivePrimeFpg
    boolean exon
    int segmentId, coord, aaPos, frame, fullLen
    boolean cds
    String signature

    FpgPart(String chrom, int chrCoord, String geneName, boolean fivePrimeFpg, boolean exon, int segmentId, int coord, int aaPos, int frame, int fullLen) {
        this.chrom = chrom
        this.chrCoord = chrCoord
        this.geneName = geneName
        this.fivePrimeFpg = fivePrimeFpg
        this.cds = aaPos > 0 && aaPos < fullLen && coord >= 0
        this.exon = exon
        this.segmentId = segmentId
        this.coord = coord
        this.aaPos = aaPos
        this.frame = frame
        this.fullLen = fullLen
        this.signature = "$geneName\t$fivePrimeFpg\t$exon\t$segmentId\t$coord"
    }

    String toPrettyString() {
        return [geneName, cds ? "Yes" : "No", exon ? "Exon" : "Intron", segmentId, coord, fivePrimeFpg ? aaPos : fullLen - aaPos, frame].join("\t")
    }

    @Override
    String toString() {
        return signature
    }

    @Override
    int hashCode() {
        return this.signature.hashCode()
    }

    @Override
    boolean equals(Object obj) {
        return this.signature.equals((obj as FpgPart).signature)
    }
}

def fetchGene = { String chrom, int coord ->
    def geneList = geneByChrom[chrom]
    def gene = geneList.find {
        transcriptStartCoords.get(it) <= coord &&
                transcriptEndCoords.get(it) >= coord
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
    boolean s = strand[gene]
    def exonLen = { int exonId ->
        Math.max(0, Math.min(exonsEnds[exonId], cdsEnd) - Math.max(exonsStarts[exonId], cdsStart))
    }
    def truncExonLen = { int exonId ->
        s ? Math.max(0, Math.min(Math.min(cdsEnd, exonsEnds[exonId]), coord - 1) - Math.max(exonsStarts[exonId], cdsStart)) : // bkpt marks first nt deleted, so -1 for 5' and +1 for 3'
                Math.max(0, Math.min(exonsEnds[exonId], cdsEnd) - Math.max(Math.max(exonsStarts[exonId], cdsStart), coord + 1))
    }
    int fullLen = (int) (0..(n - 1)).collect { exonLen(it) }.sum()

    for (int i = 0; i < n; i++) { // From head to tail
        if (coord <= exonsStarts[i]) // intron
        {
            int cc = s ? (int) (0..(i - 1)).collect { exonLen(it) }.sum() : (int) (i..(n - 1)).collect {
                exonLen(it)
            }.sum()
            return new FpgPart(gene2chr[gene], coord, gene, fivePrime, false,
                    s ? i : n - i,
                    s ? coord - exonsEnds[i - 1] : exonsStarts[i] - coord,
                    (int) (cc / 3), fivePrime ? cc % 3 : (fullLen - cc) % 3, (int) (fullLen / 3))
        } else if (coord >= exonsStarts[i] && coord <= exonsEnds[i]) // exon
        {
            int cc = (s ?
                    (int) (i > 0 ? (0..(i - 1)).collect { exonLen(it) }.sum() : 0) :
                    (int) (i < n - 1 ? ((i + 1)..(n - 1)).collect { exonLen(it) }.sum() : 0)) +
                    truncExonLen(i)
            return new FpgPart(gene2chr[gene], coord, gene, fivePrime, true,
                    s ? i + 1 : n - i,
                    s ? coord - exonsStarts[i] : exonsEnds[i] - coord,
                    (int) (cc / 3), fivePrime ? cc % 3 : (fullLen - cc) % 3, (int) (fullLen / 3))
        }
    }
}

// Read and convert to common format
def inputData = new ArrayList<String>()

println "[${new Date()}] Reading input file"

def inputTypeArgs = inputType.split("-")
inputType = inputTypeArgs[0]

switch (inputType) {
    case 'fcatcher':
        def inputFile = new File(inputFileName)
        inputFile.splitEachLine("\t") { line ->
            if (!line[0].startsWith("#"))
                inputData.add("chr" + line[6].split(":")[0..1].collect { it.trim() }.join("\t") + "\tchr"
                        + line[7].split(":")[0..1].collect { it.trim() }.join("\t")
                        + "\t" + tissueType + "\t$inputFileName\t-1\t-1")
        }
        break

    case 'tophat':
        def inputFile = new File(inputFileName)
        int minSpan = 1, minSum = 2
        if (inputTypeArgs.length == 3) {
            minSpan = Math.max(1, Integer.parseInt(inputTypeArgs[1]))
            minSum = Integer.parseInt(inputTypeArgs[2])
        }

        if (inputTypeArgs.length == 2 && inputTypeArgs[1] == 'post')
            inputFile.splitEachLine("[ \t]") { line ->
                println line
                println line.size()
                if (!line[0].startsWith("#") && line.size() > 3 && line[1].startsWith("chr")) {
                    def chrs = line[1].split("-")
                    inputData.add([chrs[0], line[2], chrs[1], line[3], tissueType, line[0], -1, -1].join("\t"))
                }
            }
        else
            inputFile.splitEachLine("\t") { line ->
                if (!line[0].startsWith("#")) {
                    def chrs = line[0].split("-")
                    int nSpan = Integer.parseInt(line[4]), nSupport = Integer.parseInt(line[5])
                    if (nSpan >= minSpan && (nSpan + nSupport) >= minSum)
                        inputData.add([chrs[0], line[1], chrs[1], line[2], tissueType, inputFileName, nSpan, nSupport].join("\t"))
                }
            }
        break

    case 'rnastar':
        def inputFile = new File(inputFileName)
        int minSpan = 1, minSum = 2
        if (inputTypeArgs.length == 3) {
            minSpan = Integer.parseInt(inputTypeArgs[1])
            minSum = Integer.parseInt(inputTypeArgs[2])
        }

        def sign2counter = new ConcurrentHashMap<String, AtomicInteger[]>()
        def sign2coord = new ConcurrentHashMap<String, String>()
        inputData = inputFile.readLines().findAll { !it.startsWith("#") }

        def n = new AtomicInteger(0), k = new AtomicInteger(0), m = new AtomicInteger(0)
        GParsPool.withPool THREADS, {
            inputData.eachParallel { line ->
                line = line.split("\t")
                // Sort reads
                int type = Integer.parseInt(line[6])
                String chrom1 = line[0], chrom2 = line[3]
                int coord1 = Integer.parseInt(line[1]), coord2 = Integer.parseInt(line[4])
                boolean strand1 = line[2] == "+", strand2 = line[5] == "+"
                String gene1 = fetchGene(chrom1, coord1)
                String gene2 = fetchGene(chrom2, coord2)

                if (gene1 != null && gene2 != null) {
                    def map1 = map(false, gene1, coord1 + (strand1 ? -1 : 1)),
                        map2 = map(false, gene2, coord2 + (strand1 ? 1 : -1))
                    if (map1 != null && map1.exon && map2 != null && map2.exon) {
                        exon1 = map1.segmentId
                        exon2 = map2.segmentId
                        String signature = [gene1, exon1, gene2, exon2].join("\t")

                        def counter = new AtomicInteger[2]
                        counter[0] = new AtomicInteger(0)
                        counter[1] = new AtomicInteger(0)
                        counter = sign2counter.putIfAbsent(signature, counter) ?: counter

                        if (type < 0) {
                            counter[1].incrementAndGet()
                            k.incrementAndGet()
                        } else {
                            sign2coord.putIfAbsent(signature, [chrom1, coord1, chrom2, coord2, tissueType].join("\t"))
                            counter[0].incrementAndGet()
                            m.incrementAndGet()
                        }
                    }
                }
                def nn = n.incrementAndGet()
                if (nn % 100000 == 0)
                    println "[${new Date()}] [Post-processing RNASTAR input] $nn reads analyzed"
            }
        }

        inputData.clear()

        sign2counter.each {
            int nSpan = it.value[0].intValue(), nSupport = it.value[1].intValue()
            if (nSpan >= minSpan && (nSpan + nSupport) >= minSum && sign2coord[it.key] != null)
                inputData.add(sign2coord[it.key] + "\t" + inputFileName + "\t" + nSpan + "\t" + nSupport)
        }
        println "[${new Date()}] [Post-processing RNASTAR input] Taken $n reads, of them mapped to canonical RefSeq: $k as encompassing, $m as spanning. Total ${sign2coord.size()} exon pairs, of them ${inputData.size()} passed junction coverage filter."
        break

    case 'coord':
        def inputFile = new File(inputFileName)
        inputFile.eachLine { line ->
            if (!line.startsWith("#"))
                inputData.add([line.split("\t").collect { it.trim() }, inputFileName, -1, -1].flatten().join("\t"))
        }
        break

    default:
        println "Unknown input format"
        System.exit(-1)
}
if (inputData.size() == 0) {
    println "No fusions loaded"
    System.exit(-1)
}

// Make a list of fusions
println "[${new Date()}] Mapping breakpoints to known genes (this is going to filter a lot)"
def i = 0, j = 0

class Fusion {
    final int nSpan, nSupport
    final FpgPart fpgPart5, fpgPart3
    final String tissue, sample
    final int id

    Fusion(int id, int nSpan, int nSupport,
           FpgPart fpgPart5, FpgPart fpgPart3,
           String tissue, String sample) {
        this.id = id
        this.nSpan = nSpan
        this.nSupport = nSupport
        this.fpgPart5 = fpgPart5
        this.fpgPart3 = fpgPart3
        this.tissue = tissue
        this.sample = sample
    }
}

def fusionList = new ArrayList<Fusion>()
def tissue2FpgMap = new HashMap<String, Set<FpgPart>>()
def fpgSet = new HashSet<FpgPart>()
int ff = 0
int m1 = 0, m2 = 0, m3 = 0
inputData.each { line ->
    j++
    def splitLine = line.split("\t")
    def coord5 = Integer.parseInt(splitLine[1]), coord3 = Integer.parseInt(splitLine[3])
    def tissue = splitLine[4].toUpperCase(), sample = splitLine[5]
    def nSpan = splitLine[6].toInteger(), nSupport = splitLine[7].toInteger()
    def gene5, gene3
    def fpgPart5, fpgPart3
    if (!(gene5 = fetchGene(splitLine[0], coord5)) || !(fpgPart5 = map(true, gene5, coord5))) {
        if (ff++ < 20)
            println "[${new Date()}] FILTER (reporting first 20 only): Not mapped to any acceptable transcript: 5' \"${splitLine[0]}:${coord5}\" at fusion #${j} in input"
        m1++
    }
    if (!(gene3 = fetchGene(splitLine[2], coord3)) || !(fpgPart3 = map(false, gene3, coord3))) {
        if (ff++ < 20)
            println "[${new Date()}] FILTER (reporting first 20 only): Not mapped to any acceptable transcript: 3' \"${splitLine[2]}:${coord3}\" at fusion #${j} in input"
        m2++
    }
    if (gene5 == gene3) {
        if (ff++ < 20)
            println "[${new Date()}] FILTER (reporting first 20 only): Same gene for 5' and 3' FPG at fusion #${j} in input"
        fpgPart5 = null
        m3++
    }

    if (fpgPart5 && fpgPart3) {
        fusionList.add(new Fusion(j, nSpan, nSupport, fpgPart5, fpgPart3, tissue, sample))
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
println "[${new Date()}] $i fusions mapped out of $j. Dropped fusion candidates: $m1 - 5'FPG not mapped, $m2 - 3'FPG not mapped, $m3 - mapped to same gene"

////////////////////////
// FEATURES FOR FPGS //
//////////////////////
println "[${new Date()}] Getting features for FPG parts"

// Expression data
println "[${new Date()}] Loading expression-related data"
def libs = new File("$homeDir/libs").listFiles().findAll { it.isDirectory() && !it.isHidden() }.collect { it.name }
def promData = new HashMap<String, Map<String, List<Double>>>(),
    exprData = new HashMap<String, Map<String, Double>>(),
    utrData = new HashMap<String, Map<String, List<Double>>>()
libs.each { lib ->
    lib = lib.toUpperCase()
    promData.put(lib, new HashMap<String, List<Double>>())
    new File("$homeDir/libs/$lib/prom.txt").splitEachLine("\t") { line ->
        promData[lib].put(line[0], line[1..(line.size() - 1)].collect { Double.parseDouble(it) })
    }
    exprData.put(lib, new HashMap<String, Double>())
    new File("$homeDir/libs/$lib/expr.txt").splitEachLine("\t") { line ->
        exprData[lib].put(line[0], Double.parseDouble(line[1]))
    }
    utrData.put(lib, new HashMap<String, List<Double>>())
    new File("$homeDir/libs/$lib/utr.txt").splitEachLine("\t") { line ->
        utrData[lib].put(line[0], line[1..(line.size() - 1)].collect { Double.parseDouble(it) })
    }
}
int nPromFeatures = promData.iterator().next().value.iterator().next().value.size(),
    nUTRFeatures = utrData.iterator().next().value.iterator().next().value.size()

// Domain and PII data
def domainsFileName = "$homeDir/common/domains.txt", domainAnnotFileName = "$homeDir/common/interpro_annot.txt",
    piisFileName = "$homeDir/common/piis.txt"
println "[${new Date()}] Loading domain and protein interaction interface-related data"

class Feature {
    String parentGene
    int aaFrom, aaTo
    String featureId

    Feature(String parentGene, int aaFrom, int aaTo, String featureId) {
        this.parentGene = parentGene
        this.aaFrom = aaFrom
        this.aaTo = aaTo
        this.featureId = featureId
    }
}

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
def gene2DavidIdFileName = "$homeDir/common/gene_symbol2id.txt", davidIdExpandFileName = "$homeDir/common/id_expand.txt",
    davidId2GOIdFileName = "$homeDir/common/id2go.txt", ffasRangesFileName = "$homeDir/common/ffas_range.txt",
    go2themeFileName = "$homeDir/common/go2theme.txt"
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

class TissueSpecificFeatures {
    // Stage 1:
    List<Double> promFeatures
    List<Double> utrFeatures

    // Stage 2:
    double piiExprRetained, piiExprLost
}

class DomainFeatures {
    // Stage 1:
    def domainsRetained = new HashSet<String>(), domainsLost = new HashSet<String>(), domainsBroken = new HashSet<String>()
    def piisRetained = new ArrayList<String>(), piisLost = new ArrayList<String>()

    // Stage 2:
    int nSelfPIIsRetained = 0, nSelfPIIsLost = 0
    int nPIIsRetained, nPIIsLost
    double[] domainProfileRetained, domainProfileLost, domainProfileBroken
}

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
def schemaFileName = "$homeDir/nbn_schema", classifierFileName = "$homeDir/common/class"

// Wrapper
class ClassifierWrapper {
    Instances trainingData
    SerializedClassifier sc
    String schema = "@RELATION ONCOFUSE\n" +
            "@ATTRIBUTE\t5_r_prom1\tNUMERIC\n" +
            "@ATTRIBUTE\t5_r_prom2\tNUMERIC\n" +
            "@ATTRIBUTE\tr_self_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tr_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tr_pii_expr\tNUMERIC\n" +
            "@ATTRIBUTE\tr_CTF\tNUMERIC\n" +
            "@ATTRIBUTE\tr_G\tNUMERIC\n" +
            "@ATTRIBUTE\tr_H\tNUMERIC\n" +
            "@ATTRIBUTE\tr_K\tNUMERIC\n" +
            "@ATTRIBUTE\tr_P\tNUMERIC\n" +
            "@ATTRIBUTE\tr_TF\tNUMERIC\n" +
            "@ATTRIBUTE\t3_r_utr1\tNUMERIC\t\n" +
            "@ATTRIBUTE\tb_CTF\tNUMERIC\n" +
            "@ATTRIBUTE\tb_G\tNUMERIC\n" +
            "@ATTRIBUTE\tb_H\tNUMERIC\n" +
            "@ATTRIBUTE\tb_K\tNUMERIC\n" +
            "@ATTRIBUTE\tb_P\tNUMERIC\n" +
            "@ATTRIBUTE\tb_TF\tNUMERIC\t\n" +
            "@ATTRIBUTE\t3_l_prom1\tNUMERIC\n" +
            "@ATTRIBUTE\t3_l_prom2\tNUMERIC\n" +
            "@ATTRIBUTE\tl_self_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tl_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tl_pii_expr\tNUMERIC\n" +
            "@ATTRIBUTE\tl_CTF\tNUMERIC\n" +
            "@ATTRIBUTE\tl_G\tNUMERIC\n" +
            "@ATTRIBUTE\tl_H\tNUMERIC\n" +
            "@ATTRIBUTE\tl_K\tNUMERIC\n" +
            "@ATTRIBUTE\tl_P\tNUMERIC\n" +
            "@ATTRIBUTE\tl_TF\tNUMERIC\n" +
            "@ATTRIBUTE\t5_l_utr1\tNUMERIC\n" +
            "@ATTRIBUTE\tclass\t{0,1}\n" +
            "@DATA"

    ClassifierWrapper(String trainingSetFname, String model) {
        sc = new SerializedClassifier()
        sc.setModelFile(new File(model))

        ArffLoader loader = new ArffLoader()
        //loader.setFile(new File(trainingSetFname))
        loader.setSource(new ByteArrayInputStream(schema.getBytes()))
        trainingData = loader.getDataSet()
        trainingData.setClassIndex(trainingData.numAttributes() - 1)
    }

    private Instance fusion2Instance(List<Double> featureVector) {
        double[] arr = new double[featureVector.size()]
        featureVector.eachWithIndex { it, ind -> arr[ind] = it }
        Instance inst = new Instance(1.0, arr)
        featureVector.eachWithIndex { it, ind -> if (Double.isNaN(it)) inst.setMissing(ind) }
        inst.setDataset(trainingData)
        return inst
    }

    double[] getPValue(List<Double> featureVector) {
        sc.distributionForInstance(fusion2Instance(featureVector))
    }
}

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
    pw.println(["SAMPLE_ID", "FUSION_ID", "TISSUE", "SPANNING_READS", "SUPPORTING_READS", "GENOMIC",
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
        pw.println([fusion.sample, fusion.id, fusion.tissue, fusion.nSpan, fusion.nSupport,
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