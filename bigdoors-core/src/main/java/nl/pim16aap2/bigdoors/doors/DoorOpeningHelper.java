package nl.pim16aap2.bigdoors.doors;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.IDoorEventCaller;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorToggleEvent;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents a utility singleton that is used to open {@link IDoor}s.
 *
 * @author Pim
 */
@Flogger
public final class DoorOpeningHelper
{
    private final ILocalizer localizer;
    private final DoorActivityManager doorActivityManager;
    private final DoorTypeManager doorTypeManager;
    private final IConfigLoader config;
    private final IBlockAnalyzer blockAnalyzer;
    private final IPLocationFactory locationFactory;
    private final IProtectionCompatManager protectionCompatManager;
    private final GlowingBlockSpawner glowingBlockSpawner;
    private final IBigDoorsEventFactory bigDoorsEventFactory;
    private final IPExecutor executor;
    private final IDoorEventCaller doorEventCaller;

    @Inject //
    DoorOpeningHelper(
        ILocalizer localizer, DoorActivityManager doorActivityManager, DoorTypeManager doorTypeManager,
        IConfigLoader config, IBlockAnalyzer blockAnalyzer, IPLocationFactory locationFactory,
        IProtectionCompatManager protectionCompatManager, GlowingBlockSpawner glowingBlockSpawner,
        IBigDoorsEventFactory bigDoorsEventFactory, IPExecutor executor, IDoorEventCaller doorEventCaller)
    {
        this.localizer = localizer;
        this.doorActivityManager = doorActivityManager;
        this.doorTypeManager = doorTypeManager;
        this.config = config;
        this.blockAnalyzer = blockAnalyzer;
        this.locationFactory = locationFactory;
        this.protectionCompatManager = protectionCompatManager;
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.bigDoorsEventFactory = bigDoorsEventFactory;
        this.executor = executor;
        this.doorEventCaller = doorEventCaller;
    }

    /**
     * Aborts an attempt to toggle a {@link IDoor} and cleans up leftover data from this attempt.
     *
     * @param door
     *     The {@link IDoor}.
     * @param result
     *     The reason the action was aborted.
     * @param cause
     *     What caused the toggle in the first place.
     * @param responsible
     *     Who is responsible for the action.
     * @return The result.
     */
    DoorToggleResult abort(
        IDoor door, DoorToggleResult result, DoorActionCause cause, IPPlayer responsible, IMessageable messageReceiver)
    {
        log.at(Level.FINE).log("Aborted toggle for door %d because of %s. Toggle Reason: %s, Responsible: %s",
                               door.getDoorUID(), result.name(), cause.name(), responsible.asString());

        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this door. However, in every other case it should, because the door is
        // registered as busy before all the other checks take place.
        if (!result.equals(DoorToggleResult.BUSY))
            doorActivityManager.setDoorAvailable(door.getDoorUID());

        if (!result.equals(DoorToggleResult.NO_PERMISSION))
        {
            if (messageReceiver instanceof IPPlayer)
                messageReceiver.sendMessage(localizer.getMessage(result.getLocalizationKey(), door.getName()));
            else
                log.at(Level.INFO).log("Failed to toggle door: %d, reason: %s", door.getDoorUID(), result.name());
        }
        return result;
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createTogglePrepareEvent(AbstractDoor, DoorActionCause, DoorActionType, IPPlayer,
     * double, boolean, Cuboid)}.
     */
    IDoorEventTogglePrepare callTogglePrepareEvent(
        AbstractDoor door, DoorActionCause cause, DoorActionType actionType, IPPlayer responsible, double time,
        boolean skipAnimation, Cuboid newCuboid)
    {
        final IDoorEventTogglePrepare event =
            bigDoorsEventFactory.createTogglePrepareEvent(door, cause, actionType, responsible,
                                                          time, skipAnimation, newCuboid);
        callDoorToggleEvent(event);
        return event;
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createToggleStartEvent(AbstractDoor, DoorActionCause, DoorActionType, IPPlayer,
     * double, boolean, Cuboid)}.
     */
    IDoorEventToggleStart callToggleStartEvent(
        AbstractDoor door, DoorActionCause cause, DoorActionType actionType, IPPlayer responsible, double time,
        boolean skipAnimation, Cuboid newCuboid)
    {
        final IDoorEventToggleStart event =
            bigDoorsEventFactory.createToggleStartEvent(door, cause, actionType, responsible,
                                                        time, skipAnimation, newCuboid);
        callDoorToggleEvent(event);
        return event;
    }

    private void callDoorToggleEvent(IDoorToggleEvent prepareEvent)
    {
        doorEventCaller.callDoorEvent(prepareEvent);
    }

    /**
     * Registers a new block mover. Must be called from the main thread.
     *
     * @throws IllegalStateException
     *     When called from a thread that is not the main thread.
     */
    boolean registerBlockMover(
        DoorBase doorBase, AbstractDoor abstractDoor, DoorActionCause cause, double time, boolean skipAnimation,
        Cuboid newCuboid, IPPlayer responsible, DoorActionType actionType)
    {
        executor.assertMainThread();
        return doorBase.registerBlockMover(abstractDoor, cause, time, skipAnimation,
                                           newCuboid, responsible, actionType);
    }

    /**
     * See {@link IPExecutor#isMainThread()}.
     */
    boolean isMainThread()
    {
        return executor.isMainThread();
    }

    /**
     * Checks if the owner of a door can break blocks between 2 positions.
     * <p>
     * If the player is not allowed to break the block(s), they'll receive a message about this.
     *
     * @param door
     *     The {@link IDoor} being opened.
     * @param cuboid
     *     The area of blocks to check.
     * @param responsible
     *     Who is responsible for the action.
     * @return True if the player is allowed to break the block(s).
     */
    public boolean canBreakBlocksBetweenLocs(IDoor door, Cuboid cuboid, IPPlayer responsible)
    {
        // If the returned value is an empty Optional, the player is allowed to break blocks.
        return protectionCompatManager.canBreakBlocksBetweenLocs(responsible, cuboid.getMin(), cuboid.getMax(),
                                                                 door.getWorld()).map(
            protectionCompat ->
            {
                log.at(Level.WARNING).log("Player '%s' is not allowed to open door '%s' (%d) here! Reason: %s",
                                          responsible, door.getName(), door.getDoorUID(), protectionCompat);
                return false;
            }).orElse(true);
    }

    /**
     * Checks if an area is empty. "Empty" here means that there no blocks that are not air or liquid.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param currentCuboid
     *     The {@link Cuboid} representing the area the door currently takes up. Any parts of the new cuboid overlapping
     *     this cuboid will be ignored.
     * @param player
     *     The {@link IPPlayer} to notify of violations. May be null.
     * @param world
     *     The world to check the blocks in.
     * @return True if the area is not empty.
     */
    public boolean isLocationEmpty(Cuboid newCuboid, Cuboid currentCuboid, @Nullable IPPlayer player, IPWorld world)
    {
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

                    if (!blockAnalyzer.isAirOrLiquid(locationFactory.create(world, xAxis, yAxis, zAxis)))
                    {
                        if (player == null)
                            return false;

                        glowingBlockSpawner
                            .builder().forPlayer(player).withColor(PColor.RED).forDuration(Duration.ofSeconds(4))
                            .atPosition(xAxis + 0.5, yAxis, zAxis + 0.5).inWorld(world).build();
                        isEmpty = false;
                    }
                }
            }
        }
        return isEmpty;
    }

    /**
     * Gets the number of blocks this door can move in the given direction. If set, it won't go further than
     * {@link nl.pim16aap2.bigdoors.doors.doorarchetypes.IDiscreteMovement#getBlocksToMove()}.
     * <p>
     * TODO: This isn't used anywhere? Perhaps either centralize its usage or remove it.
     *
     * @param vec
     *     Which direction to count the number of available blocks in.
     * @param player
     *     The player for whom to check. May be null.
     * @param world
     *     The world to check the blocks in.
     * @param cuboid
     *     The {@link Cuboid} representing the area the door currently takes up.
     * @param blocksToMove
     *     The number of blocks to try move.
     * @return Gets the number of blocks this door can move in the given direction.
     */
    public int getBlocksInDir(Vector3Di vec, @Nullable IPPlayer player, IPWorld world, Cuboid cuboid, int blocksToMove)
    {
        final Vector3Di curMin = cuboid.getMin();
        final Vector3Di curMax = cuboid.getMax();

        final int startY = vec.y() == 0 ? curMin.y() : vec.y() == 1 ? curMax.y() + 1 : curMin.y() - 1;

        // Doors cannot start outside of the world limit.
        if (startY < 0 || startY > 255)
            return 0;

        int startX;
        int startZ;
        int endX;
        int endY;
        int endZ;
        startX = vec.x() == 0 ? curMin.x() : vec.x() == 1 ? curMax.x() + 1 : curMin.x() - 1;
        startZ = vec.z() == 0 ? curMin.z() : vec.z() == 1 ? curMax.z() + 1 : curMin.z() - 1;

        endX = vec.x() == 0 ? curMax.x() : startX;
        endY = vec.y() == 0 ? curMax.y() : startY;
        endZ = vec.z() == 0 ? curMax.z() : startZ;


        Vector3Di locA = new Vector3Di(startX, startY, startZ);
        Vector3Di locB = new Vector3Di(endX, endY, endZ);

        // xLen and zLen describe the length of the door in the x and the z direction respectively.
        // If the rotation direction and the blocksToMove variable are defined, use the blocksToMove variable instead.
        final int xLen = blocksToMove < 1 ? (curMax.x() - curMin.x()) + 1 : blocksToMove;
        int yLen = blocksToMove < 1 ? (curMax.y() - curMin.y()) + 1 : blocksToMove;
        final int zLen = blocksToMove < 1 ? (curMax.z() - curMin.z()) + 1 : blocksToMove;

        yLen = vec.y() == 1 ? Math.min(255, curMax.y() + yLen) :
               vec.y() == -1 ? Math.max(0, curMin.y() - yLen) : yLen;

        // The maxDist is the number of blocks to check in a direction. This is either getBlocksToMove if it that has
        // been specified. If it hasn't, it's the length of the door in the provided direction.
        final int maxDist = blocksToMove > 0 ? blocksToMove :
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
     * Checks if a {@link AbstractDoor} can be toggled or not.
     * <p>
     * It checks the following items:
     * <p>
     * - The {@link AbstractDoor} is not already being animated.
     * <p>
     * - The {@link AbstractDoor} is enabled.
     * <p>
     * - The {@link AbstractDoor} is not locked.
     * <p>
     * - All chunks this {@link AbstractDoor} might interact with are loaded.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param actionType
     *     The type of action.
     * @return {@link DoorToggleResult#SUCCESS} if it can be toggled
     */
    DoorToggleResult canBeToggled(AbstractDoor door, DoorActionType actionType)
    {
        if (!doorActivityManager.attemptRegisterAsBusy(door.getDoorUID()))
            return DoorToggleResult.BUSY;

        if (actionType == DoorActionType.OPEN && !door.isOpenable())
            return DoorToggleResult.ALREADY_OPEN;
        else if (actionType == DoorActionType.CLOSE && !door.isCloseable())
            return DoorToggleResult.ALREADY_CLOSED;

        if (door.isLocked())
            return DoorToggleResult.LOCKED;

        if (!doorTypeManager.isDoorTypeEnabled(door.getDoorType()))
            return DoorToggleResult.TYPE_DISABLED;

        if (!chunksLoaded(door))
        {
            log.at(Level.WARNING).log("Chunks for door '%s' could not be not loaded!", door.getName());
            return DoorToggleResult.ERROR;
        }

        return DoorToggleResult.SUCCESS;
    }

    private boolean chunksLoaded(@SuppressWarnings({"unused", "squid:S1172"}) IDoor door)
    {
        // TODO: Implement this.
        throw new UnsupportedOperationException("NOT IMPLEMENTED!");
    }

    /**
     * Registers a BlockMover with the {@link DatabaseManager}
     *
     * @param blockMover
     *     The {@link BlockMover}.
     */
    public void registerBlockMover(BlockMover blockMover)
    {
        doorActivityManager.addBlockMover(blockMover);
    }

    /**
     * Checks if a {@link BlockMover} of a {@link IDoor} has been registered with the {@link DatabaseManager}.
     *
     * @param doorUID
     *     The UID of the {@link IDoor}.
     * @return True if a {@link BlockMover} has been registered with the {@link DatabaseManager} for the {@link IDoor}.
     */
    @SuppressWarnings("unused")
    public boolean isBlockMoverRegistered(long doorUID)
    {
        return getBlockMover(doorUID).isPresent();
    }

    /**
     * Gets the {@link BlockMover} of a {@link IDoor} if it has been registered with the {@link DatabaseManager}.
     *
     * @param doorUID
     *     The UID of the {@link IDoor}.
     * @return The {@link BlockMover} of a {@link IDoor} if it has been registered with the {@link DatabaseManager}.
     */
    public Optional<BlockMover> getBlockMover(long doorUID)
    {
        return doorActivityManager.getBlockMover(doorUID);
    }

    /**
     * Gets the animation time of a {@link IDoor} from the config based on its {@link DoorType}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @return The animation time of this {@link IDoor}.
     */
    public double getAnimationTime(AbstractDoor door)
    {
        return config.getAnimationTime(door.getDoorType());
    }
}
