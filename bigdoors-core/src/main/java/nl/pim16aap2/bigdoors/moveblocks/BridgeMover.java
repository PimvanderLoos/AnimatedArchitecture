package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Drawbridge;
import nl.pim16aap2.bigdoors.doors.EDoorType;
import nl.pim16aap2.bigdoors.doors.IHorizontalAxisAlignedDoorArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotEast;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotNorth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotSouth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotWest;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.IGetNewLocation;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link Drawbridge}s.
 *
 * @author Pim
 */
public class BridgeMover<T extends AbstractDoorBase & IHorizontalAxisAlignedDoorArchetype> extends BlockMover
{
    private final IGetNewLocation gnl;
    protected final boolean NS;
    protected final BiFunction<PBlockData, Double, Vector3Dd> getVector;

    private int halfEndCount;
    private double step;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param door            The {@link AbstractDoorBase}.
     * @param time            The amount of time (in seconds) the door will try to toggle itself in.
     * @param skipAnimation   If the door should be opened instantly (i.e. skip animation) or not.
     * @param upDown          Whether the {@link EDoorType#DRAWBRIDGE} should go up or down.
     * @param rotateDirection The direction the {@link AbstractDoorBase} will move.
     * @param multiplier      The speed multiplier.
     * @param player          The player who opened this door.
     */
    public BridgeMover(final @NotNull T door, final double time, final @NotNull PBlockFace upDown,
                       final @NotNull RotateDirection rotateDirection, final boolean skipAnimation,
                       final double multiplier, final @NotNull IPPlayer player, final @NotNull IVector3DiConst finalMin,
                       final @NotNull IVector3DiConst finalMax, final @NotNull DoorActionCause cause,
                       final @NotNull DoorActionType actionType)
    {
        super(door, time, skipAnimation, rotateDirection, player, finalMin, finalMax, cause, actionType);

        NS = door.isNorthSouthAligned();

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
                gnl = new GNLVerticalRotNorth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getVector = this::getVectorNorth;
                break;
            case EAST:
                gnl = new GNLVerticalRotEast(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getVector = this::getVectorEast;
                break;
            case SOUTH:
                gnl = new GNLVerticalRotSouth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getVector = this::getVectorSouth;
                break;
            case WEST:
                gnl = new GNLVerticalRotWest(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getVector = this::getVectorWest;
                break;
            default:
                gnl = null;
                getVector = null;
                PLogger.get().dumpStackTrace("Failed to open door \"" + getDoorUID()
                                                 + "\". Reason: Invalid rotateDirection \"" +
                                                 rotateDirection.toString() + "\"");
                return; // TODO: This will cause a memory leak, as this object will never be removed from the list keeping track of movers.
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
        halfEndCount = super.endCount / 2;
        step = (Math.PI / 2.0f) / super.endCount;
        super.soundActive = new PSoundDescription(PSound.DRAWBRIDGE_RATTLING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    /**
     * Calculates the speed vector of a block when rotating in northern direction.
     *
     * @param block   The block.
     * @param stepSum The angle (in rads) of the block.
     * @return The speed vector of the block.
     */
    @NotNull
    private Vector3Dd getVectorNorth(final @NotNull PBlockData block, final double stepSum)
    {
        final double startAngle = block.getStartAngle();
        final double posX = block.getFBlock().getPLocation().getX();
        final double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle - stepSum);
        final double posZ = door.getEngine().getZ() - block.getRadius() * Math.sin(startAngle - stepSum);
        return new Vector3Dd(posX, posY, posZ + 0.5);
    }

    /**
     * Calculates the speed vector of a block when rotating in western direction.
     *
     * @param block   The block.
     * @param stepSum The angle (in rads) of the block.
     * @return The speed vector of the block.
     */
    @NotNull
    private Vector3Dd getVectorWest(final @NotNull PBlockData block, final double stepSum)
    {
        final double startAngle = block.getStartAngle();
        final double posX = door.getEngine().getX() - block.getRadius() * Math.sin(startAngle - stepSum);
        final double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle - stepSum);
        final double posZ = block.getFBlock().getPLocation().getZ();
        return new Vector3Dd(posX + 0.5, posY, posZ);
    }

    /**
     * Calculates the speed vector of a block when rotating in southern direction.
     *
     * @param block   The block.
     * @param stepSum The angle (in rads) of the block.
     * @return The speed vector of the block.
     */
    @NotNull
    private Vector3Dd getVectorSouth(final @NotNull PBlockData block, final double stepSum)
    {
        final float startAngle = block.getStartAngle();
        final double posX = block.getFBlock().getPLocation().getX();
        final double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle + stepSum);
        final double posZ = door.getEngine().getZ() - block.getRadius() * Math.sin(startAngle + stepSum);
        return new Vector3Dd(posX, posY, posZ + 0.5);
    }

    /**
     * Calculates the speed vector of a block when rotating in eastern direction.
     *
     * @param block   The block.
     * @param stepSum The angle (in rads) of the block.
     * @return The speed vector of the block.
     */
    @NotNull
    private Vector3Dd getVectorEast(final @NotNull PBlockData block, final double stepSum)
    {
        final float startAngle = block.getStartAngle();
        final double posX = door.getEngine().getX() - block.getRadius() * Math.sin(startAngle + stepSum);
        final double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle + stepSum);
        final double posZ = block.getFBlock().getPLocation().getZ();
        return new Vector3Dd(posX + 0.5, posY, posZ);
    }

    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull IVector3DdConst startLocation = block.getStartPosition();
        final @NotNull IPLocationConst finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                                 startLocation.getY(), startLocation.getZ());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
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
            double radius = block.getRadius();
            if (radius != 0)
            {
                Vector3Dd vec = getVector.apply(block, stepSum)
                                         .subtract(block.getFBlock().getPosition());
                vec.multiply(0.101);
                block.getFBlock().setVelocity(vec);
            }
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

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return gnl.getNewLocation(radius, xAxis, yAxis, zAxis);
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
