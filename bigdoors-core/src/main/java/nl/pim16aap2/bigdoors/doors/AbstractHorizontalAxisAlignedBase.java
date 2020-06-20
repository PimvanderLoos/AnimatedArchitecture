package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

/**
 * Represents all {@link EDoorType}s that are aligned on the North/South or East/West axis. For example: {@link
 * SlidingDoor}.
 * <p>
 * Only doors with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public abstract class AbstractHorizontalAxisAlignedBase extends AbstractDoorBase
{
    /**
     * If this door is positioned along the North/South axis or null if it hasn't been calculated yet.
     */
    private boolean northSouthAxis;

    @Deprecated
    protected AbstractHorizontalAxisAlignedBase(final @NotNull PLogger pLogger, final long doorUID,
                                                final @NotNull DoorData doorData, final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected AbstractHorizontalAxisAlignedBase(final @NotNull DoorData doorData)
    {
        super(doorData);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        if (onNorthSouthAxis())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateCoordsDependents()
    {
        super.updateCoordsDependents();
    }

//    /**
//     * Calculate if this {@link AbstractDoorBase} is aligned with the z-axis (North/South) or the x-axis (East/West).
//     *
//     * @return True if aligned with the z-axis (North/South), False when aligned with the x-axis (East/West).
//     */
//    protected boolean calculateNorthSouthAxis()
//    {
//        // When the door is upright and is 1 block deep in the X-axis (East/West), it means the door extends along
//        // the z axis, which means it is also positioned along the North/South axis.
//        if (dimensions.getY() != 0)
//            return (dimensions.getX()) == 0;
//
//        // The engine of the door is aligned with the north/south axis if the engine is
//        // on the east or west side of the door.
//        return getCurrentDirection().equals(PBlockFace.EAST) || getCurrentDirection().equals(PBlockFace.WEST);
//    }

//    /**
//     * Retrieve if the {@link AbstractDoorBase} is aligned with the z-axis (North/South). If not calculated/invalidated,
//     * {@link #calculateNorthSouthAxis()} is called to (re)calculate it.
//     *
//     * @return True if aligned with the z-axis (North/South), False when aligned with the x-axis (East/West).
//     */
//    public final boolean onNorthSouthAxis()
//    {
//        return northSouthAxis == null ? northSouthAxis = calculateNorthSouthAxis() : northSouthAxis;
//    }

    /**
     * Checks if the {@link AbstractDoorBase} is aligned with the z-axis (North/South).
     *
     * @return True if aligned with the z-axis (North/South), False when aligned with the x-axis (East/West).
     */
    public final boolean onNorthSouthAxis()
    {
        return northSouthAxis;
    }
}
