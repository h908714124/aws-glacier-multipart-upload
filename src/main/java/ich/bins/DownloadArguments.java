package ich.bins;

import net.jbock.Command;
import net.jbock.Option;

import java.nio.file.Path;

@Command(
        name = "glacier-download",
        parseOrExitMethodAcceptsList = true)
interface DownloadArguments {

    @Option(names = "--archive-id")
    String archiveId();

    @Option(names = "--download-path")
    Path downloadPath();
}
