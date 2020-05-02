package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypePortcullis;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.VerticalMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a Portcullis doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class Portcullis extends AbstractDoorBase implements IMovingDoorArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypePortcullis.get();

    /**
     * The number of blocks this door will try to move.
     */
    protected int blocksToMove;


    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        return Optional.of(new Portcullis(doorData, (int) args[0]));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof Portcullis))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an Portcullis from type: " + door.getDoorType().toString());

        final @NotNull Portcullis portcullis = (Portcullis) door;
        return new Object[]{portcullis.getBlocksToMove()};
    }

    public Portcullis(final @NotNull DoorData doorData, final int blocksToMove)
    {
        super(doorData);
        this.blocksToMove = blocksToMove;
    }

    @Deprecated
    protected Portcullis(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                         final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    @Deprecated
    protected Portcullis(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.PORTCULLIS);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.UP) ? RotateDirection.DOWN : RotateDirection.UP;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return isOpen() ? PBlockFace.DOWN : PBlockFace.UP;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        return calculateCurrentChunkRange();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(RotateDirection.UP);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.DOWN : RotateDirection.UP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        Vector3Di vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));

        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
                           1 + Math.abs(vec.getY() * dimensions.getY());

        newMin.setX(min.getX());
        newMin.setY(min.getY() + blocksToMove * vec.getY());
        newMin.setZ(min.getZ());

        newMax.setX(max.getX());
        newMax.setY(max.getY() + blocksToMove * vec.getY());
        newMax.setZ(max.getZ());
        return true;

//        Vector3D vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
//
//        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
//                           1 + Math.abs(vec.getX() * dimensions.getX() + vec.getY() * dimensions.getY() +
//                                            vec.getZ() * dimensions.getZ());
//
//        newMin.setX(min.getBlockX() + blocksToMove * vec.getX());
//        newMin.setY(min.getBlockY() + blocksToMove * vec.getY());
//        newMin.setZ(min.getBlockZ() + blocksToMove * vec.getZ());
//
//        newMax.setX(max.getBlockX() + blocksToMove * vec.getX());
//        newMax.setY(max.getBlockY() + blocksToMove * vec.getY());
//        newMax.setZ(max.getBlockZ() + blocksToMove * vec.getZ());
//        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {

        int blocksToMove = newMin.getY() - min.getY();
        doorOpeningUtility.registerBlockMover(
            new VerticalMover(time, this, skipAnimation, blocksToMove, doorOpeningUtility.getMultiplier(this),
                              initiator, newMin, newMax));
    }

    /**
     * Gets the number of blocks this door will try to move.
     *
     * @return The number of blocks this door will try to move.
     */
    public int getBlocksToMove2() // TODO: This isn't #2.
    {
        return blocksToMove;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;
        if (getClass() != o.getClass())
            return false;

        final @NotNull Elevator other = (Elevator) o;
        return blocksToMove == other.blocksToMove;
    }
}
