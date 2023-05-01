package ich.bins;

import net.jbock.Option;
import net.jbock.Parameter;
import net.jbock.SuperCommand;
import net.jbock.VarargsParameter;

import java.nio.file.Path;
import java.util.List;

/**
 * Some basic glacier operations.
 */
@SuperCommand(name = "glacier")
interface OperationCommand {

    @Parameter(index = 0)
    Operation operation();

    /**
     * aws glacier vault name
     * the vault must exist
     *
     * @return VAULT
     */
    @Option(names = "--vault-name")
    String vaultName();

    /**
     * aws service endpoint
     * example: 'glacier.eu-central-1.amazonaws.com'
     *
     * @return URL
     */
    @Option(names = "--service-endpoint")
    String serviceEndpoint();

    /**
     * aws signing region
     * example: 'eu-central-1'
     *
     * @return REGION
     */
    @Option(names = "--signing-region")
    String signingRegion();

    @VarargsParameter
    List<String> rest();
}
