# aws-glacier-multipart-upload

### What's this?

A command line tool to upload an archive (file) into amazon glacier.

### Requirements

A vault must already exist. I created one through AWS management console.

### Usage

````bash
gradle clean run --args='--help'
````

Or [compile to native](https://github.com/oracle/graal/releases) first:


````bash
gradle shadowJar
native-image --no-fallback -jar build/libs/aws-glacier-multipart-upload-all.jar upload
./upload --help
````

