package ich.bins;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.UploadMultipartPartRequest;
import com.amazonaws.services.glacier.model.UploadMultipartPartResult;
import com.amazonaws.util.BinaryUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class ArchiveMPU {

  private static final String partSize = "1048576"; // 1 MB.
  private static final ProfileCredentialsProvider credentials = new ProfileCredentialsProvider();

  private final AmazonGlacier client;
  private final String fileToUpload;
  private final String description;
  private final String vaultName;

  private ArchiveMPU(AmazonGlacier client, String fileToUpload, String description, String vaultName) {
    this.client = client;
    this.fileToUpload = fileToUpload;
    this.description = description;
    this.vaultName = vaultName;
  }

  public static void main(String[] args) throws IOException {

    try {
      if (args.length != 5) {
        System.out.println("Args: fileToUpload description vaultName serviceEndpoint signingRegion");
        System.out.println("Example:\n\t" +
            "/home/ich/myarchive.tar.gpg\n\t" + //fileToUpload
            "myarchive.tar.gpg\n\t" + //description
            "myvault\n\t" + // vaultName
            "glacier.eu-central-1.amazonaws.com\n\t" + //serviceEndpoint
            "eu-central-1"); //signingRegion
        System.exit(1);
      }
      String fileToUpload = args[0];
      String description = args[1];
      String vaultName = args[2];
      String serviceEndpoint = args[3];
      String signingRegion = args[4];

      System.out.println("fileToUpload: " + fileToUpload);
      System.out.println("description: " + description);
      System.out.println("vaultName: " + vaultName);
      System.out.println("serviceEndpoint: " + serviceEndpoint);
      System.out.println("signingRegion: " + signingRegion);

      AmazonGlacier client =
          AmazonGlacierClientBuilder.standard()
              .withCredentials(credentials)
              .withEndpointConfiguration(
                  new AwsClientBuilder.EndpointConfiguration(
                      serviceEndpoint, signingRegion))
              .build();

      ArchiveMPU archiveMPU = new ArchiveMPU(client, fileToUpload, description, vaultName);

      InitiateMultipartUploadResult initiateUploadResult = archiveMPU.initiateMultipartUpload();
      System.out.println("initiateUploadResult:\n" + initiateUploadResult);
      String uploadId = initiateUploadResult.getUploadId();
      String checksum = archiveMPU.uploadParts(uploadId);
      CompleteMultipartUploadResult result = archiveMPU.completeMultiPartUpload(
          uploadId,
          checksum);
      System.out.println("Completed an archive\n:" + result);

    } catch (Exception e) {
      e.printStackTrace(System.out);
    }

  }

  private InitiateMultipartUploadResult initiateMultipartUpload() {
    // Initiate
    InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest()
        .withVaultName(vaultName)
        .withArchiveDescription(description)
        .withPartSize(partSize);

    return client.initiateMultipartUpload(request);
  }

  private String uploadParts(
      String uploadId) throws
      NoSuchAlgorithmException,
      AmazonClientException,
      IOException {
    int filePosition = 0;
    long currentPosition = 0;
    final List<byte[]> binaryChecksums = new LinkedList<>();

    final File file = new File(fileToUpload);
    try (final FileInputStream fileToUpload = new FileInputStream(file)) {
      while (currentPosition < file.length()) {
        int read = uploadPart(uploadId, vaultName, filePosition, currentPosition, binaryChecksums, fileToUpload);
        if (read < 0) {
          break;
        }
        currentPosition = currentPosition + read;
      }
    }
    return TreeHashGenerator.calculateTreeHash(binaryChecksums);
  }

  private int uploadPart(
      String uploadId,
      String vaultName,
      int filePosition,
      long currentPosition,
      List<byte[]> binaryChecksums,
      FileInputStream fileToUpload) {
    int maxAttempts = 10;
    for (int i = 0; i < maxAttempts; i++) {
      try {
        byte[] buffer = new byte[Integer.valueOf(partSize)];
        int read = fileToUpload.read(buffer, filePosition, buffer.length);
        if (read < 0) {
          return read;
        }
        byte[] bytesRead = Arrays.copyOf(buffer, read);

        String contentRange = String.format("bytes %s-%s/*", currentPosition, currentPosition + read - 1);
        String checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
        byte[] binaryChecksum = BinaryUtils.fromHex(checksum);
        binaryChecksums.add(binaryChecksum);
        System.out.println(contentRange);

        //Upload part.
        UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
            .withVaultName(vaultName)
            .withBody(new ByteArrayInputStream(bytesRead))
            .withChecksum(checksum)
            .withRange(contentRange)
            .withUploadId(uploadId);

        UploadMultipartPartResult partResult = client.uploadMultipartPart(partRequest);
        System.out.println("Part uploaded, checksum: " + partResult.getChecksum());
        return read;
      } catch (Exception e) {
        System.out.println("Attempt #" + i + " / " + maxAttempts + " has failed");
        e.printStackTrace(System.out);
      }
    }
    System.out.println("Giving up");
    return -1;
  }

  private CompleteMultipartUploadResult completeMultiPartUpload(
      String uploadId,
      String checksum) throws IOException {

    File file = new File(fileToUpload);

    CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest()
        .withVaultName(vaultName)
        .withUploadId(uploadId)
        .withChecksum(checksum)
        .withArchiveSize(String.valueOf(file.length()));

    return client.completeMultipartUpload(compRequest);
  }
}