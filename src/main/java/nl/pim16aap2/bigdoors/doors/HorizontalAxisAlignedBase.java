package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;

/**
 * Represents all door types that are aligned on the North/South or East/West
 * axis. For example: {@link nl.pim16aap2.bigdoors.doors.SlidingDoor}.
 * <p>
 * Only doors with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see DoorBase
 */
abstract class HorizontalAxisAlignedBase extends DoorBase
{
    // Check if this door is positioned along the North/South axis
    private Boolean northSouthAxis = null;

    HorizontalAxisAlignedBase(BigDoors plugin, long doorUID, DoorType doorType)
    {
        super(plugin, doorUID, doorType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateCoordsDependents()
    {
        super.updateCoordsDependents();
        northSouthAxis = null;
        // EngineSide is required to calculate the axis in many cases.
        // So, if it hasn't been set yet, do not update it yet to avoid problems.
        if (engineSide != null)
            onNorthSouthAxis();
    }

    /**
     * Calculate if this door is aligned with the z-axis (North/South) or the x-axis
     * (East/West).
     *
     * @return True if aligned with the z-axis (North/South), False when aligned
     *         with the x-axis (East/West).
     */
    protected boolean calculateNorthSouthAxis()
    {
        // When it's 1 block deep in the X-axis (East/West), it means it
        // it positioned along the North/South axis.
        return (dimensions.getX()) == 0;
    }

    /**
     * Retrieve if the door is aligned with the z-axis (North/South). If not
     * calculated/invalidated, {@link #calculateNorthSouthAxis()} is called to
     * (re)calculate it.
     *
     * @return True if aligned with the z-axis (North/South), False when aligned
     *         with the x-axis (East/West).
     */
    public boolean onNorthSouthAxis()
    {
        if (northSouthAxis == null)
            northSouthAxis = calculateNorthSouthAxis();
        return northSouthAxis;
    }
}
