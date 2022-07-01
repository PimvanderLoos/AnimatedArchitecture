package nl.pim16aap2.bigdoors.spigot.compatiblity;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

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
    @Inject
    public ProtectionCompatManagerSpigot(RestartableHolder holder)
    {
        super(holder);
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

    @Override
    public Optional<String> canBreakBlock(IPPlayer player, IPLocation pLoc)
    {
        return Optional.empty();
    }

    @Override
    public Optional<String> canBreakBlocksBetweenLocs(IPPlayer player, Vector3Di pos1, Vector3Di pos2, IPWorld world)
    {
        return Optional.empty();
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
