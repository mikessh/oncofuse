===================
= ONCOFUSE V1.0.9 =
===================


Oncofuse is a framework designed to estimate the oncogenic potential of de-novo discovered gene fusions. It uses several hallmark features and employs a bayesian classifier to provide the probability of a given gene fusion being a driver mutation.

Oncofuse is described in the following paper:
Mikhail Shugay, Inigo Ortiz de Mendibil, Jose L. Vizmanos and Francisco J. Novo. Oncofuse: a computational framework for the prediction of the oncogenic potential of gene fusions. Bioinformatics. 16 Aug 2013. doi:10.1093/bioinformatics/btt445.

See http://www.unav.es/genetica/oncofuse.html for further details.



>WHAT'S NEW

- Version 1.0.9 (Nov-2014) minor improvements, support for hg18, hg19 and hg38 genome assemblies. ALWAYS check the coordinate system

- Version 1.0.8 (Oct-2014) input format for FusionCatcher changed to support v0.99.3b, support for spanning/encompassing read filtering.

- Version 1.0.7 (Jun-Aug-2014) several minor improvements, e.g. reporting number of spanning/encompassing reads in output.

- Version 1.0.6 (24-09-2013) fixes some bugs. We strongly recommend updating to this version.

- As from version 1.0.5 (released on 16-09-2013) the classifier also takes into account broken domains in fusion proteins. This affords increased precision and recall rates than originally published.

- Version 1.0.4 has extended the output format and supports tophat-fusion-post and RNASTAR input.

- As from version 1.0.3, installation of Groovy is not necessary. Input file types and the content of output file have also been improved. 



>OPTIONS

-p option specifies the number of threads Oncofuse will use

-a option specifies genome assembly version. Allowed values: hg18, hg19 and hg38. Default value: hg19



>INPUT

This tool is designed to predict the oncogenic potential of fusion genes found by Next-Generation Sequencing in cancer cells. It also provides information on hallmarks of driver gene fusions, such as expression gain of resulting fusion gene, retained protein interaction interfaces and resulting protein domain functional profile.

Pre-requisites: Java(TM) SE Runtime Environment (build 1.7.0 and higher)

Running: $java -Xmx1G -jar Oncofuse.jar input_file input_type tissue_type output_file

Supported tissue types (tissue of origin for gene fusion): EPI (epithelial), HEM (hematopoietic), MES (mesenchymal), AVG (averaged, when tissue of origin is unknown)

Supported input types:

input_type = "coord"
Default format accepted by Oncofuse 
Tab-delimited file with lines containing 5' and 3' breakpoint positions (first nucleotide lost upon fusion) and tissue of origin: 
5' chrom \t 5' coord \t 3' chrom \t 3' coord \t tissue_type
For this format tissue of origin is set individually for each entry in input file and tissue_type argument should be set as "-"

input_type = "tophat"

Default output file of Tophat-fusion and Tophat2 (usually fusions.out file in results folder). Data is pre-filtered based on number of spanning N>=1 and total number of supporting reads M>=2 reads. These parameters could be changed with extended input type argument "tophat-N-M". Tissue type has to be set using tissue_type argument. Tophat-fusion-post is also supported with extended input type argument "tophat-post".

input_type = "fcatcher"

Default output file of FusionCatcher software. Tissue type has to be set using tissue_type argument.

input_type = "rnastar"

Default output file of RNASTAR software. Data is pre-filtered based on number of spanning N>=1 and total number of supporting reads M>=2 reads. These parameters could be changed with extended input type argument "rnastar-N-M". Tissue type has to be set using tissue_type argument.



>OUTPUT

A tab-delimited table with the following columns

SAMPLE_ID	The ID of sample for tophat-post, input file name otherwise
FUSION_ID	The original line number in input file
TISSUE	As specified by library argument or in 'coord' input file
GENOMIC	Chromosomal coordinates for both breakpoints (as in input file)

SPANNING_READS	Number of reads that cover fusion junction
ENCOMPASSING_READS	Number of reads that map discordantly with one mate mapping to 5'FPG (fusion partner gene) and other mapping to 3'FPG

5_FPG_GENE_NAME	The HGNC symbol of 5' fusion partner gene
5_IN_CDS?	Indicates whether breakpoint is within the CDS of this gene
5_SEGMENT_TYPE	Indicates whether breakpoint is located within either exon or intron
5_SEGMENT_ID	Indicates number of exon or intron where breakpoint is located
5_COORD_IN_SEGMENT	Indicates coordinates for breakpoint within that exon/intron
5_FULL_AA	Length of translated 5' FPG in full amino acids
5_FRAME	Frame of translated 5' FPG

(Same as 7 lines above for the 3' fusion partner gene)

FPG_FRAME_DIFFERENCE	The resulting frame of fusion gene, if equals to 0 then the fusion is in-frame

P_VAL_CORR	The Bayesian probability of fusion being a passenger (class 0), given as Bonferroni-corrected P-value
DRIVER_PROB	The Bayesian probability of fusion being a driver (class 1)
EXPRESSION_GAIN	Expression gain of fusion calculated as [(expression of 5' gene)/(expression of 3' gene)]-1

5_DOMAINS_RETAINED	List of protein domains retained in 5' fusion partner gene
3_DOMAINS_RETAINED	List of protein domains retained in 3' fusion partner gene
5_DOMAINS_BROKEN	List of protein domains that overlap breakpoint in 5' fusion partner gene
3_DOMAINS_BROKEN	List of protein domains that overlap breakpoint in 3' fusion partner gene
5_PII_RETAINED	List of protein interaction interfaces retained in 5' fusion partner gene
3_PII_RETAINED	List of protein interaction interfaces retained in 3' fusion partner gene
CTF, G, H, K, P and TF	Corresponding functional family association scores (log-transformed, scaled to the largest score obtained from classifier training set). 
 