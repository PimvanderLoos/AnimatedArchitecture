package nl.pim16aap2.bigdoors.movable;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkLoader;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableEventToggleStart;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableToggleEvent;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents a utility singleton that is used to open {@link IMovable}s.
 *
 * @author Pim
 */
@Flogger
public final class MovableOpeningHelper
{
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final MovableActivityManager movableActivityManager;
    private final MovableTypeManager movableTypeManager;
    private final IConfigLoader config;
    private final IPExecutor executor;
    private final IBlockAnalyzer blockAnalyzer;
    private final IPLocationFactory locationFactory;
    private final IProtectionCompatManager protectionCompatManager;
    private final GlowingBlockSpawner glowingBlockSpawner;
    private final IBigDoorsEventFactory bigDoorsEventFactory;
    private final MovableRegistry movableRegistry;
    private final IChunkLoader chunkLoader;
    private final IBigDoorsEventCaller bigDoorsEventCaller;

    @Inject //
    MovableOpeningHelper(
        ILocalizer localizer,
        ITextFactory textFactory,
        MovableActivityManager movableActivityManager,
        MovableTypeManager movableTypeManager,
        IConfigLoader config,
        IPExecutor executor,
        IBlockAnalyzer blockAnalyzer,
        IPLocationFactory locationFactory,
        IProtectionCompatManager protectionCompatManager,
        GlowingBlockSpawner glowingBlockSpawner,
        IBigDoorsEventFactory bigDoorsEventFactory,
        MovableRegistry movableRegistry,
        IChunkLoader chunkLoader,
        IBigDoorsEventCaller bigDoorsEventCaller)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.movableActivityManager = movableActivityManager;
        this.movableTypeManager = movableTypeManager;
        this.config = config;
        this.executor = executor;
        this.blockAnalyzer = blockAnalyzer;
        this.locationFactory = locationFactory;
        this.protectionCompatManager = protectionCompatManager;
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.bigDoorsEventFactory = bigDoorsEventFactory;
        this.movableRegistry = movableRegistry;
        this.chunkLoader = chunkLoader;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
    }

    /**
     * Aborts an attempt to toggle a {@link IMovable} and cleans up leftover data from this attempt.
     *
     * @param movable
     *     The {@link IMovable}.
     * @param result
     *     The reason the action was aborted.
     * @param cause
     *     What caused the toggle in the first place.
     * @param responsible
     *     Who is responsible for the action.
     * @return The result.
     */
    MovableToggleResult abort(
        AbstractMovable movable, MovableToggleResult result, MovableActionCause cause, IPPlayer responsible,
        IMessageable messageReceiver)
    {
        log.atFine().log("Aborted toggle for movable %d because of %s. Toggle Reason: %s, Responsible: %s",
                         movable.getUid(), result.name(), cause.name(), responsible.asString());

        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this movable. However, in every other case it should, because the movable is
        // registered as busy before all the other checks take place.
        if (!result.equals(MovableToggleResult.BUSY))
            movableActivityManager.setMovableAvailable(movable.getUid());

        if (!result.equals(MovableToggleResult.NO_PERMISSION))
        {
            if (messageReceiver instanceof IPPlayer)
                messageReceiver.sendError(
                    textFactory,
                    localizer.getMessage(result.getLocalizationKey(),
                                         localizer.getMovableType(movable.getType()),
                                         movable.getName()));
            else
            {
                final Level level = result == MovableToggleResult.BUSY ? Level.FINE : Level.INFO;
                log.at(level).log("Failed to toggle movable: %d, reason: %s", movable.getUid(), result.name());
            }
        }
        return result;
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createTogglePrepareEvent(MovableSnapshot, MovableActionCause, MovableActionType,
     * IPPlayer, double, boolean, Cuboid)}.
     */
    IMovableEventTogglePrepare callTogglePrepareEvent(
        MovableSnapshot snapshot, MovableActionCause cause, MovableActionType actionType, IPPlayer responsible,
        double time,
        boolean skipAnimation, Cuboid newCuboid)
    {
        final IMovableEventTogglePrepare event =
            bigDoorsEventFactory.createTogglePrepareEvent(
                snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
        callMovableToggleEvent(event);
        return event;
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createToggleStartEvent(AbstractMovable, MovableSnapshot, MovableActionCause,
     * MovableActionType, IPPlayer, double, boolean, Cuboid)}.
     */
    IMovableEventToggleStart callToggleStartEvent(
        AbstractMovable movable, MovableSnapshot snapshot, MovableActionCause cause, MovableActionType actionType,
        IPPlayer responsible, double time, boolean skipAnimation, Cuboid newCuboid)
    {
        final IMovableEventToggleStart event =
            bigDoorsEventFactory.createToggleStartEvent(
                movable, snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
        callMovableToggleEvent(event);
        return event;
    }

    private void callMovableToggleEvent(IMovableToggleEvent prepareEvent)
    {
        bigDoorsEventCaller.callBigDoorsEvent(prepareEvent);
    }

    /**
     * Registers a new block mover. Must be called from the main thread.
     */
    boolean registerBlockMover(
        AbstractMovable movable, MovableSnapshot snapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible, MovableActionType actionType)
    {
        return movable.base.registerBlockMover(
            movable, snapshot, cause, time, skipAnimation, newCuboid, responsible, actionType);
    }

    private MovableToggleResult toggle(
        MovableSnapshot snapshot, AbstractMovable targetMovable, MovableActionCause cause, IMessageable messageReceiver,
        IPPlayer responsible, boolean skipAnimation, MovableActionType actionType,
        boolean canSkipAnimation, Cuboid newCuboid, double animationTime)
    {
        if (snapshot.getOpenDir() == RotateDirection.NONE)
        {
            log.atSevere().withCause(new IllegalStateException("OpenDir cannot be NONE!")).log();
            return MovableToggleResult.ERROR;
        }

        if (!movableRegistry.isRegistered(targetMovable))
            return abort(targetMovable, MovableToggleResult.INSTANCE_UNREGISTERED, cause, responsible,
                         messageReceiver);

        if (skipAnimation && !canSkipAnimation)
            return abort(targetMovable, MovableToggleResult.ERROR, cause, responsible, messageReceiver);

        final MovableToggleResult isOpenable = canBeToggled(snapshot, targetMovable.getType(), newCuboid,
                                                            actionType);
        if (isOpenable != MovableToggleResult.SUCCESS)
            return abort(targetMovable, isOpenable, cause, responsible, messageReceiver);

        final IMovableEventTogglePrepare prepareEvent =
            callTogglePrepareEvent(
                snapshot, cause, actionType, responsible, animationTime, skipAnimation, newCuboid);

        if (prepareEvent.isCancelled())
            return abort(targetMovable, MovableToggleResult.CANCELLED, cause, responsible, messageReceiver);

        final @Nullable IPPlayer responsiblePlayer = cause.equals(MovableActionCause.PLAYER) ? responsible : null;
        if (!isLocationEmpty(newCuboid, snapshot.getCuboid(), responsiblePlayer, snapshot.getWorld()))
            return abort(targetMovable, MovableToggleResult.OBSTRUCTED, cause, responsible, messageReceiver);

        if (!canBreakBlocksBetweenLocs(snapshot, newCuboid, responsible))
            return abort(targetMovable, MovableToggleResult.NO_PERMISSION, cause, responsible, messageReceiver);

        final boolean scheduled = registerBlockMover(
            targetMovable, snapshot, cause, animationTime, skipAnimation, newCuboid, responsible, actionType);

        if (!scheduled)
            return MovableToggleResult.ERROR;

        executor.runAsync(() -> callToggleStartEvent(
            targetMovable, snapshot, cause, actionType, responsible, animationTime, skipAnimation, newCuboid));

        return MovableToggleResult.SUCCESS;
    }

    MovableToggleResult toggle(
        AbstractMovable movable, MovableActionCause cause, IMessageable messageReceiver, IPPlayer responsible,
        @Nullable Double targetTime, boolean skipAnimation, MovableActionType actionType)
    {
        final Optional<Cuboid> newCuboid;
        final double animationTime;
        final MovableSnapshot snapshot;
        final boolean canSkipAnimation;

        movable.getLock().readLock().lock();
        try
        {
            if (movable.base.exceedSizeLimit(responsible))
                return abort(movable, MovableToggleResult.TOO_BIG, cause, responsible, messageReceiver);

            newCuboid = movable.getPotentialNewCoordinates();
            if (newCuboid.isEmpty())
                return abort(movable, MovableToggleResult.ERROR, cause, responsible, messageReceiver);

            animationTime = movable.getAnimationTime(targetTime);
            canSkipAnimation = movable.canSkipAnimation();
            snapshot = movable.getSnapshot();
        }
        finally
        {
            movable.getLock().readLock().unlock();
        }
        return toggle(
            snapshot, movable, cause, messageReceiver, responsible, skipAnimation, actionType, canSkipAnimation,
            newCuboid.get(), animationTime);
    }

    /**
     * Checks if the owner of a movable can break blocks between 2 positions.
     * <p>
     * If the player is not allowed to break the block(s), they'll receive a message about this.
     *
     * @param movable
     *     The {@link IMovable} being opened.
     * @param cuboid
     *     The area of blocks to check.
     * @param responsible
     *     Who is responsible for the action.
     * @return True if the player is allowed to break the block(s).
     */
    public boolean canBreakBlocksBetweenLocs(IMovableConst movable, Cuboid cuboid, IPPlayer responsible)
    {
        if (protectionCompatManager.canSkipCheck())
            return true;
        try
        {
            return executor.runOnMainThread(() -> canBreakBlocksBetweenLocs0(movable, cuboid, responsible))
                           .get(500, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e)
               .log("Failed to check if blocks can be broken in cuboid %s for user: '%s' for movable %s",
                    cuboid, responsible, movable);
            return false;
        }
    }

    private boolean canBreakBlocksBetweenLocs0(IMovableConst movable, Cuboid cuboid, IPPlayer responsible)
    {
        // If the returned value is an empty Optional, the player is allowed to break blocks.
        return protectionCompatManager.canBreakBlocksBetweenLocs(responsible, cuboid.getMin(), cuboid.getMax(),
                                                                 movable.getWorld()).map(
            protectionCompat ->
            {
                log.atWarning().log("Player '%s' is not allowed to open movable '%s' (%d) here! Reason: %s",
                                    responsible, movable.getName(), movable.getUid(), protectionCompat);
                return false;
            }).orElse(true);
    }

    /**
     * Checks if an area is empty. "Empty" here means that there no blocks that are not air or liquid.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the movable will take up after the toggle.
     * @param currentCuboid
     *     The {@link Cuboid} representing the area the movable currently takes up. Any parts of the new cuboid
     *     overlapping this cuboid will be ignored.
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
                    // Ignore blocks that are currently part of the movable.
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
     * Gets the number of blocks this movable can move in the given direction. If set, it won't go further than
     * {@link nl.pim16aap2.bigdoors.movable.movablearchetypes.IDiscreteMovement#getBlocksToMove()}.
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
     *     The {@link Cuboid} representing the area the movable currently takes up.
     * @param blocksToMove
     *     The number of blocks to try move.
     * @return Gets the number of blocks this movable can move in the given direction.
     */
    public int getBlocksInDir(Vector3Di vec, @Nullable IPPlayer player, IPWorld world, Cuboid cuboid, int blocksToMove)
    {
        final Vector3Di curMin = cuboid.getMin();
        final Vector3Di curMax = cuboid.getMax();

        final int startY = vec.y() == 0 ? curMin.y() : vec.y() == 1 ? curMax.y() + 1 : curMin.y() - 1;

        // Movables cannot start outside the world limit.
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

        // xLen and zLen describe the length of the movable in the x and the z direction respectively.
        // If the rotation direction and the blocksToMove variable are defined, use the blocksToMove variable instead.
        final int xLen = blocksToMove < 1 ? (curMax.x() - curMin.x()) + 1 : blocksToMove;
        int yLen = blocksToMove < 1 ? (curMax.y() - curMin.y()) + 1 : blocksToMove;
        final int zLen = blocksToMove < 1 ? (curMax.z() - curMin.z()) + 1 : blocksToMove;

        yLen = vec.y() == 1 ? Math.min(255, curMax.y() + yLen) :
               vec.y() == -1 ? Math.max(0, curMin.y() - yLen) : yLen;

        // The maxDist is the number of blocks to check in a direction. This is either getBlocksToMove if it that has
        // been specified. If it hasn't, it's the length of the movable in the provided direction.
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
     * Checks if a {@link AbstractMovable} can be toggled or not.
     * <p>
     * It checks the following items:
     * <p>
     * - The {@link AbstractMovable} is not already being animated.
     * <p>
     * - The {@link AbstractMovable} is enabled.
     * <p>
     * - The {@link AbstractMovable} is not locked.
     * <p>
     * - All chunks this {@link AbstractMovable} might interact with are loaded.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param type
     *     The type of movable being toggled.
     * @param newCuboid
     *     The target cuboid of the movable.
     * @param actionType
     *     The type of action.
     * @return {@link MovableToggleResult#SUCCESS} if it can be toggled
     */
    MovableToggleResult canBeToggled(
        IMovableConst movable, MovableType type, Cuboid newCuboid, MovableActionType actionType)
    {
        if (!movableActivityManager.attemptRegisterAsBusy(movable.getUid()))
            return MovableToggleResult.BUSY;

        if (actionType == MovableActionType.OPEN && !movable.isOpenable())
            return MovableToggleResult.ALREADY_OPEN;
        else if (actionType == MovableActionType.CLOSE && !movable.isCloseable())
            return MovableToggleResult.ALREADY_CLOSED;

        if (movable.isLocked())
            return MovableToggleResult.LOCKED;

        if (!movableTypeManager.isMovableTypeEnabled(type))
            return MovableToggleResult.TYPE_DISABLED;

        if (!chunksLoaded(movable, newCuboid))
        {
            log.atWarning().log("Chunks for movable '%s' could not be not loaded!", movable.getName());
            return MovableToggleResult.ERROR;
        }

        return MovableToggleResult.SUCCESS;
    }

    private boolean chunksLoaded(IMovableConst movable, Cuboid newCuboid)
    {
        final var mode = config.loadChunksForToggle() ?
                         IChunkLoader.ChunkLoadMode.ATTEMPT_LOAD : IChunkLoader.ChunkLoadMode.VERIFY_LOADED;

        final var result = chunkLoader.checkChunks(movable.getWorld(), movable.getCuboid(), mode);
        if (result == IChunkLoader.ChunkLoadResult.FAIL)
            return false;

        return chunkLoader.checkChunks(movable.getWorld(), newCuboid, mode) != IChunkLoader.ChunkLoadResult.FAIL;
    }

    /**
     * Checks if a {@link BlockMover} of a {@link IMovable} has been registered with the {@link DatabaseManager}.
     *
     * @param movableUID
     *     The UID of the {@link IMovable}.
     * @return True if a {@link BlockMover} has been registered with the {@link DatabaseManager} for the
     * {@link IMovable}.
     */
    @SuppressWarnings("unused")
    public boolean isBlockMoverRegistered(long movableUID)
    {
        return getBlockMover(movableUID).isPresent();
    }

    /**
     * Gets the {@link BlockMover} of a {@link IMovable} if it has been registered with the {@link DatabaseManager}.
     *
     * @param movableUID
     *     The UID of the {@link IMovable}.
     * @return The {@link BlockMover} of a {@link IMovable} if it has been registered with the {@link DatabaseManager}.
     */
    public Optional<BlockMover> getBlockMover(long movableUID)
    {
        return movableActivityManager.getBlockMover(movableUID);
    }
}
