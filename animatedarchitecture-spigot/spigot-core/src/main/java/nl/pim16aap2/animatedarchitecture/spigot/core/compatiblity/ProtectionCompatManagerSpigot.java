package nl.pim16aap2.animatedarchitecture.spigot.core.compatiblity;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionCompatManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Class that manages all objects of {@link IProtectionCompat}.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class ProtectionCompatManagerSpigot
    implements IRestartable, Listener, IProtectionCompatManager, IDebuggable
{
    private final FakePlayerCreator fakePlayerCreator;

    private List<IProtectionHookSpigot> protectionHooks = new ArrayList<>();

    @Inject ProtectionCompatManagerSpigot(
        RestartableHolder holder,
        DebuggableRegistry debuggableRegistry,
        FakePlayerCreator fakePlayerCreator)
    {
        this.fakePlayerCreator = fakePlayerCreator;

        holder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Attempts to load protection hooks for any plugins that are currently enabled.
     * <p>
     * Plugins that will be enabled later will be handled by {@link #onPluginEnable(PluginEnableEvent)}.
     */
    private void loadHooks()
    {
    }

    /**
     * Load a compat for the plugin enabled in the event if needed.
     *
     * @param event
     *     The event of the plugin that is loaded.
     */
    @EventHandler
    void onPluginEnable(PluginEnableEvent event)
    {
    }

    /**
     * Used to apply a predicate to all registered protection hooks.
     *
     * @param predicate
     *     The predicate to apply.
     * @return The name of the protection hook that returned false, or an empty Optional if all returned true.
     */
    private Optional<String> checkForEachHook(Predicate<IProtectionHookSpigot> predicate)
    {
        for (final IProtectionHookSpigot hook : protectionHooks)
        {
            try
            {
                if (!predicate.test(hook))
                    return Optional.of(hook.getName());
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Error while checking protection hook %s", hook.getName());
                return Optional.of(hook.getName());
            }
        }
        return Optional.empty();
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
        if (protectionHooks.isEmpty())
            return Optional.empty();

        final Location bukkitLocation = SpigotAdapter.getBukkitLocation(location);
        return getPlayer(player, bukkitLocation)
            .map(bukkitPlayer -> checkForEachHook(hook -> hook.canBreakBlock(bukkitPlayer, bukkitLocation)))
            .orElseGet(() -> Optional.of("ERROR!"));
    }

    @Override
    public Optional<String> canBreakBlocksBetweenLocs(IPlayer player, Vector3Di pos0, Vector3Di pos1, IWorld world)
    {
        if (protectionHooks.isEmpty())
            return Optional.empty();

        final Location loc0 = new Location(SpigotAdapter.getBukkitWorld(world), pos0.x(), pos1.y(), pos1.z());
        final Location loc1 = new Location(SpigotAdapter.getBukkitWorld(world), pos1.x(), pos1.y(), pos1.z());

        return getPlayer(player, loc0)
            .map(bukkitPlayer -> checkForEachHook(hook -> hook.canBreakBlocksBetweenLocs(bukkitPlayer, loc0, loc1)))
            .orElseGet(() -> Optional.of("ERROR!"));
    }

    @Override
    public boolean canSkipCheck()
    {
        return protectionHooks.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reinitialize all protection compats.
     */
    @Override
    public void initialize()
    {
        this.protectionHooks = new ArrayList<>();
        loadHooks();
    }

    @Override
    public void shutDown()
    {
        protectionHooks.clear();
        this.protectionHooks = List.of();
    }

    @Override
    public String getDebugInformation()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Can create fake players: ").append(fakePlayerCreator.canCreatePlayers()).append('\n');
        sb.append("Protection hooks: \n");
        for (final IProtectionHookSpigot protectionHook : protectionHooks)
            sb.append("  ").append(protectionHook.getName()).append('\n');

        return sb.toString();
    }
}
