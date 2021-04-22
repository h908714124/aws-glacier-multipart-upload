package ich.bins;

import net.jbock.Command;
import net.jbock.Option;

import java.nio.file.Path;
import java.util.Optional;

@Command("glacier-upload")
abstract class Arguments {

  /**
   * file to upload
   * absolute or relative path
   *
   * @return FILE
   */
  @Option("file")
  abstract Path fileToUpload();

  /**
   * archive name
   * file name in vault
   *
   * @return NAME
   */
  @Option("description")
  abstract String description();

  /**
   * aws glacier vault name
   * the vault must exist
   *
   * @return VAULT
   */
  @Option("vault-name")
  abstract String vaultName();

  /**
   * aws service endpoint
   * example: 'glacier.eu-central-1.amazonaws.com'
   *
   * @return URL
   */
  @Option("service-endpoint")
  abstract String serviceEndpoint();

  /**
   * aws signing region
   * example: 'eu-central-1'
   *
   * @return REGION
   */
  @Option("signing-region")
  abstract String signingRegion();

  // DOWNLOAD ONLY
  @Option("archive-id")
  abstract Optional<String> archiveId();

  // DOWNLOAD ONLY
  @Option("download-path")
  abstract Optional<Path> downloadPath();
}
