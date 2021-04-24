package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;

public abstract class DebugReporter
{
    /**
     * Gets the datadump containing useful information for debugging issues.
     */
    public @NonNull String getDump()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Java version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Registered Platform: ")
          .append(BigDoors.get().getPlatform() == null ? "NONE!" : BigDoors.get().getPlatform().getClass().getName())
          .append("\n");
        return sb.toString();
    }
}
