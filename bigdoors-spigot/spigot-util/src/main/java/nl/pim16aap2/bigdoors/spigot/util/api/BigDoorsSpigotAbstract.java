package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents the implementation of {@link IBigDoorsPlatform} for the Spigot platform.
 *
 * @author Pim
 */
public abstract class BigDoorsSpigotAbstract extends JavaPlugin
    implements Listener, IBigDoorsPlatform
{
}
