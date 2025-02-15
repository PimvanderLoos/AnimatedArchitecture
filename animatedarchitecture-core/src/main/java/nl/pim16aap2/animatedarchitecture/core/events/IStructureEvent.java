package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;

import java.util.Optional;

/**
 * Represents the event where a structure was created.
 */
public interface IStructureEvent extends IAnimatedArchitectureEvent
{
    /**
     * Gets the {@link Structure} that was created.
     *
     * @return The {@link Structure} that will be created.
     */
    Structure getStructure();

    /**
     * Gets the {@link IPlayer} that was responsible for the creation this structure.
     *
     * @return The {@link IPlayer} that created if the creation was requested by a player. If it was requested by
     * something else (e.g. the server), an empty optional is returned.
     */
    Optional<IPlayer> getResponsible();
}
