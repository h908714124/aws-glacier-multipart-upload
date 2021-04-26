package ich.bins;

import net.jbock.Param;
import net.jbock.SuperCommand;

/**
 * Some basic glacier operations.
 */
@SuperCommand("glacier")
abstract class OperationCommand {

    @Param(0)
    abstract Operation operation();
}
