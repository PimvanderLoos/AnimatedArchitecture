package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpigotAdapter
{
    /**
     * Converts an {@link IPWorld} object to a {@link World} object.
     * <p>
     * If the {@link IPLocation} is an {@link PWorldSpigot}, only a simple cast is performed. Otherwise, a new {@link
     * World} is constructed.
     *
     * @param pWorld The BigDoors world.
     * @return The Spigot world.
     */
    @Nullable
    public static World getBukkitWorld(final @NotNull IPWorld pWorld)
    {
        if (pWorld instanceof PWorldSpigot)
            return ((PWorldSpigot) pWorld).getBukkitWorld();
        return Bukkit.getWorld(pWorld.getUID());
    }

    /**
     * Converts an {@link IPLocation} object to a {@link Location} object.
     * <p>
     * If the {@link IPLocation} is an {@link PLocationSpigot}, only a simple cast is performed. Otherwise, a new {@link
     * Location} is constructed.
     *
     * @param pLocation The BigDoors location.
     * @return The Spigot location.
     */
    @NotNull
    public static Location getBukkitLocation(final @NotNull IPLocation pLocation)
    {
        if (pLocation instanceof PLocationSpigot)
            return ((PLocationSpigot) pLocation).getBukkitLocation();
        return new Location(getBukkitWorld(pLocation.getWorld()), pLocation.getX(), pLocation.getY(), pLocation.getZ());
    }

    /**
     * Gets a Bukkit vector from a BigDoors vector.
     *
     * @param vector The BigDoors vector.
     * @return The bukkit vector.
     */
    @NotNull
    public static Vector getBukkitVector(final @NotNull IVector3DiConst vector)
    {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Tries to get an online Bukkit player represented by an {@link IPPlayer}.
     *
     * @param pPlayer The {@link IPPlayer}.
     * @return The online bukkit player, if possible.
     */
    @Nullable
    public static Player getBukkitPlayer(final @NotNull IPPlayer pPlayer)
    {
        return Bukkit.getPlayer(pPlayer.getUUID());
    }

    /**
     * Tries to get an offline Bukkit player represented by an {@link IPPlayer}.
     *
     * @param pPlayer The {@link IPPlayer}.
     * @return The offline bukkit player.
     */
    @NotNull
    public static OfflinePlayer getOfflineBukkitPlayer(final @NotNull IPPlayer pPlayer)
    {
        return Bukkit.getOfflinePlayer(pPlayer.getUUID());
    }

    /**
     * Gets a Bukkit vector from a BigDoors vector.
     *
     * @param vector The BigDoors vector.
     * @return The bukkit vector.
     */
    @NotNull
    public static Vector getBukkitVector(final @NotNull IVector3DdConst vector)
    {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Wraps a Bukkit player in an IPPlayer.
     *
     * @param player The Bukkit player.
     * @return The IPPlayer.
     */
    @NotNull
    public static IPPlayer wrapPlayer(final @NotNull Player player)
    {
        return new PPlayerSpigot(player);
    }

    /**
     * Wraps an offline Bukkit player in an IPPlayer.
     *
     * @param player The Bukkit player.
     * @return The IPPlayer.
     */
    @NotNull
    public static IPPlayer wrapPlayer(final @NotNull OfflinePlayer player)
    {
        return new PPlayerSpigot(player);
    }

    /**
     * Wraps a Bukkit location in an IPLocation.
     *
     * @param location The Bukkit location.
     * @return The IPLocation.
     */
    @NotNull
    public static IPLocation wrapLocation(final @NotNull Location location)
    {
        return new PLocationSpigot(location);
    }

    /**
     * Wraps a Bukkit world in an IPWorld.
     *
     * @param world The Bukkit world.
     * @return The IPWorld.
     */
    @NotNull
    public static IPWorld wrapWorld(final @NotNull World world)
    {
        return new PWorldSpigot(world);
    }
}
