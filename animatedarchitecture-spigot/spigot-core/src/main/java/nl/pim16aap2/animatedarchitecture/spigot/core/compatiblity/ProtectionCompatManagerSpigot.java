package nl.pim16aap2.animatedarchitecture.spigot.core.compatiblity;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionCompatManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Class that manages all objects of {@link IProtectionCompat}.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class ProtectionCompatManagerSpigot extends Restartable implements Listener, IProtectionCompatManager
{
    private final FakePlayerCreator fakePlayerCreator;

    @Inject ProtectionCompatManagerSpigot(RestartableHolder holder, FakePlayerCreator fakePlayerCreator)
    {
        super(holder);
        this.fakePlayerCreator = fakePlayerCreator;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reinitialize all protection compats.
     */
    @Override
    public void initialize()
    {
    }

    @Override
    public void shutDown()
    {
    }

    /**
     * Get the player to use for protection checks.
     * <p>
     * This will return the online player if they are online, otherwise it will return a fake-online player.
     *
     * @param player
     *     The player to get the bukkit player for.
     * @param location
     *     The location to provide for the fake player.
     * @return The player to use for protection checks.
     */
    private Optional<Player> getPlayer(IPlayer player, Location location)
    {
        final OfflinePlayer offlinePlayer = SpigotAdapter.getOfflineBukkitPlayer(player);
        final @Nullable Player onlinePlayer = offlinePlayer.getPlayer();
        if (onlinePlayer != null)
            return Optional.of(onlinePlayer);
        return fakePlayerCreator.createPlayer(offlinePlayer, location);
    }

    @Override
    public Optional<String> canBreakBlock(IPlayer player, ILocation location)
    {
        final Optional<Player> playerOptional = getPlayer(player, SpigotAdapter.getBukkitLocation(location));
        if (playerOptional.isEmpty())
            return Optional.of("ERROR!");
        return Optional.empty();
    }

    @Override
    public Optional<String> canBreakBlocksBetweenLocs(IPlayer player, Vector3Di pos1, Vector3Di pos2, IWorld world)
    {
        final Location location = new Location(SpigotAdapter.getBukkitWorld(world), pos1.x(), pos1.y(), pos1.z());
        final Optional<Player> playerOptional = getPlayer(player, location);
        if (playerOptional.isEmpty())
            return Optional.of("ERROR!");
        return Optional.empty();
    }

    @Override
    public boolean canSkipCheck()
    {
        return true;
    }

    /**
     * Load a compat for the plugin enabled in the event if needed.
     *
     * @param event
     *     The event of the plugin that is loaded.
     */
    @SuppressWarnings("unused")
    @EventHandler
    void onPluginEnable(PluginEnableEvent event)
    {
    }
}
