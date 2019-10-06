package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BigDoorsSpigotAbstract extends JavaPlugin
    implements Listener, IRestartableHolder, IBigDoorsPlatform
{
}
