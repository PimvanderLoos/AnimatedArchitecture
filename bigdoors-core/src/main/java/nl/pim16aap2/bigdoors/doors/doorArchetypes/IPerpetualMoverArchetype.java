package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.IDoorBase;

/**
 * Represents doors that are always active (e.g. clocks, windmills).
 *
 * @author Pim
 */
public interface IPerpetualMoverArchetype extends IDoorBase
{
    /**
     * {@inheritDoc}
     */
    @Override
    default boolean perpetualMovement()
    {
        return true;
    }
}
