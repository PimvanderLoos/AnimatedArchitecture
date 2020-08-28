package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Drawbridge;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAlignedDoorArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.Functional.TriFunction;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@link BlockMover} for {@link Drawbridge}s.
 *
 * @author Pim
 */
public class BridgeMover<T extends AbstractDoorBase & IHorizontalAxisAlignedDoorArchetype> extends BlockMover
{
    private final Vector3DdConst rotationCenter;
    protected final boolean NS;
    @NotNull
    protected final TriFunction<Vector3Dd, Vector3DdConst, Double, Vector3Dd> rotator;

    private int halfEndCount;
    private double step;
    protected final double angle;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param door            The {@link AbstractDoorBase}.
     * @param time            The amount of time (in seconds) the door will try to toggle itself in.
     * @param skipAnimation   If the door should be opened instantly (i.e. skip animation) or not.
     * @param rotateDirection The direction the {@link AbstractDoorBase} will move.
     * @param multiplier      The speed multiplier.
     * @param player          The player who opened this door.
     */
    public BridgeMover(final double time, final @NotNull T door,
                       final @NotNull RotateDirection rotateDirection, final boolean skipAnimation,
                       final double multiplier, final @NotNull IPPlayer player, final @NotNull Vector3DiConst finalMin,
                       final @NotNull Vector3DiConst finalMax, final @NotNull DoorActionCause cause,
                       final @NotNull DoorActionType actionType)
    {
        super(door, time, skipAnimation, rotateDirection, player, finalMin, finalMax, cause, actionType);

        NS = door.isNorthSouthAligned();
        rotationCenter = new Vector3Dd(door.getEngine()).add(0.5, 0, 0.5);

        final int xLen = Math.abs(door.getMaximum().getX() - door.getMinimum().getX());
        final int yLen = Math.abs(door.getMaximum().getY() - door.getMinimum().getY());
        final int zLen = Math.abs(door.getMaximum().getZ() - door.getMinimum().getZ());
        final int doorSize = Math.max(xLen, Math.max(yLen, zLen)) + 1;
        final double[] vars = Util.calculateTimeAndTickRate(doorSize, time, multiplier, 5.2);
        this.time = vars[0];
        tickRate = (int) vars[1];

        switch (rotateDirection)
        {
            case NORTH:
                angle = -Math.PI / 2;
                rotator = Vector3Dd::rotateAroundXAxis;
                break;
            case SOUTH:
                angle = Math.PI / 2;
                rotator = Vector3Dd::rotateAroundXAxis;
                break;
            case EAST:
                angle = Math.PI / 2;
                rotator = Vector3Dd::rotateAroundZAxis;
                break;
            case WEST:
                angle = -Math.PI / 2;
                rotator = Vector3Dd::rotateAroundZAxis;
                break;
            default:
                angle = 0;
                rotator = null;
                PLogger.get().logThrowable(new IllegalArgumentException(
                    "RotateDirection \"" + rotateDirection.name() + " is not valid for this type!"));
                return;
        }

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20 * super.time);
        step = angle / super.endCount;
        halfEndCount = super.endCount / 2;
        super.soundActive = new PSoundDescription(PSound.DRAWBRIDGE_RATTLING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    @NotNull
    protected Vector3Dd getGoalPos(final double angle, final double x, final double y, final double z)
    {
        return rotator.apply(new Vector3Dd(x, y, z), rotationCenter, angle);
    }

    @NotNull
    protected Vector3Dd getGoalPos(final double angle, final @NotNull PBlockData pBlockData)
    {
        return getGoalPos(angle, pBlockData.getStartX(), pBlockData.getStartY(), pBlockData.getStartZ());
    }

    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        return getGoalPos(angle, block);
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        final double stepSum = step * ticks;
        final boolean replace = ticks == halfEndCount;

        // It is not possible to edit falling block blockdata (client won't update it),
        // so delete the current fBlock and replace it by one that's been rotated.
        // Also, this stuff needs to be done on the main thread.
        if (replace)
            BigDoors.get().getPlatform().newPExecutor().runSync(this::respawnBlocks);

        for (final PBlockData block : savedBlocks)
        {
            final @NotNull Vector3Dd vec = getGoalPos(stepSum, block).subtract(block.getFBlock().getPosition())
                                                                     .multiply(0.101);
            block.getFBlock().setVelocity(vec);
        }
    }

    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the Z values does not change.
        final double deltaA = (door.getEngine().getY() - yAxis);
        final double deltaB = NS ? (door.getEngine().getX() - xAxis) : (door.getEngine().getZ() - zAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    @NotNull
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return BigDoors.get().getPlatform().getPLocationFactory().create(world, getGoalPos(angle, xAxis, yAxis, zAxis));
    }

    @Override
    protected float getStartAngle(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the angle between the used axes (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the Z values does not change.
        final float deltaA = NS ? door.getEngine().getX() - xAxis : door.getEngine().getZ() - zAxis;
        final float deltaB = door.getEngine().getY() - yAxis;
        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
