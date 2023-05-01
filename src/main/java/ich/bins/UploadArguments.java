package ich.bins;

import net.jbock.Command;
import net.jbock.Option;

import java.nio.file.Path;

@Command(
        name = "glacier-upload",
        parseOrExitMethodAcceptsList = true)
interface UploadArguments {

    /**
     * file to upload
     * absolute or relative path
     *
     * @return FILE
     */
    @Option(names = "--file")
    Path fileToUpload();

    /**
     * archive name
     * file name in vault
     *
     * @return NAME
     */
    @Option(names = "--description")
    String description();
}
