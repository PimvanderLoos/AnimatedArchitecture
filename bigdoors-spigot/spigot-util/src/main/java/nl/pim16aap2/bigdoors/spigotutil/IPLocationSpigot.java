package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a BigDoors location for the Spigot platform.
 *
 * @author Pim
 */
public interface IPLocationSpigot
{
    /**
     * Gets the Bukkit World backing this {@link IPLocationSpigot}.
     *
     * @return The Bukkit World.
     */
    @NotNull
    Location getBukkitLocation();
}
