# aws-glacier-multipart-upload

### What's this?

A command line tool to upload an archive (file) into amazon glacier.

### Requirements

A vault must already exist. I created one through AWS management console.

### Usage

````bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_KEY=...
./gradlew clean run --args='--help'
````

