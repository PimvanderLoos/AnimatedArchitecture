package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;

public abstract class DebugReporter
{
    /**
     * Gets the datadump containing useful information for debugging issues.
     */
    public abstract @NonNull String getDump();
}
