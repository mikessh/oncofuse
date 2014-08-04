
Legacy Oncofuse script, please see this [page](http://www.unav.es/genetica/oncofuse.html) for details on running the pipeline.

To obtain and compile the latest version of legacy Oncofuse package execute:

```
git clone https://github.com/mikessh/oncofuse --branch legacy
cd oncofuse
mvm clean install
```

Then run as

```
cd target/
java -jar oncofuse-v1.0.X.jar [args]
```

If you need to copy oncofuse to other folder make sure **libs** and **common** folders are placed in the same directory, or just use symlink

![alt tag](http://www.unav.es/genetica/logo.png)