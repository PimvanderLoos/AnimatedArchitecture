package nl.pim16aap2.bigdoors.movable;

import com.google.common.flogger.StackSize;
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
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.AnimationBlockManagerFactory;
import nl.pim16aap2.bigdoors.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.moveblocks.Animator;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationBlockManager;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
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
    private final LimitsManager limitsManager;
    private final IBigDoorsEventCaller bigDoorsEventCaller;
    private final AnimationBlockManagerFactory animationBlockManagerFactory;
    private final MovementRequestData.IFactory movementRequestDataFactory;

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
        LimitsManager limitsManager,
        IBigDoorsEventCaller bigDoorsEventCaller,
        AnimationBlockManagerFactory animationBlockManagerFactory,
        MovementRequestData.IFactory movementRequestDataFactory)
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
        this.limitsManager = limitsManager;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
        this.animationBlockManagerFactory = animationBlockManagerFactory;
        this.movementRequestDataFactory = movementRequestDataFactory;
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
     * @param stamp
     *     The stamp of the animation. When this is not null, this stamp will be used to unregister the animation. This
     *     will 'release the lock' for the UID.
     * @return The same result that was passed in as argument.
     */
    private MovableToggleResult abort(
        AbstractMovable movable, MovableToggleResult result, MovableActionCause cause, IPPlayer responsible,
        IMessageable messageReceiver, @Nullable Long stamp)
    {
        log.atFine().log("Aborted toggle for movable %d because of %s. Toggle Reason: %s, Responsible: %s",
                         movable.getUid(), result.name(), cause.name(), responsible.asString());

        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this movable. However, in every other case it should, because the movable is
        // registered as busy before all the other checks take place.
        if (stamp != null)
            movableActivityManager.unregisterAnimation(movable.getUid(), stamp);

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

                if (result.equals(MovableToggleResult.INSTANCE_UNREGISTERED))
                    log.at(level).withStackTrace(StackSize.FULL)
                       .log("Encountered unregistered movable movable: %d", movable.getUid());
                else
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
    private IMovableEventTogglePrepare callTogglePrepareEvent(
        MovableSnapshot snapshot, MovableActionCause cause, MovableActionType actionType, IPPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid)
    {
        final IMovableEventTogglePrepare event =
            bigDoorsEventFactory.createTogglePrepareEvent(
                snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
        callMovableToggleEvent(event);
        return event;
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createTogglePrepareEvent(MovableSnapshot, MovableActionCause, MovableActionType,
     * IPPlayer, double, boolean, Cuboid)}.
     */
    private IMovableEventTogglePrepare callTogglePrepareEvent(MovementRequestData data)
    {
        return callTogglePrepareEvent(
            data.getSnapshotOfMovable(),
            data.getCause(),
            data.getActionType(),
            data.getResponsible(),
            data.getAnimationTime(),
            data.isAnimationSkipped(),
            data.getNewCuboid());
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createToggleStartEvent(AbstractMovable, MovableSnapshot, MovableActionCause,
     * MovableActionType, IPPlayer, double, boolean, Cuboid)}.
     */
    private IMovableEventToggleStart callToggleStartEvent(
        AbstractMovable movable, MovableSnapshot snapshot, MovableActionCause cause, MovableActionType actionType,
        IPPlayer responsible, double time, boolean skipAnimation, Cuboid newCuboid)
    {
        final IMovableEventToggleStart event =
            bigDoorsEventFactory.createToggleStartEvent(
                movable, snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
        callMovableToggleEvent(event);
        return event;
    }

    /**
     * See
     * {@link IBigDoorsEventFactory#createToggleStartEvent(AbstractMovable, MovableSnapshot, MovableActionCause,
     * MovableActionType, IPPlayer, double, boolean, Cuboid)}.
     */
    private IMovableEventToggleStart callToggleStartEvent(AbstractMovable movable, MovementRequestData data)
    {
        return callToggleStartEvent(
            movable,
            data.getSnapshotOfMovable(),
            data.getCause(),
            data.getActionType(),
            data.getResponsible(),
            data.getAnimationTime(),
            data.isAnimationSkipped(),
            data.getNewCuboid());
    }

    private void callMovableToggleEvent(IMovableToggleEvent prepareEvent)
    {
        bigDoorsEventCaller.callBigDoorsEvent(prepareEvent);
    }

    /**
     * Registers a new block mover. Must be called from the main thread.
     */
    private boolean registerBlockMover(
        AbstractMovable movable, MovementRequestData data, IAnimationComponent component, @Nullable IPPlayer player,
        AnimationType animationType, long stamp)
    {
        try
        {
            final IAnimationBlockManager animationBlockManager =
                animationBlockManagerFactory.newManager(animationType, player);

            final Animator blockMover =
                new Animator(movable, data, component, animationBlockManager);

            movableActivityManager.addAnimator(stamp, blockMover);
            executor.runOnMainThread(blockMover::startAnimation);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
            return false;
        }
        return true;
    }

    private MovableToggleResult toggle(
        MovableSnapshot snapshot, AbstractMovable targetMovable, MovementRequestData data,
        IAnimationComponent component, IMessageable messageReceiver, @Nullable IPPlayer player,
        AnimationType animationType)
    {
        if (snapshot.getOpenDir() == MovementDirection.NONE)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("OpenDir cannot be 'NONE'!");
            return MovableToggleResult.ERROR;
        }

        if (!movableRegistry.isRegistered(targetMovable))
            return abort(targetMovable, MovableToggleResult.INSTANCE_UNREGISTERED, data.getCause(),
                         data.getResponsible(), messageReceiver, null);

        final OptionalLong registrationResult =
            movableActivityManager.registerAnimation(snapshot.getUid(), animationType.requiresWriteAccess());
        if (registrationResult.isEmpty())
            return MovableToggleResult.BUSY;

        final long stamp = registrationResult.getAsLong();

        final MovableToggleResult isOpenable =
            canBeToggled(snapshot, targetMovable.getType(), data.getNewCuboid(), data.getActionType());

        if (isOpenable != MovableToggleResult.SUCCESS)
            return abort(targetMovable, isOpenable, data.getCause(), data.getResponsible(), messageReceiver, stamp);

        final IMovableEventTogglePrepare prepareEvent = callTogglePrepareEvent(data);
        if (prepareEvent.isCancelled())
            return abort(targetMovable, MovableToggleResult.CANCELLED, data.getCause(), data.getResponsible(),
                         messageReceiver, stamp);

        final @Nullable IPPlayer responsiblePlayer =
            data.getCause().equals(MovableActionCause.PLAYER) ? data.getResponsible() : null;
        if (!isLocationEmpty(data.getNewCuboid(), snapshot.getCuboid(), responsiblePlayer, snapshot.getWorld()))
            return abort(targetMovable, MovableToggleResult.OBSTRUCTED, data.getCause(), data.getResponsible(),
                         messageReceiver, stamp);

        if (!canBreakBlocksBetweenLocs(snapshot, data.getNewCuboid(), data.getResponsible()))
            return abort(targetMovable, MovableToggleResult.NO_PERMISSION, data.getCause(), data.getResponsible(),
                         messageReceiver, stamp);

        final boolean scheduled = registerBlockMover(targetMovable, data, component, player, animationType, stamp);
        if (!scheduled)
            return MovableToggleResult.ERROR;

        executor.runAsync(() -> callToggleStartEvent(targetMovable, data));

        return MovableToggleResult.SUCCESS;
    }

    MovableToggleResult toggle(AbstractMovable movable, MovableToggleRequest request, IPPlayer responsible)
    {
        final Optional<Cuboid> newCuboid;
        final double animationTime;
        final MovableSnapshot snapshot;
        final MovementRequestData data;
        final IAnimationComponent component;

        movable.getLock().readLock().lock();
        try
        {
            if (request.isSkipAnimation() && !movable.canSkipAnimation())
                return abort(
                    movable, MovableToggleResult.ERROR, request.getCause(), responsible, request.getMessageReceiver(),
                    null);

            if (exceedSizeLimit(movable, responsible))
                return abort(
                    movable, MovableToggleResult.TOO_BIG, request.getCause(), responsible,
                    request.getMessageReceiver(), null);

            newCuboid = movable.getPotentialNewCoordinates();
            if (newCuboid.isEmpty())
                return abort(
                    movable, MovableToggleResult.ERROR, request.getCause(), responsible, request.getMessageReceiver(),
                    null);

            animationTime = movable.getAnimationTime(request.getTime());
            snapshot = movable.getSnapshot();

            data = movementRequestDataFactory.newToggleRequestData(
                snapshot, request.getCause(), animationTime, request.isSkipAnimation(), newCuboid.get(), responsible,
                request.getAnimationType(), request.getActionType());
            component = movable.constructAnimationComponent(data);
        }
        finally
        {
            movable.getLock().readLock().unlock();
        }
        return toggle(
            snapshot, movable, data, component, request.getMessageReceiver(), responsible, request.getAnimationType());
    }

    /**
     * Checks if this movable exceeds the size limit for the given player.
     * <p>
     * See {@link LimitsManager#exceedsLimit(IPPlayer, Limit, int)}.
     *
     * @param player
     *     The player whose limit to compare against this movable's size.
     * @return True if {@link AbstractMovable#getBlockCount()} exceeds the {@link Limit#MOVABLE_SIZE} for this movable.
     */
    private boolean exceedSizeLimit(AbstractMovable movable, IPPlayer player)
    {
        return limitsManager.exceedsLimit(player, Limit.MOVABLE_SIZE, movable.getBlockCount());
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
    private MovableToggleResult canBeToggled(
        IMovableConst movable, MovableType type, Cuboid newCuboid, MovableActionType actionType)
    {
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
}