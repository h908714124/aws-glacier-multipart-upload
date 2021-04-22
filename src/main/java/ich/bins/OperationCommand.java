package ich.bins;

import net.jbock.Param;
import net.jbock.SuperCommand;

@SuperCommand("glacier-upload")
abstract class OperationCommand {

    @Param(0)
    abstract Operation operation();
}
