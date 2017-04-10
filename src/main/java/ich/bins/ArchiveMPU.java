package ich.bins;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class ArchiveMPU {

  private static final int partSize = 1048576; // 1 MB.
  private static final int MAX_ATTEMPTS = 50;

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

      System.out.println("------------");
      System.out.println("fileToUpload: " + fileToUpload);
      System.out.println("description: " + description);
      System.out.println("vaultName: " + vaultName);
      System.out.println("serviceEndpoint: " + serviceEndpoint);
      System.out.println("signingRegion: " + signingRegion);
      System.out.println("------------");

      System.out.println("File size: " + new File(fileToUpload).length());

      AmazonGlacier client =
          AmazonGlacierClientBuilder.standard()
              .withCredentials(credentials)
              .withEndpointConfiguration(
                  new AwsClientBuilder.EndpointConfiguration(
                      serviceEndpoint, signingRegion))
              .withClientConfiguration(new ClientConfiguration())
              .build();

      ArchiveMPU archiveMPU = new ArchiveMPU(client, fileToUpload, description, vaultName);

      InitiateMultipartUploadResult initiateUploadResult = archiveMPU.initiateMultipartUpload();
      System.out.println(initiateUploadResult);
      String uploadId = initiateUploadResult.getUploadId();
      String checksum = archiveMPU.uploadParts(uploadId);
      CompleteMultipartUploadResult result = archiveMPU.completeMultiPartUpload(
          uploadId,
          checksum);
      System.out.println("Upload finished\n:" + result);
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }

  private InitiateMultipartUploadResult initiateMultipartUpload() {
    // Initiate
    InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest()
        .withVaultName(vaultName)
        .withArchiveDescription(description)
        .withPartSize(Integer.toString(partSize));

    return client.initiateMultipartUpload(request);
  }

  private String uploadParts(
      String uploadId) throws
      NoSuchAlgorithmException,
      AmazonClientException,
      IOException, InterruptedException {
    long currentPosition = 0;
    List<UploadResult> commands = new LinkedList<>();

    File file = new File(fileToUpload);

    try (final FileInputStream fileToUpload = new FileInputStream(file)) {
      while (currentPosition < file.length()) {
        UploadResult command = uploadPart(uploadId, currentPosition, fileToUpload);
        if (command.bytesRead == null) {
          break;
        }
        commands.add(command);
        currentPosition += command.bytesRead.length;
      }
    }
    List<byte[]> binaryChecksums = commands.stream()
        .map(command -> BinaryUtils.fromHex(command.checksum))
        .collect(Collectors.toList());
    ExecutorService pool = Executors.newFixedThreadPool(4);
    List<Future<UploadMultipartPartResult>> futures = pool.invokeAll(commands);
    boolean success = futures.stream().allMatch(f -> {
      try {
        f.get();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    if (success) {
      return TreeHashGenerator.calculateTreeHash(binaryChecksums);
    } else {
      throw new IllegalStateException("Some uploads have failed");
    }
  }

  private static final class UploadResult implements Callable<UploadMultipartPartResult> {
    final long offset;
    final byte[] bytesRead;
    final String vaultName;
    final String uploadId;
    private final AmazonGlacier client;
    final String checksum;

    private UploadResult(long offset,
                         byte[] bytesRead,
                         String vaultName,
                         String uploadId,
                         AmazonGlacier client) {
      this.bytesRead = bytesRead;
      this.offset = offset;
      this.vaultName = vaultName;
      this.uploadId = uploadId;
      this.client = client;
      this.checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
    }

    @Override
    public UploadMultipartPartResult call() throws Exception {
      String contentRange = String.format("bytes %d-%d/*",
          offset,
          offset + bytesRead.length - 1);
      for (int i = 0; i < MAX_ATTEMPTS; i++) {
        try {
          UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
              .withVaultName(vaultName)
              .withBody(new ByteArrayInputStream(bytesRead))
              .withChecksum(checksum)
              .withRange(contentRange)
              .withUploadId(uploadId);
          UploadMultipartPartResult partResult = client.uploadMultipartPart(partRequest);
          System.out.println(contentRange + " uploaded, checksum: " + partResult.getChecksum());
          return partResult;
        } catch (Exception e) {
          System.out.println(contentRange + " (attempt " + i + " / " + MAX_ATTEMPTS + ") failed: " + e.getMessage());
        }
      }
      throw new IllegalStateException(contentRange + ": Giving up after " + MAX_ATTEMPTS + " attempts");
    }
  }

  private UploadResult uploadPart(
      final String uploadId,
      final long currentPosition,
      final FileInputStream fileToUpload) throws IOException {
    byte[] buffer = new byte[partSize];
    int read = fileToUpload.read(buffer, 0, buffer.length);
    if (read < 0) {
      return new UploadResult(currentPosition, null, vaultName, uploadId, client);
    }
    byte[] bytesRead = Arrays.copyOf(buffer, read);
    return new UploadResult(currentPosition, bytesRead, vaultName, uploadId, client);
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