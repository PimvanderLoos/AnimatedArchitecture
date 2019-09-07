package nl.pim16aap2.bigdoors.spigotutil;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpigotAdapter
{
    /**
     * Converts an {@link IPWorld} object to a {@link World} object.
     *
     * @param pWorld The BigDoors world.
     * @return The Spigot world.
     */
    @Nullable
    public static World getBukkitWorld(final @NotNull IPWorld pWorld)
    {
        if (pWorld instanceof IPWorldSpigot)
            return ((IPWorldSpigot) pWorld).getBukkitWorld();
        return Bukkit.getWorld(pWorld.getUID());
    }

    /**
     * Converts an {@link IPLocation} object to a {@link Location} object.
     *
     * @param pLocation The BigDoors location.
     * @return The Spigot location.
     */
    @NotNull
    public static Location getBukkitLocation(final @NotNull IPLocation pLocation)
    {
        if (pLocation instanceof IPLocationSpigot)
            return ((IPLocationSpigot) pLocation).getBukkitLocation();
        return new Location(getBukkitWorld(pLocation.getWorld()), pLocation.getX(), pLocation.getY(), pLocation.getZ());
    }

    /**
     * Gets a Bukkit vector from a BigDoors vector.
     *
     * @param vector The BigDoors vector.
     * @return The bukkit vector.
     */
    @NotNull
    public static Vector getBukkitVector(final @NotNull Vector3Di vector)
    {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Gets a Bukkit vector from a BigDoors vector.
     *
     * @param vector The BigDoors vector.
     * @return The bukkit vector.
     */
    @NotNull
    public static Vector getBukkitVector(final @NotNull Vector3Dd vector)
    {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }
}
