package ich.bins;

import net.jbock.CommandLineArguments;
import net.jbock.Description;
import net.jbock.LongName;

@CommandLineArguments(
    missionStatement = "Upload files to amazon glacier",
    programName = "glacier-upload")
abstract class Arguments {

  @LongName("file")
  @Description(argumentName = "FILE", value = {
      "file to upload",
      "absolute or relative path"})
  abstract String fileToUpload();

  @LongName("description")
  @Description(argumentName = "NAME", value = {
      "archive name",
      "file name in vault"})
  abstract String description();

  @LongName("vault-name")
  @Description(argumentName = "VAULT", value = {
      "aws glacier vault name",
      "the vault must exist"})
  abstract String vaultName();

  @LongName("service-endpoint")
  @Description({
      "aws service endpoint",
      "example: 'glacier.eu-central-1.amazonaws.com'"})
  abstract String serviceEndpoint();

  @LongName("signing-region")
  @Description(argumentName = "REGION", value = {
      "aws signing region",
      "example: 'eu-central-1'"})
  abstract String signingRegion();
}
