package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.IDoorBase;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a type of door that doesn't move. I.e. a clock.
 *
 * @author Pim
 */
public interface IStationaryDoorArchetype extends IDoorBase
{
    @Override
    default @NotNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboidCopy());
    }

    @Override
    default @NotNull Vector2Di[] calculateChunkRange()
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
    default @NotNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }
}
