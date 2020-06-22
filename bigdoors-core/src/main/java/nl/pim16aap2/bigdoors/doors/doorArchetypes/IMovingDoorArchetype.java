package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.IDoorBase;

/**
 * Represents a type of door that can move. I.e. a sliding door.
 *
 * @author Pim
 */
public interface IMovingDoorArchetype extends IDoorBase
{
    /** {@inheritDoc} */
    @Override
    default boolean canSkipAnimation()
    {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    default boolean isOpenable()
    {
        return !isOpen();
    }

    /** {@inheritDoc} */
    @Override
    default boolean isCloseable()
    {
        return isOpen();
    }
}
