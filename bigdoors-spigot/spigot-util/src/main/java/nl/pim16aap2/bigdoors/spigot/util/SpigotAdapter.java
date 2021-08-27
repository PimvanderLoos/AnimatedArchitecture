package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class SpigotAdapter
{
    private SpigotAdapter()
    {
        // Utility class
    }

    /**
     * Converts an {@link IPWorld} object to a {@link World} object.
     * <p>
     * If the {@link IPLocation} is an {@link PWorldSpigot}, only a simple cast is performed. Otherwise, a new {@link
     * World} is constructed.
     *
     * @param pWorld
     *     The BigDoors world.
     * @return The Spigot world.
     */
    public static @Nullable World getBukkitWorld(IPWorld pWorld)
    {
        if (pWorld instanceof PWorldSpigot)
            return ((PWorldSpigot) pWorld).getBukkitWorld();
        return Bukkit.getWorld(pWorld.worldName());
    }

    /**
     * Converts an {@link IPLocation} object to a {@link Location} object.
     * <p>
     * If the {@link IPLocation} is an {@link PLocationSpigot}, only a simple cast is performed. Otherwise, a new {@link
     * Location} is constructed.
     *
     * @param pLocation
     *     The BigDoors location.
     * @return The Spigot location.
     */
    public static Location getBukkitLocation(IPLocation pLocation)
    {
        if (pLocation instanceof PLocationSpigot)
            return ((PLocationSpigot) pLocation).getBukkitLocation();
        return new Location(getBukkitWorld(pLocation.getWorld()), pLocation.getX(), pLocation.getY(), pLocation.getZ());
    }

    /**
     * Gets a Bukkit vector from a BigDoors vector.
     *
     * @param vector
     *     The BigDoors vector.
     * @return The bukkit vector.
     */
    public static Vector getBukkitVector(Vector3Di vector)
    {
        return new Vector(vector.x(), vector.y(), vector.z());
    }

    /**
     * Tries to get an online Bukkit player represented by an {@link IPPlayer}.
     *
     * @param pPlayer
     *     The {@link IPPlayer}.
     * @return The online bukkit player, if possible.
     */
    public static @Nullable Player getBukkitPlayer(IPPlayer pPlayer)
    {
        return Bukkit.getPlayer(pPlayer.getUUID());
    }

    /**
     * Tries to get an offline Bukkit player represented by an {@link IPPlayer}.
     *
     * @param pPlayer
     *     The {@link IPPlayer}.
     * @return The offline bukkit player.
     */
    public static OfflinePlayer getOfflineBukkitPlayer(IPPlayer pPlayer)
    {
        return Bukkit.getOfflinePlayer(pPlayer.getUUID());
    }

    /**
     * Gets a Bukkit vector from a BigDoors vector.
     *
     * @param vector
     *     The BigDoors vector.
     * @return The bukkit vector.
     */
    public static Vector getBukkitVector(Vector3Dd vector)
    {
        return new Vector(vector.x(), vector.y(), vector.z());
    }

    /**
     * Wraps a Bukkit player in an IPPlayer.
     *
     * @param player
     *     The Bukkit player.
     * @return The IPPlayer.
     */
    public static IPPlayer wrapPlayer(Player player)
    {
        return new PPlayerSpigot(player);
    }

    /**
     * Wraps an offline Bukkit player in an IPPlayer.
     *
     * @param player
     *     The Bukkit player.
     * @return The IPPlayer.
     */
    public static CompletableFuture<Optional<IPPlayer>> wrapPlayer(OfflinePlayer player)
    {
        return BigDoors.get().getPlatform().getPPlayerFactory().create(player.getUniqueId());
    }

    /**
     * Wraps a Bukkit location in an IPLocation.
     *
     * @param location
     *     The Bukkit location.
     * @return The IPLocation.
     */
    public static IPLocation wrapLocation(Location location)
    {
        return new PLocationSpigot(location);
    }

    /**
     * Wraps a Bukkit world in an IPWorld.
     *
     * @param world
     *     The Bukkit world.
     * @return The IPWorld.
     */
    public static IPWorld wrapWorld(World world)
    {
        return new PWorldSpigot(world);
    }
}
