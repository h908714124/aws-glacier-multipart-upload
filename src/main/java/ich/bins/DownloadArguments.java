package ich.bins;

import net.jbock.Command;
import net.jbock.Option;

import java.nio.file.Path;

@Command(name = "glacier-download")
abstract class DownloadArguments extends Arguments {

    @Option(names = "--archive-id")
    abstract String archiveId();

    @Option(names = "--download-path")
    abstract Path downloadPath();

}
