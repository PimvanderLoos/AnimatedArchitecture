package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import lombok.ToString;
import org.bukkit.event.Event;

@ToString
public abstract class AnimatedArchitectureSpigotEvent extends Event
{
    protected AnimatedArchitectureSpigotEvent()
    {
        super(true);
    }
}
