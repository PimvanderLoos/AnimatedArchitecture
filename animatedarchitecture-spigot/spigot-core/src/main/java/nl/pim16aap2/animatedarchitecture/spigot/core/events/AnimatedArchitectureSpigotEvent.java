package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.DebugReporterSpigot;
import org.bukkit.event.Event;

/**
 * Represents an event that is part of the AnimatedArchitectureSpigot event system.
 * <p>
 * Subclasses of this class should be listed in the {@link DebugReporterSpigot} class.
 */
@ToString
public abstract class AnimatedArchitectureSpigotEvent extends Event
{
    protected AnimatedArchitectureSpigotEvent()
    {
        super(true);
    }
}
