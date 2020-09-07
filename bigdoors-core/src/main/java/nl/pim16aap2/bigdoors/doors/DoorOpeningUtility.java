package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a utility singleton that is used to open {@link AbstractDoorBase}s.
 *
 * @author Pim
 */
public final class DoorOpeningUtility
{
    @Nullable
    private static DoorOpeningUtility instance;

    @Getter // Temporary until this is a global thing that can be accessed from the core.
    private final IGlowingBlockSpawner glowingBlockSpawner;
    private final IConfigLoader config;
    private final IProtectionCompatManager protectionManager;

    /**
     * Constructs a new {@link DoorOpeningUtility}.
     *
     * @param pLogger             The logger.
     * @param glowingBlockSpawner The class that
     * @param config              The configuration of the BigDoors plugin.
     * @param protectionManager   The class used to check with compatibility hooks if it is allowed to be toggled.
     */
    private DoorOpeningUtility(final @NotNull PLogger pLogger,
                               final @NotNull IGlowingBlockSpawner glowingBlockSpawner,
                               final @NotNull IConfigLoader config,
                               final @NotNull IProtectionCompatManager protectionManager)
    {
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.config = config;
        this.protectionManager = protectionManager;
    }

    /**
     * Initializes the {@link DoorOpeningUtility}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param pLogger             The logger.
     * @param glowingBlockSpawner The class that
     * @param config              The configuration of the BigDoors plugin.
     * @param protectionManager   The class used to check with compatibility hooks if it is allowed to be toggled.
     * @return The instance of this {@link DoorOpeningUtility}.
     */
    public static @NotNull DoorOpeningUtility init(final @NotNull PLogger pLogger,
                                                   final @NotNull IGlowingBlockSpawner glowingBlockSpawner,
                                                   final @NotNull IConfigLoader config,
                                                   final @NotNull IProtectionCompatManager protectionManager)
    {
        return (instance == null) ?
               instance = new DoorOpeningUtility(pLogger, glowingBlockSpawner, config, protectionManager) :
               instance;
    }

    /**
     * Gets the instance of the {@link DoorOpeningUtility} if it exists.
     *
     * @return The instance of the {@link DoorOpeningUtility}.
     */
    public static @NotNull DoorOpeningUtility get()
    {
//        Preconditions.checkState(instance != null,
//                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

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
        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this door. However, in every other case it should, because the door is
        // registered as busy before all the other checks take place.
        if (!result.equals(DoorToggleResult.BUSY))
            BigDoors.get().getDoorManager().setDoorAvailable(door.getDoorUID());

        if (!result.equals(DoorToggleResult.NOPERMISSION))
            if (!cause.equals(DoorActionCause.PLAYER))
                PLogger.get().warn("Failed to toggle door: " + door.getDoorUID() + ", reason: " + result.name());
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
    public boolean canBreakBlocksBetweenLocs(final @NotNull AbstractDoorBase door, final @NotNull CuboidConst cuboid,
                                             final @NotNull IPPlayer responsible)
    {
        // If the returned value is an empty Optional, the player is allowed to break blocks.
        return protectionManager
            .canBreakBlocksBetweenLocs(responsible, cuboid.getMin(), cuboid.getMax(), door.getWorld()).map(
                PROT ->
                {
                    PLogger.get()
                           .warn("Player \"" + door.getPlayerUUID().toString() + "\" is not allowed to open door " +
                                     door.getName() + " (" + door.getDoorUID() + ") here! Reason: " + PROT);
                    return false;
                }).orElse(true);
    }

    /**
     * Checks if an area is empty. "Empty" here means that there no blocks that are not air or liquid.
     *
     * @param newCuboid     The {@link CuboidConst} representing the area the door will take up after the toggle.
     * @param currentCuboid The {@link CuboidConst} representing the area the door currently takes up.
     * @param player        The {@link IPPlayer} to notify of violations. May be null.
     * @param world         The world to check the blocks in.
     * @return True if the area is not empty.
     */
    public boolean isLocationEmpty(final @NotNull CuboidConst newCuboid, final @NotNull CuboidConst currentCuboid,
                                   final @Nullable IPPlayer player, final @NotNull IPWorld world)
    {
        final @NotNull IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();
        final Vector3DiConst newMin = newCuboid.getMin();
        final Vector3DiConst newMax = newCuboid.getMax();
        final Vector3DiConst curMin = currentCuboid.getMin();
        final Vector3DiConst curMax = currentCuboid.getMax();

        boolean isEmpty = true;
        for (int xAxis = newMin.getX(); xAxis <= newMax.getX(); ++xAxis)
        {
            for (int yAxis = newMin.getY(); yAxis <= newMax.getY(); ++yAxis)
            {
                for (int zAxis = newMin.getZ(); zAxis <= newMax.getZ(); ++zAxis)
                {
                    // Ignore blocks that are currently part of the door.
                    // It's expected and accepted for them to be in the way.
                    if (Util.between(xAxis, curMin.getX(), curMax.getX()) &&
                        Util.between(yAxis, curMin.getY(), curMax.getY()) &&
                        Util.between(zAxis, curMin.getZ(), curMax.getZ()))
                        continue;

                    if (!BigDoors.get().getPlatform().getBlockAnalyzer()
                                 .isAirOrLiquid(locationFactory.create(world, xAxis, yAxis, zAxis)))
                    {
                        if (player == null)
                            return false;

                        glowingBlockSpawner
                            .spawnGlowingBlock(player, world, 10, xAxis, yAxis, zAxis, PColor.RED);
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
     * @param cuboid       The {@link CuboidConst} representing the area the door currently takes up.
     * @param blocksToMove The number of blocks to try move.
     * @return Gets the number of blocks this door can move in the given direction.
     */
    public int getBlocksInDir(final @NotNull Vector3DiConst vec, final @Nullable IPPlayer player,
                              final @NotNull IPWorld world, final @NotNull CuboidConst cuboid, final int blocksToMove)
    {
        final Vector3DiConst curMin = cuboid.getMin();
        final Vector3DiConst curMax = cuboid.getMax();

        final int startY = vec.getY() == 0 ? curMin.getY() : vec.getY() == 1 ? curMax.getY() + 1 : curMin.getY() - 1;

        // Doors cannot start outside of the world limit.
        if (startY < 0 || startY > 255)
            return 0;

        int startX, startZ, endX, endY, endZ;
        startX = vec.getX() == 0 ? curMin.getX() : vec.getX() == 1 ? curMax.getX() + 1 : curMin.getX() - 1;
        startZ = vec.getZ() == 0 ? curMin.getZ() : vec.getZ() == 1 ? curMax.getZ() + 1 : curMin.getZ() - 1;

        endX = vec.getX() == 0 ? curMax.getX() : startX;
        endY = vec.getY() == 0 ? curMax.getY() : startY;
        endZ = vec.getZ() == 0 ? curMax.getZ() : startZ;


        final @NotNull Vector3Di locA = new Vector3Di(startX, startY, startZ);
        final @NotNull Vector3Di locB = new Vector3Di(endX, endY, endZ);

        // xLen and zLen describe the length of the door in the x and the z direction respectively.
        // If the rotation direction and the blocksToMove variable are defined, use the blocksToMove variable instead.
        final int xLen = blocksToMove < 1 ? (curMax.getX() - curMin.getX()) + 1 : blocksToMove;
        int yLen = blocksToMove < 1 ? (curMax.getY() - curMin.getY()) + 1 : blocksToMove;
        final int zLen = blocksToMove < 1 ? (curMax.getZ() - curMin.getZ()) + 1 : blocksToMove;

        yLen = vec.getY() == 1 ? Math.min(255, curMax.getY() + yLen) :
               vec.getY() == -1 ? Math.max(0, curMin.getY() - yLen) : yLen;

        // The maxDist is the number of blocks to check in a direction. This is either getBlocksToMove if it that has
        // been specified. If it hasn't, it's the length of the door in the provided direction.
        int maxDist = blocksToMove > 0 ? blocksToMove :
                      Math.abs(vec.getX() * xLen + vec.getY() * yLen + vec.getZ() * zLen);

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
            locA.add(vec.getX(), vec.getY(), vec.getZ());
            locB.add(vec.getX(), vec.getY(), vec.getZ());
            ++steps;
        }

        // If the direction was in a negative direction, make sure the output is negative as well.
        return (vec.getX() == -1 || vec.getY() == -1 || vec.getZ() == -1) ? -1 * ret : ret;
    }

    /**
     * Checks if a {@link AbstractDoorBase} is busy and set it to busy if that is the case.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase} to check.
     * @return True if already busy.
     */
    private boolean isBusySetIfNot(final long doorUID)
    {
        if (BigDoors.get().getDoorManager().isDoorBusy(doorUID))
            return true;
        BigDoors.get().getDoorManager().setDoorBusy(doorUID);
        return false;
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
    public @NotNull DoorToggleResult canBeToggled(final @NotNull AbstractDoorBase door,
                                                  final @NotNull DoorActionCause cause,
                                                  final @NotNull DoorActionType actionType)
    {
        if (isBusySetIfNot(door.getDoorUID()))
            return DoorToggleResult.BUSY;

        if (actionType == DoorActionType.OPEN && !door.isOpenable())
            return DoorToggleResult.ALREADYOPEN;
        else if (actionType == DoorActionType.CLOSE && !door.isCloseable())
            return DoorToggleResult.ALREADYCLOSED;

        if (door.isLocked())
            return DoorToggleResult.LOCKED;
        if (!DoorTypeManager.get().isDoorTypeEnabled(door.getDoorType()))
            return DoorToggleResult.TYPEDISABLED;

        if (!chunksLoaded(door))
        {
            PLogger.get().warn("Chunks for door " + door.getName() + " could not be not loaded!");
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
        for (int x = chunkRange[0].getX(); x <= chunkRange[1].getX(); ++x)
            for (int y = chunkRange[0].getY(); y <= chunkRange[1].getY(); ++y)
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
        BigDoors.get().getDoorManager().addBlockMover(blockMover);
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
        return BigDoors.get().getDoorManager().getBlockMover(doorUID);
    }

    /**
     * Gets the speed multiplier of a {@link AbstractDoorBase} from the config based on its {@link DoorType}.
     *
     * @param door The {@link AbstractDoorBase}.
     * @return The speed multiplier of this {@link AbstractDoorBase}.
     */
    public double getMultiplier(final @NotNull AbstractDoorBase door)
    {
        return config.getMultiplier(door.getDoorType());
    }
}
