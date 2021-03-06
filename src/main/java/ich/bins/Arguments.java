package ich.bins;

import net.jbock.Option;

import java.nio.file.Path;
import java.util.Optional;

abstract class Arguments {

    /**
     * file to upload
     * absolute or relative path
     *
     * @return FILE
     */
    @Option(names = "--file")
    abstract Path fileToUpload();

    /**
     * archive name
     * file name in vault
     *
     * @return NAME
     */
    @Option(names = "--description")
    abstract String description();

    /**
     * aws glacier vault name
     * the vault must exist
     *
     * @return VAULT
     */
    @Option(names = "--vault-name")
    abstract String vaultName();

    /**
     * aws service endpoint
     * example: 'glacier.eu-central-1.amazonaws.com'
     *
     * @return URL
     */
    @Option(names = "--service-endpoint")
    abstract String serviceEndpoint();

    /**
     * aws signing region
     * example: 'eu-central-1'
     *
     * @return REGION
     */
    @Option(names = "--signing-region")
    abstract String signingRegion();
}
