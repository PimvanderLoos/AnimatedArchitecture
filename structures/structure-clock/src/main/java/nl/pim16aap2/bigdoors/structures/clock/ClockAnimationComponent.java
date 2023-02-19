package nl.pim16aap2.bigdoors.structures.clock;

import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.core.moveblocks.Animator;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.WorldTime;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.structures.windmill.WindmillAnimationComponent;

import java.util.function.Predicate;

/**
 * Represents a {@link Animator} for {@link Clock}s.
 *
 * @author Pim
 */
public final class ClockAnimationComponent extends WindmillAnimationComponent
{
    /**
     * The step of 1 minute on a clock, or 1/60th of a circle in radians.
     */
    private static final float MINUTE_STEP = (float) Math.PI / 30;

    /**
     * The step of 1 hour on a clock, or 1/12th of a circle in radians.
     */
    private static final float HOUR_STEP = (float) Math.PI / 6;

    /**
     * The step of 1 minute between two full ours on a clock, or 1/720th of a circle in radians.
     */
    private static final float HOUR_SUB_STEP = (float) Math.PI / 360;

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
        StructureRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned)
    {
        super(data, movementDirection, isNorthSouthAligned);
        this.snapshot = data.getStructureSnapshot();

        isHourArm = isNorthSouthAligned ? this::isHourArmNS : this::isHourArmEW;
        angleDirectionMultiplier =
            (movementDirection == MovementDirection.EAST || movementDirection == MovementDirection.SOUTH) ? -1 : 1;
    }

    @Override
    public Animator.MovementMethod getMovementMethod()
    {
        return Animator.MovementMethod.TELEPORT;
    }

    /**
     * Checks is a given {@link IAnimatedBlock} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmNS(IAnimatedBlock animatedBlock)
    {
        return MathUtil.floor(animatedBlock.getPosition().z()) == snapshot.getRotationPoint().z();
    }

    /**
     * Checks is a given {@link IAnimatedBlock} is the hour arm or the minute arm.
     *
     * @return True if the block is part of the hour arm.
     */
    private boolean isHourArmEW(IAnimatedBlock animatedBlock)
    {
        return MathUtil.floor(animatedBlock.getPosition().x()) == snapshot.getRotationPoint().x();
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final WorldTime worldTime = snapshot.getWorld().getTime();

        final double hourAngle = angleDirectionMultiplier *
            ClockAnimationComponent.hoursToAngle(worldTime.getHours(), worldTime.getMinutes());

        final double minuteAngle =
            angleDirectionMultiplier * ClockAnimationComponent.minutesToAngle(worldTime.getMinutes());

        // Move the hour arm at a lower tickRate than the minute arm.
        final boolean moveHourArm = ticks % 10 == 0;

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            if (Math.abs(animatedBlock.getRadius()) > MathUtil.EPS)
            {
                // Move the little hand at a lower interval than the big hand.
                final boolean hourArm = isHourArm.test(animatedBlock);
                if (!moveHourArm && hourArm)
                    continue;

                final double timeAngle = hourArm ? hourAngle : minuteAngle;
                animator.applyMovement(animatedBlock, getGoalPos(timeAngle, animatedBlock), ticksRemaining);
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
    private static float minutesToAngle(int minutes)
    {
        return (float) Util.clampAngleRad(-minutes * MINUTE_STEP);
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
    private static float hoursToAngle(int hours, int minutes)
    {
        return (float) Util.clampAngleRad(-hours * HOUR_STEP - minutes * HOUR_SUB_STEP);
    }
}
