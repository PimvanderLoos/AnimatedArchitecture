package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.BigDoors;

public class DebugReporter
{
    /**
     * Gets the data-dump containing useful information for debugging issues.
     */
    public String getDump()
    {
        return new StringBuilder(50)
            .append("Java version: ").append(System.getProperty("java.version")).append('\n')
            .append("Registered Platform: ").append(BigDoors.get().getPlatform().getClass().getName()).append('\n')
            .toString();
    }
}
