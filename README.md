# aws-glacier-multipart-upload

### What's this?

A command line tool to upload an archive (file) into amazon glacier.

### Requirements

A vault must already exist. I created one through AWS management console.

Tested with Java 9 and Apache Maven 3.5.2.

### Usage

````bash
mvn package
java -jar target/glacier-upload.jar --help
````
