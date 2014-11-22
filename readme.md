
Legacy Oncofuse script, please see this [page](http://www.unav.es/genetica/oncofuse.html) for details on running the pipeline.

To obtain and compile the latest version of legacy Oncofuse package execute:

```bash
git clone https://github.com/mikessh/oncofuse --branch legacy
cd oncofuse
mvm clean install
```

Then run as

```bash
cd target/
java -jar oncofuse-v1.X.X.jar [args]
```

If you need to copy oncofuse to other folder make sure **libs** and **common** folders are placed in the same directory, or just use symlink

Note that since v1.0.9 there is an `-a hgXX` option, specifying genome assembly. It defaults to `hg19`, yet certain tools, for example FusionCatcher v0.99.3e provide output in `hg38` coordinates.

Also note that support for FusionCatcher versions earlier than 0.99.3e was deprecated

![alt tag](http://www.unav.es/genetica/logo.png)