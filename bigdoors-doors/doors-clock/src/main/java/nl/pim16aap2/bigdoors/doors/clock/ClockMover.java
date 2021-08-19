package nl.pim16aap2.bigdoors.doors.clock;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.windmill.WindmillMover;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.WorldTime;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.function.Function;

/**
 * Represents a {@link BlockMover} for {@link Clock}s.
 *
 * @author Pim
 */
public class ClockMover<T extends AbstractDoor & IHorizontalAxisAligned> extends WindmillMover<T>
{
    /**
     * Method to determine if a given {@link PBlockData} is part of the little hand or the big hand of a clock.
     * Represented as a {@link Function} becuase
     */
    protected final Function<PBlockData, Boolean> isHourArm;

    /**
     * The step of 1 minute on a clock, or 1/60th of a circle in radians.
     */
    protected static final float MINUTESTEP = (float) Math.PI / 30;

    /**
     * The step of 1 hours on a clock, or 1/12th of a circle in radians.
     */
    protected static final float HOURSTEP = (float) Math.PI / 6;

    /**
     * The step of 1 minute between two full ours on a clock, or 1/720th of a circle in radians.
     */
    protected static final float HOURSUBSTEP = (float) Math.PI / 360;

    /**
     * This value should be either 1 or -1. It is used to change the sign of the angle based on which way the clock
     * should rotate.
     */
    protected final int angleDirectionMultiplier;

    public ClockMover(final T door, final RotateDirection rotateDirection,
                      final IPPlayer player, final DoorActionCause cause,
                      final DoorActionType actionType)
        throws Exception
    {
        super(door, 0.0D, 0.0D, rotateDirection, player, cause, actionType);
        isHourArm = NS ? this::isHourArmNS : this::isHourArmEW;
        angleDirectionMultiplier =
            (rotateDirection == RotateDirection.EAST || rotateDirection == RotateDirection.SOUTH) ? -1 : 1;
    }

    @Override
    protected void init()
    {
        super.endCount = 40000;
    }

    /**
     * Checks is a given {@link PBlockData} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmNS(final PBlockData block)
    {
        return ((int) block.getStartLocation().getZ()) == door.getEngine().z();
    }

    /**
     * Checks is a given {@link PBlockData} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmEW(final PBlockData block)
    {
        return ((int) block.getStartLocation().getX()) == door.getEngine().x();
    }

    @Override
    protected Vector3Dd getFinalPosition(final PBlockData block)
    {
        return block.getStartPosition();
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        final WorldTime worldTime = world.getTime();
        final double hourAngle = angleDirectionMultiplier * ClockMover.hoursToAngle(worldTime.getHours(),
                                                                                    worldTime.getMinutes());
        final double minuteAngle = angleDirectionMultiplier * ClockMover.minutesToAngle(worldTime.getMinutes());

        // Move the hour arm at a lower tickRate than the minute arm.
        final boolean moveHourArm = ticks % 10 == 0;

        for (final PBlockData block : savedBlocks)
            if (Math.abs(block.getRadius()) > EPS)
            {
                // Move the little hand at a lower interval than the big hand.
                // TODO: Just store the hour and minute arms separately.
                final boolean hourArm = isHourArm.apply(block);
                if (!moveHourArm && hourArm)
                    continue;

                final double timeAngle = hourArm ? hourAngle : minuteAngle;
                block.getFBlock().teleport(getGoalPos(timeAngle, block));
            }
    }

    /**
     * Converts a time in minutes (60 per circle) to an angle in radians, with 0 minutes pointing up, and 30 minutes
     * pointing down.
     *
     * @param minutes The time in minutes since the last full hour.
     * @return The angle.
     */
    private static float minutesToAngle(final int minutes)
    {
        return (float) Util.clampAngleRad(-minutes * MINUTESTEP);
    }

    /**
     * Converts a time in hours (12 per circle) to an angle in radians, with 0, 12 hours pointing up, and 6, 18 hours
     * pointing down.
     *
     * @param hours   The time in hours.
     * @param minutes The time in minutes since the last full hour.
     * @return The angle.
     */
    private static float hoursToAngle(final int hours, final int minutes)
    {
        return (float) Util.clampAngleRad(-hours * HOURSTEP - minutes * HOURSUBSTEP);
    }
}
