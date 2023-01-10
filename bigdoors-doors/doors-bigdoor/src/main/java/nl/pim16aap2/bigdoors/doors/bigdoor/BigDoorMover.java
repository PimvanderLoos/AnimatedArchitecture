package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.logging.Level;

@Flogger
public class BigDoorMover extends BlockMover
{
    private final Vector3Dd rotationCenter;
    private final int halfEndCount;
    private final double angle;
    private final double step;

    public BigDoorMover(
        Context context, AbstractDoor door, RotateDirection rotDirection, double time,
        boolean skipAnimation, double multiplier, IPPlayer player, Cuboid newCuboid,
        DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        super(context, door, time, skipAnimation, rotDirection, player, newCuboid, cause, actionType);

        angle = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                rotDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;

        if (angle == 0.0D)
            log.at(Level.SEVERE).log("Invalid open direction '%s' for door: %d", rotDirection.name(), getDoorUID());

        rotationCenter = new Vector3Dd(door.getRotationPoint().x() + 0.5, yMin, door.getRotationPoint().z() + 0.5);
        super.time = 3;

        super.animationDuration = (int) (20 * super.time) + 1;
        step = angle / super.animationDuration;
        halfEndCount = super.animationDuration / 2;
    }

    @Override
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return getGoalPos(angle, startLocation.xD(), startLocation.yD(), startLocation.zD());
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        if (ticks == halfEndCount)
            applyRotation();

        final double stepSum = step * ticks;
        final double cos = Math.cos(stepSum);
        final double sin = Math.sin(stepSum);

        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getGoalPos(animatedBlock, cos, sin), ticksRemaining);
    }

    private Vector3Dd getGoalPos(double cos, double sin, double startX, double startY, double startZ)
    {
        final double translatedX = startX - rotationCenter.x();
        final double translatedZ = startZ - rotationCenter.z();

        final double changeX = translatedX * cos - translatedZ * sin;
        final double changeZ = translatedX * sin + translatedZ * cos;

        return new Vector3Dd(rotationCenter.x() + changeX, startY, rotationCenter.z() + changeZ);
    }

    private Vector3Dd getGoalPos(double angle, double startX, double startY, double startZ)
    {
        return getGoalPos(Math.cos(angle), Math.sin(angle), startX, startY, startZ);
    }

    private Vector3Dd getGoalPos(IAnimatedBlock animatedBlock, double cos, double sin)
    {
        return getGoalPos(cos, sin, animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ());
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        final double deltaA = (double) door.getRotationPoint().x() - xAxis;
        final double deltaB = (double) door.getRotationPoint().z() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return (float) Math.atan2((double) door.getRotationPoint().x() - xAxis,
                                  (double) door.getRotationPoint().z() - zAxis);
    }
}
