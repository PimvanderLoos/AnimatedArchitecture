package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEvent;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents an event related to a structure on the Spigot platform.
 * <p>
 * Each event has a structure and an optional responsible player.
 * <p>
 * For example, when deleting a structure, the responsible player is the player who deleted the structure.
 * <p>
 * However, when a structure is being toggled by a redstone signal, the responsible player is empty.
 */
abstract class StructureEvent extends AnimatedArchitectureSpigotEvent implements IStructureEvent
{
    /**
     * The structure that is targeted by the event.
     */
    @Getter
    protected final Structure structure;

    /**
     * The player responsible for the event.
     * <p>
     * If the event is caused by a player, such as a player deleting a structure, the responsible player will be set.
     * <p>
     * If the event is not caused by a player, this will be empty. This can be the case when a structure is toggled by a
     * redstone signal.
     *
     * @return The player responsible for the event or empty if the event is not caused by a player.
     */
    @Getter
    protected final Optional<IPlayer> responsible;

    protected StructureEvent(Structure structure, @Nullable IPlayer responsible)
    {
        this.structure = structure;
        this.responsible = Optional.ofNullable(responsible);
    }
}
