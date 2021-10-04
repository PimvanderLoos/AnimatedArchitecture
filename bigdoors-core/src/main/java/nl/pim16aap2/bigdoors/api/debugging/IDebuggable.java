package nl.pim16aap2.bigdoors.api.debugging;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that is debuggable by reporting debug information to {@link DebugReporter}.
 *
 * @author Pim
 */
public interface IDebuggable
{
    /**
     * Gets the debug information for this object.
     *
     * @return The debug information or null if there's nothing to log at this time.
     */
    @Nullable String getDebugInformation();
}
