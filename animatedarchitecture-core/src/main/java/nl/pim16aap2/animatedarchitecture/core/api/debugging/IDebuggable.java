package nl.pim16aap2.animatedarchitecture.core.api.debugging;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that is debuggable by reporting debug information to {@link DebugReporter}.
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
