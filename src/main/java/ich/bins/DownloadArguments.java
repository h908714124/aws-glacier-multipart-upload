package ich.bins;

import net.jbock.Command;
import net.jbock.Option;

import java.nio.file.Path;

@Command("glacier-download")
abstract class DownloadArguments extends Arguments {

    // DOWNLOAD ONLY
    @Option("archive-id")
    abstract String archiveId();

    // DOWNLOAD ONLY
    @Option("download-path")
    abstract Path downloadPath();

}
