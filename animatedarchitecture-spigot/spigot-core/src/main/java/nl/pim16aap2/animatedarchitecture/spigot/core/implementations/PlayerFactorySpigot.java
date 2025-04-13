package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.SpigotServer;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedOfflinePlayer;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPlayerFactory} for the Spigot platform.
 */
@Singleton
@Flogger
public class PlayerFactorySpigot implements IPlayerFactory
{
    private final TimedCache<UUID, WrappedPlayer> playerCache = TimedCache
        .<UUID, WrappedPlayer>builder()
        .softReference(true)
        .timeOut(Duration.ofMinutes(5))
        .cleanup(Duration.ofMinutes(5))
        .refresh(true)
        .build();

    private final SpigotServer spigotServer;
    private final DatabaseManager databaseManager;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;

    @Inject
    public PlayerFactorySpigot(
        SpigotServer spigotServer,
        DatabaseManager databaseManager,
        ILocalizer localizer,
        ITextFactory textFactory
    )
    {
        this.spigotServer = spigotServer;
        this.databaseManager = databaseManager;
        this.localizer = localizer;
        this.textFactory = textFactory;
    }

    /**
     * Tries to get an online Bukkit player represented by an {@link IPlayer}.
     *
     * @param player
     *     The {@link IPlayer}.
     * @return The online bukkit player, if possible.
     */
    public static @Nullable Player unwrapPlayer(IPlayer player)
    {
        if (player instanceof WrappedPlayer playerSpigot)
            return playerSpigot.getBukkitPlayer();

        return Bukkit.getPlayer(player.getUUID());
    }

    /**
     * Tries to get an offline Bukkit player represented by an {@link IPlayer}.
     *
     * @param player
     *     The {@link IPlayer}.
     * @return The offline bukkit player.
     */
    public static OfflinePlayer unwrapOfflinePlayer(IPlayer player)
    {
        if (player instanceof WrappedPlayer playerSpigot)
            return playerSpigot.getBukkitPlayer();

        if (player instanceof WrappedOfflinePlayer offlinePlayerSpigot)
            return offlinePlayerSpigot.getBukkitPlayer();

        return Bukkit.getOfflinePlayer(player.getUUID());
    }

    /**
     * Unwraps a {@link ICommandSender} into a Bukkit {@link CommandSender}.
     *
     * @param commandSender
     *     The command sender.
     * @return The unwrapped bukkit command sender.
     */
    public static CommandSender unwrapCommandSender(ICommandSender commandSender)
    {
        if (commandSender instanceof WrappedPlayer playerSpigot)
            return playerSpigot.getBukkitPlayer();

        if (commandSender instanceof SpigotServer)
            return Bukkit.getServer().getConsoleSender();

        throw new IllegalArgumentException(
            "Trying to unwrap command sender of illegal type: " + commandSender.getClass().getName());
    }

    /**
     * Gets the {@link WrappedPlayer} instance for the given {@link IPlayer}.
     *
     * @param player
     *     The player.
     * @return The {@link WrappedPlayer} instance, or null if no online player was found.
     */
    public @Nullable WrappedPlayer wrapPlayer(IPlayer player)
    {
        if (player instanceof WrappedPlayer playerSpigot)
            return playerSpigot;

        final @Nullable Player onlinePlayer = Bukkit.getPlayer(player.getUUID());
        if (onlinePlayer != null)
            return wrapPlayer(onlinePlayer);

        return null;
    }

    /**
     * Creates a new {@link WrappedPlayer} instance that wraps a Bukkit {@link Player}.
     *
     * @param player
     *     The Bukkit player.
     * @return The new {@link WrappedPlayer} instance.
     */
    // 'computeIfAbsent' is marked as nullable, but that can only happen when
    // the mapping function returns null, which it doesn't.
    @SuppressWarnings("NullAway")
    public WrappedPlayer wrapPlayer(Player player)
    {
        //noinspection DataFlowIssue,deprecation
        return playerCache.computeIfAbsent(
            player.getUniqueId(),
            uuid -> new WrappedPlayer(player, localizer, textFactory)
        );
    }

    /**
     * Wraps an offline Bukkit player in an IPlayer.
     *
     * @param player
     *     The Bukkit player.
     * @return The IPlayer.
     */
    public CompletableFuture<Optional<IPlayer>> wrapPlayer(OfflinePlayer player)
    {
        return create(player.getUniqueId());
    }

    /**
     * Wraps a Bukkit {@link CommandSender} in an {@link ICommandSender}.
     *
     * @param commandSender
     *     The Bukkit command sender.
     * @return The wrapped command sender.
     */
    public ICommandSender wrapCommandSender(CommandSender commandSender)
    {
        return commandSender instanceof Player bukkitPlayer ?
            wrapPlayer(bukkitPlayer) :
            spigotServer;
    }

    @Override
    public IPlayer create(PlayerData playerData)
    {
        final @Nullable Player player = Bukkit.getPlayer(playerData.getUUID());
        if (player != null)
            return wrapPlayer(player);
        return new WrappedOfflinePlayer(playerData);
    }

    @Override
    public CompletableFuture<Optional<IPlayer>> create(UUID uuid)
    {
        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            return CompletableFuture.completedFuture(Optional.of(wrapPlayer(player)));

        return databaseManager
            .getPlayerData(uuid)
            .thenApply(playerData -> playerData.<IPlayer>map(WrappedOfflinePlayer::new))
            .exceptionally(ex ->
            {
                log.atSevere().withCause(ex).log("Failed to create player for UUID: %s", uuid);
                return Optional.empty();
            });
    }
}
