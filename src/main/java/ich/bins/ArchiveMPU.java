package ich.bins;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.UploadMultipartPartResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.util.BinaryUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ArchiveMPU implements Closeable {

    private static final int partSize = 1048576; // 1 MB.
    private static final int clientLife = 60; // see comment below
    private static final int threads = 4;

    private final Logger log = Logger.getLogger(getClass().getName());

    final OperationCommand command;

    private ArchiveMPU(OperationCommand command) {
        this.command = command;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        OperationCommand command = new OperationCommandParser().parseOrExit(args);
        ArchiveMPU m = new ArchiveMPU(command);
        switch (command.operation()) {
            case UPLOAD -> m.runUpload(new UploadArgumentsParser().parseOrExit(command.rest()));
            case DOWNLOAD -> m.runDownload(new DownloadArgumentsParser().parseOrExit(command.rest()));
        }
    }

    private void runDownload(DownloadArguments download) {
        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().withGlacierClient(client())
                .build();
        atm.download(
                command.vaultName(),
                download.archiveId(),
                download.downloadPath().toFile());
        log.info("Downloaded file to " + download.downloadPath());
    }

    private void runUpload(UploadArguments upload) throws IOException, InterruptedException {
        log.info("File size: " + upload.fileToUpload().toFile().length());
        InitiateMultipartUploadResult initiateUploadResult = initiateMultipartUpload(upload);
        log.info(initiateUploadResult.toString());
        String uploadId = initiateUploadResult.getUploadId();
        String checksum = uploadParts(upload, uploadId);
        CompleteMultipartUploadResult result = completeMultiPartUpload(
                upload, uploadId, checksum);
        log.info("Upload finished: " + result);
    }

    private AmazonGlacier client = null;
    private final AtomicLong connectionCount = new AtomicLong();

    synchronized AmazonGlacier client() {
        if (client == null) {
            client = _client();
        }
        if (connectionCount.incrementAndGet() % clientLife == 0) {
            // not sure why but it seemed that without this, connections would degrade over time
            client.shutdown();
            client = _client();
        }
        return client;
    }

    private AmazonGlacier _client() {
        return AmazonGlacierClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                command.serviceEndpoint(),
                                command.signingRegion()))
                .withClientConfiguration(new ClientConfiguration())
                .build();
    }

    private InitiateMultipartUploadResult initiateMultipartUpload(UploadArguments upload) {
        // Initiate
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest()
                .withVaultName(command.vaultName())
                .withArchiveDescription(upload.description())
                .withPartSize(Integer.toString(partSize));

        return client().initiateMultipartUpload(request);
    }

    private String uploadParts(
            UploadArguments upload,
            String uploadId) throws
            AmazonClientException,
            IOException, InterruptedException {
        long currentPosition = 0;
        List<UploadPartCommand> commands = new LinkedList<>();

        File file = upload.fileToUpload().toFile();

        AtomicInteger numParts = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();

        try (InputStream fileToUpload = new FileInputStream(file)) {
            while (currentPosition < file.length()) {
                UploadPartCommand command = uploadPart(numParts,
                        completed,
                        uploadId,
                        currentPosition,
                        fileToUpload);
                if (command.length() == 0) {
                    break;
                }
                commands.add(command);
                currentPosition += command.length();
            }
        }

        long totalLength =
                commands.stream().mapToInt(UploadPartCommand::length).sum();

        if (totalLength != file.length()) {
            throw new IllegalStateException("File size is " + file.length() +
                    " but sum of parts is " + totalLength);
        }

        List<byte[]> binaryChecksums = commands.stream()
                .map(command -> BinaryUtils.fromHex(command.checksum))
                .collect(Collectors.toList());
        numParts.set(binaryChecksums.size());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<UploadMultipartPartResult>> futures = pool.invokeAll(commands);
        boolean success = futures.stream().allMatch(f -> {
            try {
                f.get();
                return true;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error", e);
                return false;
            }
        });
        pool.shutdown();
        if (success) {
            return TreeHashGenerator.calculateTreeHash(binaryChecksums);
        } else {
            throw new IllegalStateException("Some uploads have failed");
        }
    }

    private UploadPartCommand uploadPart(
            AtomicInteger numParts,
            AtomicInteger completed,
            final String uploadId,
            final long currentPosition,
            final InputStream fileToUpload) throws IOException {
        byte[] buffer = new byte[partSize];
        int read = fileToUpload.read(buffer, 0, buffer.length);
        if (read < 0) {
            return new UploadPartCommand(this, numParts, completed, currentPosition, null, uploadId);
        }
        byte[] bytesRead = Arrays.copyOf(buffer, read);
        return new UploadPartCommand(this, numParts, completed, currentPosition, bytesRead, uploadId);
    }

    private CompleteMultipartUploadResult completeMultiPartUpload(
            UploadArguments upload,
            String uploadId,
            String checksum) {

        File file = upload.fileToUpload().toFile();

        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest()
                .withVaultName(command.vaultName())
                .withUploadId(uploadId)
                .withChecksum(checksum)
                .withArchiveSize(String.valueOf(file.length()));

        return client().completeMultipartUpload(compRequest);
    }

    @Override
    public void close() {
        client().shutdown();
    }
}
