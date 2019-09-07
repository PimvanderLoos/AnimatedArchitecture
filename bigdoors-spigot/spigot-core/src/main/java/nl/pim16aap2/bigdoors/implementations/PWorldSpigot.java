package nl.pim16aap2.bigdoors.implementations;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigotutil.IPWorldSpigot;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an implementation of {@link IPWorld} for the Spigot platform.
 *
 * @author Pim
 */
public class PWorldSpigot implements IPWorld, IPWorldSpigot
{
    @NotNull
    private final UUID uuid;
    @Nullable
    private final World world;

    public PWorldSpigot(final @NotNull UUID worldUUID)
    {
        uuid = worldUUID;
        World bukkitWorld = Bukkit.getWorld(worldUUID);
        if (bukkitWorld == null)
            PLogger.get().logException(
                new NullPointerException("World \"" + worldUUID.toString() + "\" could not be found!"));
        world = Bukkit.getWorld(worldUUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getUID()
    {
        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public World getBukkitWorld()
    {
        return world;
    }
}
