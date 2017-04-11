# aws-glacier-multipart-upload

### What is it

I couldn't get this bash script to work:

https://github.com/benporter/aws-glacier-multipart-upload

So I used java instead, using this reference:

https://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-an-archive-mpu-using-java.html

### Requirements

A vault must already exist. I created it through AWS management console.

### Upload

````bash
mvn clean package
java -jar target/bins-1.0-SNAPSHOT-jar-with-dependencies.jar \
	/home/ich/myarchive.tar.gpg \
	myarchive.tar.gpg \
	myvault \
	glacier.eu-central-1.amazonaws.com \
	eu-central-1
````
