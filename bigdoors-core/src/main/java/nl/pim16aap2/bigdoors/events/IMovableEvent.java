package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;

import java.util.Optional;

/**
 * Represents the event where a movable was created.
 *
 * @author Pim
 */
public interface IMovableEvent extends IBigDoorsEvent
{
    /**
     * Gets the {@link AbstractMovable} that was created.
     *
     * @return The {@link AbstractMovable} that will be created.
     */
    AbstractMovable getMovable();

    /**
     * Gets the {@link IPPlayer} that was responsible for the creation this movable.
     *
     * @return The {@link IPPlayer} that created if the creation was requested by a player. If it was requested by
     * something else (e.g. the server), an empty optional is returned.
     */
    Optional<IPPlayer> getResponsible();
}
