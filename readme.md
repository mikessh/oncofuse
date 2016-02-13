[![Build Status](https://travis-ci.org/mikessh/oncofuse.svg?branch=legacy)](https://travis-ci.org/mikessh/oncofuse)

---

![alt tag](http://www.unav.es/genetica/logo.png)

Oncofuse is a framework designed to estimate the oncogenic potential of de-novo discovered gene fusions. It uses several hallmark features and employs a bayesian classifier to provide the probability of a given gene fusion being a driver mutation.

Please cite the following [paper](http://www.ncbi.nlm.nih.gov/pubmed/23956304) if you are using Oncofuse:

Mikhail Shugay, Inigo Ortiz de Mendibil, Jose L. Vizmanos and Francisco J. Novo. *Oncofuse: a computational framework for the prediction of the oncogenic potential of gene fusions.* **Bioinformatics.** 16 Aug 2013. doi:10.1093/bioinformatics/btt445.

This repository contains the Oncofuse source code and the latest binary version of the framework in [releases](https://github.com/mikessh/oncofuse/releases/latest) section. Please see this [page](http://www.unav.es/genetica/oncofuse.html) for details on running the pipeline. You may be also interested in checking README.txt, examples folder and running `java -jar Oncofuse.jar -h`.

---
## Notes

* **Disclaimer**: Oncofuse solely performs fusions annotation and oncogenic potential prediction under the assumption that a given fusion exists (i.e. is verifiable by PCR). It is the goal of experimental setup and fusion detection software to filter out those fusions that do not physically exist.

  * Oncofuse reports, but does not account for fusion frame when calculating P-values. Is is done intentionally, as the fusion information often could be incomplete, e.g. there are cases when random nucleotides are added in fusion junction restoring the frame. So it is left up to the user to decide should he ignore out-of-frame fusions or not.

* Note that since v1.0.9 there is an `-a hgXX` option, specifying genome assembly. It defaults to `hg19`, yet certain tools, for example FusionCatcher v0.99.3e provide output in `hg38` coordinates.

* Also note that support for FusionCatcher versions earlier than 0.99.3 was deprecated

* Please use the [issue tracker](https://github.com/mikessh/oncofuse/issues) to report bugs and suggest new features.

---
## Compiling Oncofuse

* To obtain and compile the latest version of legacy Oncofuse package execute:

```bash
git clone https://github.com/mikessh/oncofuse --branch legacy
cd oncofuse
mvm clean install
```

* Then run as

```bash
cd target/
java -jar oncofuse-v1.X.X.jar [args]
```

If you need to copy oncofuse to other folder make sure **libs** and **common** folders are placed in the same directory, or just use symlink

## Documentation

Oncofuse is a framework designed to estimate the oncogenic potential of de-novo discovered gene fusions. It uses several hallmark features and employs a bayesian classifier to provide the probability of a given gene fusion being a driver mutation.

Oncofuse is described in the following paper:
Mikhail Shugay, Inigo Ortiz de Mendibil, Jose L. Vizmanos and Francisco J. Novo. Oncofuse: a computational framework for the prediction of the oncogenic potential of gene fusions. Bioinformatics. 16 Aug 2013. doi:10.1093/bioinformatics/btt445.

See http://www.unav.es/genetica/oncofuse.html for additional details.

### Options

``-p`` option specifies the number of threads Oncofuse will use

``-a`` option specifies genome assembly version. Allowed values: hg18, hg19 and hg38. Default value: hg19

### Input

This tool is designed to predict the oncogenic potential of fusion genes found by Next-Generation Sequencing in cancer cells. It also provides information on hallmarks of driver gene fusions, such as expression gain of resulting fusion gene, retained protein interaction interfaces and resulting protein domain functional profile.

Pre-requisites: Java(TM) SE Runtime Environment (build 1.7.0 and higher)

Running: 

```
$ java -Xmx1G -jar Oncofuse.jar input_file input_type tissue_type output_file
```

Supported tissue types (tissue of origin for gene fusion): EPI (epithelial), HEM (hematopoietic), MES (mesenchymal), AVG (averaged, when tissue of origin is unknown)

Supported input types:

**input_type = "coord"**
Default format accepted by Oncofuse 
Tab-delimited file with lines containing 5' and 3' breakpoint positions (first nucleotide lost upon fusion) and tissue of origin: 

5' chrom | 5' coord | 3' chrom | 3' coord | tissue_type
---------|----------|----------|----------|------------
         |          |          |          |            

For this format tissue of origin is set individually for each entry in input file and tissue_type argument should be set as "-"
Note that there are optional additional columns:

* 5' fusion partner gene (FPG) strand
* 3' FPG strand
* Number of spanning reads (reads that include junction bases)
* Number of encompassing reads (reads that encompass junction, but the junction itself is in the insert region)

**input_type = "tophat"**
Default output file of Tophat-fusion and Tophat2 (usually fusions.out file in results folder). Data is pre-filtered based on number of spanning N>=1 and total number of supporting reads M>=2 reads. These parameters could be changed with extended input type argument "tophat-N-M". Tissue type has to be set using tissue_type argument. Tophat-fusion-post is also supported with extended input type argument "tophat-post".

**input_type = "fcatcher"**
Default output file of FusionCatcher software. Tissue type has to be set using tissue_type argument.

**input_type = "rnastar"**
Default output file of RNASTAR software. Data is pre-filtered based on number of spanning N>=1 and total number of supporting reads M>=2 reads. These parameters could be changed with extended input type argument "rnastar-N-M". Tissue type has to be set using tissue_type argument.

### Output

A tab-delimited table with the following columns

column name | description
------------|-------------
SAMPLE_ID	|	The ID of sample for tophat-post, input file name otherwise
FUSION_ID	|	The original line number in input file
TISSUE	|	As specified by library argument or in 'coord' input file
GENOMIC	|	Chromosomal coordinates for both breakpoints (as in input file)
SPANNING_READS	|	Number of reads that cover fusion junction
ENCOMPASSING_READS	|	Number of reads that map discordantly with one mate mapping to 5'FPG (fusion partner gene) and other mapping to 3'FPG
5_FPG_GENE_NAME	|	The HGNC symbol of 5' fusion partner gene
5_IN_CDS?	|	Indicates whether breakpoint is within the CDS of this gene
5_SEGMENT_TYPE	|	Indicates whether breakpoint is located within either exon or intron
5_SEGMENT_ID	|	Indicates number of exon or intron where breakpoint is located
5_COORD_IN_SEGMENT	|	Indicates coordinates for breakpoint within that exon/intron
5_FULL_AA	|	Length of translated 5' fusion partner gene (FPG) in full amino acids
5_FRAME	|	Frame of translated 5' FPG
(Same as 7 lines above for the 3' fusion partner gene) 	|	...
FPG_FRAME_DIFFERENCE	|	The resulting frame of fusion gene, if equals to 0 then the fusion is in-frame
P_VAL_CORR	|	he Bayesian probability of fusion being a passenger (class 0), given as Bonferroni-corrected P-value
DRIVER_PROB	|	The Bayesian probability of fusion being a driver (class 1)
EXPRESSION_GAIN	|	Expression gain of fusion calculated as [(expression of 5' gene)/(expression of 3' gene)]-1
5_DOMAINS_RETAINED	|	List of protein domains retained in 5' fusion partner gene
3_DOMAINS_RETAINED	|	List of protein domains retained in 3' fusion partner gene
5_DOMAINS_BROKEN	|	List of protein domains that overlap breakpoint in 5' fusion partner gene
3_DOMAINS_BROKEN	|	List of protein domains that overlap breakpoint in 3' fusion partner gene
5_PII_RETAINED	|	List of protein interaction interfaces retained in 5' fusion partner gene
3_PII_RETAINED	|	List of protein interaction interfaces retained in 3' fusion partner gene
CTF, G, H, K, P and TF	|	Corresponding functional family association scores (FFAS, see paper for details). Values are log-transformed and scaled to the largest score obtained from classifier training set. 
