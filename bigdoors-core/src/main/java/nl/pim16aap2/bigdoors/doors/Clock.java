package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMoverArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.ClockMover;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Clock doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class Clock extends HorizontalAxisAlignedBase implements IStationaryDoorArchetype, IPerpetualMoverArchetype
{
    protected Clock(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                    final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected Clock(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.CLOCK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean calculateNorthSouthAxis()
    {
        // A clock can be 2 blocks deep in only the X or the Z direction.
        return dimensions.getX() == 1;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            setOpenDir(engine.getX() == min.getX() ? RotateDirection.SOUTH : RotateDirection.NORTH);
        else
            setOpenDir(engine.getZ() == min.getZ() ? RotateDirection.EAST : RotateDirection.WEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {
        doorOpeningUtility.registerBlockMover(new ClockMover(this, getCurrentToggleDir(), initiator));
    }
}
