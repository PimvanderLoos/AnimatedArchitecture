package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.AbstractHorizontalAxisAlignedBase;
import nl.pim16aap2.bigdoors.doors.Clock;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.WorldTime;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents a {@link BlockMover} for {@link Clock}s.
 *
 * @author Pim
 */
public class ClockMover extends WindmillMover
{
    /**
     * Method to determine if a given {@link PBlockData} is part of the little hand or the big hand of a clock.
     * Represented as a {@link Function} becuase
     */
    @NotNull
    private final Function<PBlockData, Boolean> isHourArm;

    /**
     * The start position of the clock.
     */
//    private static final float STARTPOINT = (float) Math.PI / 2;
    private static final float STARTPOINT = 0;

    /**
     * The step of 1 minute on a clock, or 1/60th of a circle in radians.
     */
    private static final float MINUTESTEP = (float) Math.PI / 30;

    /**
     * The step of 1 hours on a clock, or 1/12th of a circle in radians.
     */
    private static final float HOURSTEP = (float) Math.PI / 6;

    /**
     * The step of 1 minute between two full ours on a clock, or 1/720th of a circle in radians.
     */
    private static final float HOURSUBSTEP = (float) Math.PI / 360;

    public ClockMover(final @NotNull AbstractHorizontalAxisAlignedBase door,
                      final @NotNull RotateDirection rotateDirection, final @NotNull IPPlayer player,
                      final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType)
    {
        super(door, 0.0D, 0.0D, rotateDirection, player, cause, actionType);
        isHourArm = NS ? this::isHourArmNS : this::isHourArmEW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init()
    {
        super.endCount = 40000;
        super.tickRate = 1;
    }

    /**
     * Checks is a given {@link PBlockData} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmNS(final @NotNull PBlockData block)
    {
        // If NS, the clock rotates along the z axis (north south), so the hands are distributed along the x axis.
        // The engine location determines what the front side of the clock is and the little hand is the front side.
        return (int) block.getStartLocation().getX() == door.getEngine().getX();
    }

    /**
     * Checks is a given {@link PBlockData} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmEW(final @NotNull PBlockData block)
    {
        // If NS, the clock rotates along the z axis (north south), so the hands are distributed along the x axis.
        // The engine location determines what the front side of the clock is and the little hand is the front side.
        return (int) block.getStartLocation().getZ() == door.getEngine().getZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        return block.getStartPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeAnimationStep(final int ticks)
    {
        final WorldTime worldTime = world.getTime();
        final double hourAngle = ClockMover.hoursToAngle(worldTime.getHours(), worldTime.getMinutes());
        final double minuteAngle = ClockMover.minutesToAngle(worldTime.getMinutes());

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
                final Vector3Dd vec = getVector.apply(block, timeAngle)
                                               .subtract(block.getFBlock().getPosition());
                vec.multiply(0.03);
                block.getFBlock().setVelocity(vec);
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
        return (float) Util.clampAngleRad(STARTPOINT - minutes * MINUTESTEP);
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
        return (float) Util.clampAngleRad(STARTPOINT - hours * HOURSTEP - minutes * HOURSUBSTEP);
    }
}
