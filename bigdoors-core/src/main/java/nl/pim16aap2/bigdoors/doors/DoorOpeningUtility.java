package nl.pim16aap2.bigdoors.doors;

import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents a utility singleton that is used to open {@link AbstractDoorBase}s.
 *
 * @author Pim
 */
@UtilityClass
public final class DoorOpeningUtility
{
    /**
     * Aborts an attempt to toggle a {@link AbstractDoorBase} and cleans up leftover data from this attempt.
     *
     * @param door        The {@link AbstractDoorBase}.
     * @param result      The reason the action was aborted.
     * @param cause       What caused the toggle in the first place.
     * @param responsible Who is responsible for the action.
     * @return The result.
     */
    public @NotNull DoorToggleResult abort(final @NotNull AbstractDoorBase door, final @NotNull DoorToggleResult result,
                                           final @NotNull DoorActionCause cause, final @NotNull IPPlayer responsible)
    {
        BigDoors.get().getPLogger().logMessage(Level.FINE,
                                               String.format("Aborted toggle for door %d because of %s." +
                                                                 " Toggle Reason: %s, Responsible: %s",
                                                             door.getDoorUID(), result.name(), cause.name(),
                                                             responsible.asString()));

        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this door. However, in every other case it should, because the door is
        // registered as busy before all the other checks take place.
        if (!result.equals(DoorToggleResult.BUSY))
            BigDoors.get().getDoorActivityManager().setDoorAvailable(door.getDoorUID());

        if (!result.equals(DoorToggleResult.NOPERMISSION))
            if (!cause.equals(DoorActionCause.PLAYER))
                BigDoors.get().getPLogger()
                        .warn("Failed to toggle door: " + door.getDoorUID() + ", reason: " + result.name());
            else
            {
                BigDoors.get().getMessagingInterface()
                        .messagePlayer(responsible, BigDoors.get().getPlatform().getMessages().getString(
                            DoorToggleResult.getMessage(result), door.getName()));
            }
        return result;
    }

    /**
     * Checks if the owner of a door can break blocks between 2 positions.
     * <p>
     * If the player is not allowed to break the block(s), they'll receive a message about this.
     *
     * @param door        The {@link AbstractDoorBase} being opened.
     * @param cuboid      The area of blocks to check.
     * @param responsible Who is responsible for the action.
     * @return True if the player is allowed to break the block(s).
     */
    public boolean canBreakBlocksBetweenLocs(final @NotNull AbstractDoorBase door, final @NotNull Cuboid cuboid,
                                             final @NotNull IPPlayer responsible)
    {
        // If the returned value is an empty Optional, the player is allowed to break blocks.
        return BigDoors.get().getPlatform().getProtectionCompatManager()
                       .canBreakBlocksBetweenLocs(responsible, cuboid.getMin(), cuboid.getMax(), door.getWorld()).map(
                PROT ->
                {
                    BigDoors.get().getPLogger()
                            .warn("Player \"" + responsible.toString() + "\" is not allowed to open door " +
                                      door.getName() + " (" + door.getDoorUID() + ") here! Reason: " + PROT);
                    return false;
                }).orElse(true);
    }

    /**
     * Checks if an area is empty. "Empty" here means that there no blocks that are not air or liquid.
     *
     * @param newCuboid     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param currentCuboid The {@link Cuboid} representing the area the door currently takes up. Any parts of the new
     *                      cuboid overlapping this cuboid will be ignored.
     * @param player        The {@link IPPlayer} to notify of violations. May be null.
     * @param world         The world to check the blocks in.
     * @return True if the area is not empty.
     */
    public boolean isLocationEmpty(final @NotNull Cuboid newCuboid, final @NotNull Cuboid currentCuboid,
                                   final @Nullable IPPlayer player, final @NotNull IPWorld world)
    {
        final @NotNull IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();
        final Vector3Di newMin = newCuboid.getMin();
        final Vector3Di newMax = newCuboid.getMax();
        final Vector3Di curMin = currentCuboid.getMin();
        final Vector3Di curMax = currentCuboid.getMax();

        boolean isEmpty = true;
        for (int xAxis = newMin.x(); xAxis <= newMax.x(); ++xAxis)
        {
            for (int yAxis = newMin.y(); yAxis <= newMax.y(); ++yAxis)
            {
                for (int zAxis = newMin.z(); zAxis <= newMax.z(); ++zAxis)
                {
                    // Ignore blocks that are currently part of the door.
                    // It's expected and accepted for them to be in the way.
                    if (Util.between(xAxis, curMin.x(), curMax.x()) &&
                        Util.between(yAxis, curMin.y(), curMax.y()) &&
                        Util.between(zAxis, curMin.z(), curMax.z()))
                        continue;

                    if (!BigDoors.get().getPlatform().getBlockAnalyzer()
                                 .isAirOrLiquid(locationFactory.create(world, xAxis, yAxis, zAxis)))
                    {
                        if (player == null)
                            return false;

                        final int posX = xAxis;
                        final int posY = yAxis;
                        final int posZ = zAxis;
                        BigDoors.get().getPlatform().getGlowingBlockSpawner().ifPresent(
                            spawner -> spawner.spawnGlowingBlock(player, world, 10, posX, posY, posZ, PColor.RED));
                        isEmpty = false;
                    }
                }
            }
        }
        return isEmpty;
    }

    /**
     * Gets the number of blocks this door can move in the given direction. If set, it won't go further than {@link
     * IBlocksToMoveArchetype#getBlocksToMove()}.
     * <p>
     * TODO: This isn't used anywhere? Perhaps either centralize its usage or remove it.
     *
     * @param vec          Which direction to count the number of available blocks in.
     * @param player       The player for whom to check. May be null.
     * @param world        The world to check the blocks in.
     * @param cuboid       The {@link Cuboid} representing the area the door currently takes up.
     * @param blocksToMove The number of blocks to try move.
     * @return Gets the number of blocks this door can move in the given direction.
     */
    public int getBlocksInDir(final @NotNull Vector3Di vec, final @Nullable IPPlayer player,
                              final @NotNull IPWorld world, final @NotNull Cuboid cuboid, final int blocksToMove)
    {
        final Vector3Di curMin = cuboid.getMin();
        final Vector3Di curMax = cuboid.getMax();

        final int startY = vec.y() == 0 ? curMin.y() : vec.y() == 1 ? curMax.y() + 1 : curMin.y() - 1;

        // Doors cannot start outside of the world limit.
        if (startY < 0 || startY > 255)
            return 0;

        int startX, startZ, endX, endY, endZ;
        startX = vec.x() == 0 ? curMin.x() : vec.x() == 1 ? curMax.x() + 1 : curMin.x() - 1;
        startZ = vec.z() == 0 ? curMin.z() : vec.z() == 1 ? curMax.z() + 1 : curMin.z() - 1;

        endX = vec.x() == 0 ? curMax.x() : startX;
        endY = vec.y() == 0 ? curMax.y() : startY;
        endZ = vec.z() == 0 ? curMax.z() : startZ;


        @NotNull Vector3Di locA = new Vector3Di(startX, startY, startZ);
        @NotNull Vector3Di locB = new Vector3Di(endX, endY, endZ);

        // xLen and zLen describe the length of the door in the x and the z direction respectively.
        // If the rotation direction and the blocksToMove variable are defined, use the blocksToMove variable instead.
        final int xLen = blocksToMove < 1 ? (curMax.x() - curMin.x()) + 1 : blocksToMove;
        int yLen = blocksToMove < 1 ? (curMax.y() - curMin.y()) + 1 : blocksToMove;
        final int zLen = blocksToMove < 1 ? (curMax.z() - curMin.z()) + 1 : blocksToMove;

        yLen = vec.y() == 1 ? Math.min(255, curMax.y() + yLen) :
               vec.y() == -1 ? Math.max(0, curMin.y() - yLen) : yLen;

        // The maxDist is the number of blocks to check in a direction. This is either getBlocksToMove if it that has
        // been specified. If it hasn't, it's the length of the door in the provided direction.
        int maxDist = blocksToMove > 0 ? blocksToMove :
                      Math.abs(vec.x() * xLen + vec.y() * yLen + vec.z() * zLen);

        int ret = 0;
        int steps = 0;
        boolean obstructed = false;
        while (steps < maxDist)
        {
            final boolean isEmpty = isLocationEmpty(new Cuboid(locA, locB), cuboid, player, world);
            if (!isEmpty)
            {
                obstructed = true;
                if (player == null)
                    break;
            }
            if (!obstructed) // There is no point in checking how many blocks are available behind an obstruction.
                ++ret;
            locA = locA.add(vec.x(), vec.y(), vec.z());
            locB = locB.add(vec.x(), vec.y(), vec.z());
            ++steps;
        }

        // If the direction was in a negative direction, make sure the output is negative as well.
        return (vec.x() == -1 || vec.y() == -1 || vec.z() == -1) ? -1 * ret : ret;
    }

    /**
     * Checks if a {@link AbstractDoorBase} can be toggled or not.
     * <p>
     * It checks the following items:
     * <p>
     * - The {@link AbstractDoorBase} is not already being animated.
     * <p>
     * - The {@link DoorType} is enabled.
     * <p>
     * - The {@link AbstractDoorBase} is not locked.
     * <p>
     * - All chunks this {@link AbstractDoorBase} might interact with are loaded.
     *
     * @param door       The {@link AbstractDoorBase}.
     * @param cause      Who or what initiated this action.
     * @param actionType The type of action.
     * @return {@link DoorToggleResult#SUCCESS} if it can be toggled
     */
    @NotNull DoorToggleResult canBeToggled(final @NotNull AbstractDoorBase door,
                                           final @NotNull DoorActionCause cause,
                                           final @NotNull DoorActionType actionType)
    {
        if (!BigDoors.get().getDoorActivityManager().attemptRegisterAsBusy(door.getDoorUID()))
            return DoorToggleResult.BUSY;

        if (actionType == DoorActionType.OPEN && !door.isOpenable())
            return DoorToggleResult.ALREADYOPEN;
        else if (actionType == DoorActionType.CLOSE && !door.isCloseable())
            return DoorToggleResult.ALREADYCLOSED;

        if (door.isLocked())
            return DoorToggleResult.LOCKED;

        if (!BigDoors.get().getDoorTypeManager().isDoorTypeEnabled(door.getDoorType()))
            return DoorToggleResult.TYPEDISABLED;

        if (!chunksLoaded(door))
        {
            BigDoors.get().getPLogger().warn("Chunks for door " + door.getName() + " could not be not loaded!");
            return DoorToggleResult.ERROR;
        }

        return DoorToggleResult.SUCCESS;
    }

    /**
     * Checks if all chunks in range of the door (see {@link AbstractDoorBase#getChunkRange()}) are loaded.
     * <p>
     * If a chunk is not loaded, an attempt to load it will be made.
     *
     * @param door The door.
     * @return False if 1 or more chunks are not loaded and cannot be loaded.
     */
    private boolean chunksLoaded(final @NotNull AbstractDoorBase door)
    {
        final Vector2Di[] chunkRange = door.getChunkRange();
        for (int x = chunkRange[0].x(); x <= chunkRange[1].x(); ++x)
            for (int y = chunkRange[0].y(); y <= chunkRange[1].y(); ++y)
                if (BigDoors.get().getPlatform().getChunkManager().load(door.getWorld(), new Vector2Di(x, y)) ==
                    IChunkManager.ChunkLoadResult.FAIL)
                    return false;
        return true;
    }

    /**
     * Registers a BlockMover with the {@link DatabaseManager}
     *
     * @param blockMover The {@link BlockMover}.
     */
    public void registerBlockMover(final @NotNull BlockMover blockMover)
    {
        BigDoors.get().getDoorActivityManager().addBlockMover(blockMover);
    }

    /**
     * Checks if a {@link BlockMover} of a {@link AbstractDoorBase} has been registered with the {@link
     * DatabaseManager}.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return True if a {@link BlockMover} has been registered with the {@link DatabaseManager} for the {@link
     * AbstractDoorBase}.
     */
    public boolean isBlockMoverRegistered(final long doorUID)
    {
        return getBlockMover(doorUID).isPresent();
    }

    /**
     * Gets the {@link BlockMover} of a {@link AbstractDoorBase} if it has been registered with the {@link
     * DatabaseManager}.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link BlockMover} of a {@link AbstractDoorBase} if it has been registered with the {@link
     * DatabaseManager}.
     */
    public @NotNull Optional<BlockMover> getBlockMover(final long doorUID)
    {
        return BigDoors.get().getDoorActivityManager().getBlockMover(doorUID);
    }

    /**
     * Gets the speed multiplier of a {@link AbstractDoorBase} from the config based on its {@link DoorType}.
     *
     * @param door The {@link AbstractDoorBase}.
     * @return The speed multiplier of this {@link AbstractDoorBase}.
     */
    public double getMultiplier(final @NotNull AbstractDoorBase door)
    {
        return BigDoors.get().getPlatform().getConfigLoader().getMultiplier(door.getDoorType());
    }
}
