package ich.bins;

import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.UploadMultipartPartRequest;
import com.amazonaws.services.glacier.model.UploadMultipartPartResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

final class UploadPartCommand implements Callable<UploadMultipartPartResult> {

  private static final int MAX_ATTEMPTS = 200;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ArchiveMPU archiveMPU;
  private final AtomicInteger numParts;
  private final AtomicInteger completed;

  private final long offset;
  private final String uploadId;

  private final byte[] bytesRead;

  final String checksum;

  UploadPartCommand(ArchiveMPU archiveMPU,
                    AtomicInteger numParts,
                    AtomicInteger completed,
                    long offset,
                    byte[] bytesRead,
                    String uploadId) {
    this.archiveMPU = archiveMPU;
    this.numParts = numParts;
    this.completed = completed;
    this.bytesRead = bytesRead;
    this.offset = offset;
    this.uploadId = uploadId;
    this.checksum = bytesRead == null ?
        null :
        TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
  }

  @Override
  public UploadMultipartPartResult call() throws Exception {
    String contentRange = String.format("bytes %d-%d/*",
        offset,
        offset + bytesRead.length - 1);
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      try {
        UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
            .withVaultName(archiveMPU.arguments.vaultName())
            .withBody(new ByteArrayInputStream(bytesRead))
            .withChecksum(checksum)
            .withRange(contentRange)
            .withUploadId(uploadId);
        UploadMultipartPartResult partResult =
            archiveMPU.client().uploadMultipartPart(partRequest);
        log.info(completed.incrementAndGet() + " of " +
            numParts.get() + " parts completed. Range: " +
            contentRange + ", checksum: " +
            partResult.getChecksum());
        return partResult;
      } catch (Exception e) {
        log.info(contentRange + " (attempt " + i + " / " + MAX_ATTEMPTS + ") failed: " + e.getMessage());
      }
    }
    throw new IllegalStateException(contentRange + ": Giving up after " + MAX_ATTEMPTS + " attempts");
  }


  int length() {
    return bytesRead == null ? 0 : bytesRead.length;
  }
}
