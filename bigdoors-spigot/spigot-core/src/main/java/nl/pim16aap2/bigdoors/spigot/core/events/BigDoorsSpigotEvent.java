package nl.pim16aap2.bigdoors.spigot.core.events;

import lombok.ToString;
import org.bukkit.event.Event;

@ToString
public abstract class BigDoorsSpigotEvent extends Event
{
    protected BigDoorsSpigotEvent()
    {
        super(true);
    }
}
