package ich.bins;

import net.jbock.Parameter;
import net.jbock.SuperCommand;

/**
 * Some basic glacier operations.
 */
@SuperCommand(name = "glacier")
abstract class OperationCommand {

    @Parameter(index = 0)
    abstract Operation operation();
}
