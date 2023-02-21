package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;

import java.util.Optional;

/**
 * Represents the event where a structure was created.
 *
 * @author Pim
 */
public interface IStructureEvent extends IAnimatedArchitectureEvent
{
    /**
     * Gets the {@link AbstractStructure} that was created.
     *
     * @return The {@link AbstractStructure} that will be created.
     */
    AbstractStructure getStructure();

    /**
     * Gets the {@link IPlayer} that was responsible for the creation this structure.
     *
     * @return The {@link IPlayer} that created if the creation was requested by a player. If it was requested by
     * something else (e.g. the server), an empty optional is returned.
     */
    Optional<IPlayer> getResponsible();
}
