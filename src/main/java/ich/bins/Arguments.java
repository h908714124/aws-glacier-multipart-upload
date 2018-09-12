package ich.bins;

import net.jbock.CommandLineArguments;
import net.jbock.Parameter;

import java.nio.file.Path;

/**
 * I have not used it in a while.
 * It needs some testing.
 */
@CommandLineArguments(
    missionStatement = "Upload files to amazon glacier",
    programName = "glacier-upload")
abstract class Arguments {

  /**
   * file to upload
   * absolute or relative path
   *
   * @return FILE
   */
  @Parameter(longName = "file")
  abstract Path fileToUpload();

  /**
   * archive name
   * file name in vault
   *
   * @return NAME
   */
  @Parameter(longName = "description")
  abstract String description();

  /**
   * aws glacier vault name
   * the vault must exist
   *
   * @return VAULT
   */
  @Parameter(longName = "vault-name")
  abstract String vaultName();

  /**
   * aws service endpoint
   * example: 'glacier.eu-central-1.amazonaws.com'
   *
   * @return URL
   */
  @Parameter(longName = "service-endpoint")
  abstract String serviceEndpoint();

  /**
   * aws signing region
   * example: 'eu-central-1'
   *
   * @return REGION
   */
  @Parameter(longName = "signing-region")
  abstract String signingRegion();
}
