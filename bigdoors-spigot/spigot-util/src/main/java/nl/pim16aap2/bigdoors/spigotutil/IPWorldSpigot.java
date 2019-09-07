package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a BigDoors world for the Spigot platform.
 *
 * @author Pim
 */
public interface IPWorldSpigot
{
    /**
     * Gets the Bukkit World backing this {@link IPWorldSpigot}.
     *
     * @return The Bukkit World.
     */
    @Nullable
    World getBukkitWorld();
}
