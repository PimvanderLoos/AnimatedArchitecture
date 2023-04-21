package nl.pim16aap2.animatedarchitecture.spigot.util;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.LocationSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.PlayerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.SpigotServer;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WorldSpigot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Class that contains utility methods to convert between AnimatedArchitecture and Spigot objects.
 * <p>
 * For example, {@link #getBukkitWorld(IWorld)} converts an {@link IWorld} to a {@link World} and
 * {@link #wrapWorld(World)} can be used to do the opposite.
 */
public final class SpigotAdapter
{
    private SpigotAdapter()
    {
        // Utility class
    }

    /**
     * Converts an {@link IWorld} object to a {@link World} object.
     * <p>
     * If the {@link ILocation} is an {@link WorldSpigot}, only a simple cast is performed. Otherwise, a new
     * {@link World} is constructed.
     *
     * @param world
     *     The AnimatedArchitecture world.
     * @return The Spigot world.
     */
    public static @Nullable World getBukkitWorld(IWorld world)
    {
        if (world instanceof WorldSpigot worldSpigot)
            return worldSpigot.getBukkitWorld();
        return Bukkit.getWorld(world.worldName());
    }

    /**
     * Converts an {@link ILocation} object to a {@link Location} object.
     * <p>
     * If the {@link ILocation} is an {@link LocationSpigot}, only a simple cast is performed. Otherwise, a new
     * {@link Location} is constructed.
     *
     * @param location
     *     The AnimatedArchitecture location.
     * @return The Spigot location.
     */
    public static Location getBukkitLocation(ILocation location)
    {
        if (location instanceof LocationSpigot)
            return ((LocationSpigot) location).getBukkitLocation();
        return new Location(getBukkitWorld(location.getWorld()), location.getX(), location.getY(), location.getZ());
    }

    /**
     * Gets a Bukkit vector from a AnimatedArchitecture vector.
     *
     * @param vector
     *     The AnimatedArchitecture vector.
     * @return The bukkit vector.
     */
    public static Vector getBukkitVector(Vector3Di vector)
    {
        return new Vector(vector.x(), vector.y(), vector.z());
    }

    /**
     * Tries to get an online Bukkit player represented by an {@link IPlayer}.
     *
     * @param player
     *     The {@link IPlayer}.
     * @return The online bukkit player, if possible.
     */
    public static @Nullable Player getBukkitPlayer(IPlayer player)
    {
        return Bukkit.getPlayer(player.getUUID());
    }

    /**
     * Tries to convert an {@link IPlayer} to a {@link PlayerSpigot}.
     *
     * @param player
     *     The player object to convert.
     * @return The converted player object, or null if that was not possible.
     */
    public static @Nullable PlayerSpigot getPlayerSpigot(IPlayer player)
    {
        if (player instanceof PlayerSpigot playerSpigot)
            return playerSpigot;
        final @Nullable Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
            return null;
        return new PlayerSpigot(bukkitPlayer);
    }

    /**
     * Tries to get an offline Bukkit player represented by an {@link IPlayer}.
     *
     * @param player
     *     The {@link IPlayer}.
     * @return The offline bukkit player.
     */
    public static OfflinePlayer getOfflineBukkitPlayer(IPlayer player)
    {
        return Bukkit.getOfflinePlayer(player.getUUID());
    }

    /**
     * Gets a Bukkit vector from a AnimatedArchitecture vector.
     *
     * @param vector
     *     The AnimatedArchitecture vector.
     * @return The bukkit vector.
     */
    public static Vector getBukkitVector(Vector3Dd vector)
    {
        return new Vector(vector.x(), vector.y(), vector.z());
    }

    /**
     * Wraps a Bukkit {@link CommandSender} in an {@link ICommandSender}.
     *
     * @param commandSender
     *     The Bukkit command sender.
     * @return The wrapped command sender.
     */
    public static ICommandSender wrapCommandSender(CommandSender commandSender)
    {
        return commandSender instanceof Player player ? new PlayerSpigot(player) : new SpigotServer();
    }

    /**
     * Unwraps a {@link ICommandSender} into an Bukkit {@link CommandSender}.
     *
     * @param commandSender
     *     The command sender.
     * @return The unwrapped bukkit command sender.
     */
    public static CommandSender unwrapCommandSender(ICommandSender commandSender)
    {
        if (commandSender instanceof PlayerSpigot playerSpigot)
            return playerSpigot.getBukkitPlayer();
        if (commandSender instanceof SpigotServer)
            return Bukkit.getServer().getConsoleSender();
        throw new IllegalArgumentException("Trying to unwrap command sender of illegal type: " +
                                               commandSender.getClass().getName());
    }

    /**
     * Wraps a Bukkit player in an IPlayer.
     *
     * @param player
     *     The Bukkit player.
     * @return The IPlayer.
     */
    public static IPlayer wrapPlayer(Player player)
    {
        return new PlayerSpigot(player);
    }

    /**
     * Wraps an offline Bukkit player in an IPlayer.
     *
     * @param player
     *     The Bukkit player.
     * @return The IPlayer.
     */
    public static CompletableFuture<Optional<IPlayer>> wrapPlayer(IPlayerFactory factory, OfflinePlayer player)
    {
        return factory.create(player.getUniqueId());
    }

    /**
     * Wraps a Bukkit location in an ILocation.
     *
     * @param location
     *     The Bukkit location.
     * @return The ILocation.
     */
    public static ILocation wrapLocation(Location location)
    {
        return new LocationSpigot(location);
    }

    /**
     * Wraps a Bukkit world in an IWorld.
     *
     * @param world
     *     The Bukkit world.
     * @return The IWorld.
     */
    public static IWorld wrapWorld(World world)
    {
        return new WorldSpigot(world);
    }
}
