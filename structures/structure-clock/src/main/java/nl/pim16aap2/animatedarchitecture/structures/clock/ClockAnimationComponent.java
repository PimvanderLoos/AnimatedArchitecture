package nl.pim16aap2.animatedarchitecture.structures.clock;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.WorldTime;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.DrawbridgeAnimationComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a {@link Animator} for {@link Clock}s.
 *
 * @author Pim
 */
public final class ClockAnimationComponent extends DrawbridgeAnimationComponent
{
    /**
     * The step of 1 minute on a clock, or 1/60th of a circle in radians.
     */
    private static final double MINUTE_STEP = Math.TAU / 60;

    /**
     * The step of 1 hour on a clock, or 1/12th of a circle in radians.
     */
    private static final double HOUR_STEP = Math.TAU / 12;

    /**
     * The step of 1 on the hour arm between hours x and x+1 on a clock, or 1 / (60 * 12) = 1/720th of a circle in
     * radians.
     */
    private static final double HOUR_SUB_STEP = Math.TAU / 720;

    /**
     * Method to determine if a given {@link IAnimatedBlock} is part of the little hand or the big hand of a clock.
     */
    private final Predicate<IAnimatedBlock> isHourArm;

    /**
     * This value should be either 1 or -1. It is used to change the sign of the angle based on which way the clock
     * should rotate.
     */
    private final int angleDirectionMultiplier;

    private final StructureSnapshot snapshot;

    public ClockAnimationComponent(
        AnimationRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned)
    {
        super(data, movementDirection, isNorthSouthAligned, 4);
        this.snapshot = data.getStructureSnapshot();

        isHourArm = isNorthSouthAligned ? this::isHourArmNorthSouth : this::isHourArmEastWest;
        angleDirectionMultiplier =
            (movementDirection == MovementDirection.EAST || movementDirection == MovementDirection.SOUTH) ? 1 : -1;
    }

    /**
     * Checks is a given {@link IAnimatedBlock} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmNorthSouth(IAnimatedBlock animatedBlock)
    {
        return MathUtil.floor(animatedBlock.getStartX()) == snapshot.getRotationPoint().x();
    }

    /**
     * Checks is a given {@link IAnimatedBlock} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmEastWest(IAnimatedBlock animatedBlock)
    {
        return MathUtil.floor(animatedBlock.getStartZ()) == snapshot.getRotationPoint().z();
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return getStartPosition(xAxis, yAxis, zAxis);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks)
    {
        final WorldTime worldTime = snapshot.getWorld().getTime();
        final double hourAngle = angleDirectionMultiplier * hoursToAngle(worldTime.getHours(), worldTime.getMinutes());
        final double minuteAngle = angleDirectionMultiplier * minutesToAngle(worldTime.getMinutes());

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
        {
            final double timeAngle = isHourArm.test(animatedBlock) ? hourAngle : minuteAngle;
            animator.applyMovement(animatedBlock, getGoalPos(timeAngle, animatedBlock));
        }
    }

    /**
     * Converts a time in minutes (60 per circle) to an angle in radians, with 0 minutes pointing up, and 30 minutes
     * pointing down.
     *
     * @param minutes
     *     The time in minutes since the last full hour.
     * @return The angle.
     */
    private static double minutesToAngle(int minutes)
    {
        return Util.clampAngleRad(minutes * MINUTE_STEP);
    }

    /**
     * Converts a time in hours (12 per circle) to an angle in radians, with 0, 12 hours pointing up, and 6, 18 hours
     * pointing down.
     *
     * @param hours
     *     The time in hours.
     * @param minutes
     *     The time in minutes since the last full hour.
     * @return The angle.
     */
    private static double hoursToAngle(int hours, int minutes)
    {
        return Util.clampAngleRad(hours * HOUR_STEP + minutes * HOUR_SUB_STEP);
    }

    @Override
    public @Nullable Consumer<IAnimatedBlockData> getBlockDataRotator()
    {
        return null;
    }
}
