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