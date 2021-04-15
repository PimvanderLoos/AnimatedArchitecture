package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.IDoorBase;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;

import java.util.Optional;

/**
 * Represents a type of door that doesn't move. I.e. a clock.
 *
 * @author Pim
 */
public interface IStationaryDoorArchetype extends IDoorBase
{
    @Override
    default @NonNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid().clone());
    }

    @Override
    default @NonNull Vector2Di[] calculateChunkRange()
    {
        return calculateCurrentChunkRange();
    }

    @Override
    default boolean canSkipAnimation()
    {
        return false;
    }

    @Override
    default boolean isOpenable()
    {
        return true;
    }

    @Override
    default boolean isCloseable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always the same as {@link #getOpenDir()}, as this archetype makes no distinction between opening and closing.
     */
    @Override
    default @NonNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }
}
