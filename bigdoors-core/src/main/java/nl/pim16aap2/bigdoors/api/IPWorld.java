package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface IPWorld
{
    /**
     * Gets the UUID of this world.
     *
     * @return The UUID of this world.
     */
    @NotNull
    UUID getUID();
}
