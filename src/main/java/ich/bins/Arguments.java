package ich.bins;

import java.nio.file.Path;

interface Arguments {

    /**
     * file to upload
     * absolute or relative path
     *
     * @return FILE
     */
    Path fileToUpload();

    /**
     * archive name
     * file name in vault
     *
     * @return NAME
     */
    String description();

    /**
     * aws glacier vault name
     * the vault must exist
     *
     * @return VAULT
     */
    String vaultName();

    /**
     * aws service endpoint
     * example: 'glacier.eu-central-1.amazonaws.com'
     *
     * @return URL
     */
    String serviceEndpoint();

    /**
     * aws signing region
     * example: 'eu-central-1'
     *
     * @return REGION
     */
    String signingRegion();
}
