# aws-glacier-multipart-upload

### What's this?

A command line tool to upload an archive (file) into amazon glacier.

### Requirements

A vault must already exist. I created one through AWS management console.

### Upload

````bash
mvn package
java -jar target/glacier-upload.jar \
        --file /home/ich/myarchive.tar.gpg \
        --description myarchive.tar.gpg \
        --vault-name myvault \
        --service-endpoint glacier.eu-central-1.amazonaws.com \
        --signing-region eu-central-1
````
