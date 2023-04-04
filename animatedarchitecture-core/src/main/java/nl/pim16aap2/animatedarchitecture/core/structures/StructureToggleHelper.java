package nl.pim16aap2.animatedarchitecture.core.structures;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationBlockManagerFactory;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.animation.Animator;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationBlockManager;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEventTogglePrepare;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEventToggleStart;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureToggleEvent;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents a utility singleton that is used to open {@link IStructure}s.
 *
 * @author Pim
 */
@Flogger final class StructureToggleHelper
{
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final StructureActivityManager structureActivityManager;
    private final StructureTypeManager structureTypeManager;
    private final IConfig config;
    private final IExecutor executor;
    private final IBlockAnalyzer<?> blockAnalyzer;
    private final ILocationFactory locationFactory;
    private final IProtectionHookManager protectionHookManager;
    private final HighlightedBlockSpawner highlightedBlockSpawner;
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;
    private final StructureRegistry structureRegistry;
    private final IChunkLoader chunkLoader;
    private final LimitsManager limitsManager;
    private final IAnimatedArchitectureEventCaller animatedArchitectureEventCaller;
    private final AnimationBlockManagerFactory animationBlockManagerFactory;
    private final AnimationRequestData.IFactory movementRequestDataFactory;

    @Inject //
    StructureToggleHelper(
        ILocalizer localizer,
        ITextFactory textFactory,
        StructureActivityManager structureActivityManager,
        StructureTypeManager structureTypeManager,
        IConfig config,
        IExecutor executor,
        IBlockAnalyzer<?> blockAnalyzer,
        ILocationFactory locationFactory,
        IProtectionHookManager protectionHookManager,
        HighlightedBlockSpawner highlightedBlockSpawner,
        IAnimatedArchitectureEventFactory animatedArchitectureEventFactory,
        StructureRegistry structureRegistry,
        IChunkLoader chunkLoader,
        LimitsManager limitsManager,
        IAnimatedArchitectureEventCaller animatedArchitectureEventCaller,
        AnimationBlockManagerFactory animationBlockManagerFactory,
        AnimationRequestData.IFactory movementRequestDataFactory)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.structureActivityManager = structureActivityManager;
        this.structureTypeManager = structureTypeManager;
        this.config = config;
        this.executor = executor;
        this.blockAnalyzer = blockAnalyzer;
        this.locationFactory = locationFactory;
        this.protectionHookManager = protectionHookManager;
        this.highlightedBlockSpawner = highlightedBlockSpawner;
        this.animatedArchitectureEventFactory = animatedArchitectureEventFactory;
        this.structureRegistry = structureRegistry;
        this.chunkLoader = chunkLoader;
        this.limitsManager = limitsManager;
        this.animatedArchitectureEventCaller = animatedArchitectureEventCaller;
        this.animationBlockManagerFactory = animationBlockManagerFactory;
        this.movementRequestDataFactory = movementRequestDataFactory;
    }

    /**
     * Aborts an attempt to toggle a {@link IStructure} and cleans up leftover data from this attempt.
     *
     * @param structure
     *     The {@link IStructure}.
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
    private StructureToggleResult abort(
        AbstractStructure structure, StructureToggleResult result, StructureActionCause cause, IPlayer responsible,
        IMessageable messageReceiver, @Nullable Long stamp)
    {
        log.atFine().log("Aborted toggle for structure %d because of %s. Toggle Reason: %s, Responsible: %s",
                         structure.getUid(), result.name(), cause.name(), responsible.asString());

        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this structure. However, in every other case it should, because the structure is
        // registered as busy before all the other checks take place.
        if (stamp != null)
            structureActivityManager.unregisterAnimation(structure.getUid(), stamp);

        if (messageReceiver instanceof IPlayer)
            messageReceiver.sendMessage(textFactory.newText().append(
                localizer.getMessage(result.getLocalizationKey()), TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(structure.getType())),
                arg -> arg.highlight(structure.getName())));
        else
        {
            final Level level = result == StructureToggleResult.BUSY ? Level.FINE : Level.INFO;

            if (result.equals(StructureToggleResult.INSTANCE_UNREGISTERED))
                log.at(level).withStackTrace(StackSize.FULL)
                   .log("Encountered unregistered structure structure: %d", structure.getUid());
            else
                log.at(level).log("Failed to toggle structure: %d, reason: %s", structure.getUid(), result.name());
        }
        return result;
    }

    /**
     * See
     * {@link IAnimatedArchitectureEventFactory#createTogglePrepareEvent(StructureSnapshot, StructureActionCause,
     * StructureActionType, IPlayer, double, boolean, Cuboid)}.
     */
    private IStructureEventTogglePrepare callTogglePrepareEvent(
        StructureSnapshot snapshot, StructureActionCause cause, StructureActionType actionType, IPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid)
    {
        final IStructureEventTogglePrepare event =
            animatedArchitectureEventFactory.createTogglePrepareEvent(
                snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
        callStructureToggleEvent(event);
        return event;
    }

    /**
     * See
     * {@link IAnimatedArchitectureEventFactory#createTogglePrepareEvent(StructureSnapshot, StructureActionCause,
     * StructureActionType, IPlayer, double, boolean, Cuboid)}.
     */
    private IStructureEventTogglePrepare callTogglePrepareEvent(AnimationRequestData data)
    {
        return callTogglePrepareEvent(
            data.getStructureSnapshot(),
            data.getCause(),
            data.getActionType(),
            data.getResponsible(),
            data.getAnimationTime(),
            data.isAnimationSkipped(),
            data.getNewCuboid());
    }

    /**
     * See
     * {@link IAnimatedArchitectureEventFactory#createToggleStartEvent(AbstractStructure, StructureSnapshot,
     * StructureActionCause, StructureActionType, IPlayer, double, boolean, Cuboid)}.
     */
    private IStructureEventToggleStart callToggleStartEvent(
        AbstractStructure structure, StructureSnapshot snapshot, StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible, double time, boolean skipAnimation, Cuboid newCuboid)
    {
        final IStructureEventToggleStart event =
            animatedArchitectureEventFactory.createToggleStartEvent(
                structure, snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
        callStructureToggleEvent(event);
        return event;
    }

    /**
     * See
     * {@link IAnimatedArchitectureEventFactory#createToggleStartEvent(AbstractStructure, StructureSnapshot,
     * StructureActionCause, StructureActionType, IPlayer, double, boolean, Cuboid)}.
     */
    private IStructureEventToggleStart callToggleStartEvent(AbstractStructure structure, AnimationRequestData data)
    {
        return callToggleStartEvent(
            structure,
            data.getStructureSnapshot(),
            data.getCause(),
            data.getActionType(),
            data.getResponsible(),
            data.getAnimationTime(),
            data.isAnimationSkipped(),
            data.getNewCuboid());
    }

    private void callStructureToggleEvent(IStructureToggleEvent prepareEvent)
    {
        animatedArchitectureEventCaller.callAnimatedArchitectureEvent(prepareEvent);
    }

    /**
     * Registers a new block mover. Must be called from the main thread.
     */
    private boolean registerBlockMover(
        AbstractStructure structure, AnimationRequestData data, IAnimationComponent component,
        @Nullable IPlayer player,
        AnimationType animationType, long stamp)
    {
        try
        {
            final IAnimationBlockManager animationBlockManager =
                animationBlockManagerFactory.newManager(animationType, player);

            final Animator blockMover =
                new Animator(structure, data, component, animationBlockManager);

            structureActivityManager.addAnimator(stamp, blockMover);
            executor.runOnMainThread(blockMover::startAnimation);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
            return false;
        }
        return true;
    }

    private StructureToggleResult toggle(
        StructureSnapshot snapshot,
        AbstractStructure targetStructure,
        AnimationRequestData data,
        IAnimationComponent component,
        IMessageable messageReceiver,
        @Nullable IPlayer player,
        AnimationType animationType)
    {
        if (snapshot.getOpenDir() == MovementDirection.NONE)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("OpenDir cannot be 'NONE'!");
            return StructureToggleResult.ERROR;
        }

        // Read-only animations are fine for unregistered structures, as they do (should) not have any side effects.
        if (animationType.requiresWriteAccess() && !structureRegistry.isRegistered(targetStructure))
            return abort(targetStructure, StructureToggleResult.INSTANCE_UNREGISTERED, data.getCause(),
                         data.getResponsible(), messageReceiver, null);

        final OptionalLong registrationResult =
            structureActivityManager.registerAnimation(targetStructure, animationType.requiresWriteAccess());
        if (registrationResult.isEmpty())
            return StructureToggleResult.BUSY;

        final long stamp = registrationResult.getAsLong();

        final StructureToggleResult isToggleable =
            canBeToggled(snapshot, targetStructure.getType(), data.getNewCuboid(), data.getActionType());

        if (isToggleable != StructureToggleResult.SUCCESS)
            return abort(targetStructure, isToggleable, data.getCause(), data.getResponsible(), messageReceiver, stamp);

        final IStructureEventTogglePrepare prepareEvent = callTogglePrepareEvent(data);
        if (prepareEvent.isCancelled())
            return abort(targetStructure, StructureToggleResult.CANCELLED, data.getCause(), data.getResponsible(),
                         messageReceiver, stamp);

        final @Nullable IPlayer responsiblePlayer =
            data.getCause().equals(StructureActionCause.PLAYER) ? data.getResponsible() : null;

        if (animationType.requiresWriteAccess() &&
            !isLocationEmpty(data.getNewCuboid(), snapshot.getCuboid(), responsiblePlayer, snapshot.getWorld()))
            return abort(targetStructure, StructureToggleResult.OBSTRUCTED, data.getCause(), data.getResponsible(),
                         messageReceiver, stamp);

        if (animationType.requiresWriteAccess() &&
            !canBreakBlocks(snapshot, snapshot.getCuboid(), data.getNewCuboid(), data.getResponsible()))
            return abort(targetStructure, StructureToggleResult.NO_PERMISSION, data.getCause(), data.getResponsible(),
                         messageReceiver, stamp);

        final boolean scheduled = registerBlockMover(targetStructure, data, component, player, animationType, stamp);
        if (!scheduled)
            return StructureToggleResult.ERROR;

        executor.runAsync(() -> callToggleStartEvent(targetStructure, data));

        return StructureToggleResult.SUCCESS;
    }

    StructureToggleResult toggle(AbstractStructure structure, StructureAnimationRequest request, IPlayer responsible)
    {
        final StructureSnapshot snapshot;
        final AnimationRequestData data;
        final IAnimationComponent component;

        structure.getLock().readLock().lock();
        try
        {
            if (request.isSkipAnimation() && !structure.canSkipAnimation())
                return abort(
                    structure, StructureToggleResult.ERROR, request.getCause(), responsible,
                    request.getMessageReceiver(),
                    null);

            if (exceedSizeLimit(structure, responsible))
                return abort(
                    structure, StructureToggleResult.TOO_BIG, request.getCause(), responsible,
                    request.getMessageReceiver(), null);

            final Optional<Cuboid> newCuboid = structure.getPotentialNewCoordinates();
            if (newCuboid.isEmpty())
                return abort(
                    structure, StructureToggleResult.ERROR, request.getCause(), responsible,
                    request.getMessageReceiver(),
                    null);

            final double animationTime = structure.getAnimationTime(request.getTime());
            snapshot = structure.getSnapshot();

            data = movementRequestDataFactory.newToggleRequestData(
                snapshot, request.getCause(), animationTime, request.isSkipAnimation(),
                request.isPreventPerpetualMovement(), newCuboid.get(), responsible,
                request.getAnimationType(), request.getActionType());
            component = structure.constructAnimationComponent(data);
        }
        finally
        {
            structure.getLock().readLock().unlock();
        }
        return toggle(
            snapshot, structure, data, component, request.getMessageReceiver(), responsible,
            request.getAnimationType());
    }

    /**
     * Checks if this structure exceeds the size limit for the given player.
     * <p>
     * See {@link LimitsManager#exceedsLimit(IPlayer, Limit, int)}.
     *
     * @param player
     *     The player whose limit to compare against this structure's size.
     * @return True if {@link AbstractStructure#getBlockCount()} exceeds the {@link Limit#STRUCTURE_SIZE} for this
     * structure.
     */
    private boolean exceedSizeLimit(AbstractStructure structure, IPlayer player)
    {
        return limitsManager.exceedsLimit(player, Limit.STRUCTURE_SIZE, structure.getBlockCount());
    }

    /**
     * Checks if the owner of a structure can break blocks inside a cuboid.
     * <p>
     * If the player is not allowed to break the block(s), they'll receive a message about this.
     *
     * @param structure
     *     The {@link IStructure} being opened.
     * @param cuboid0
     *     The first area of blocks to check.
     * @param cuboid1
     *     The second area of blocks to check.
     * @param responsible
     *     Who is responsible for the action.
     * @return True if the player is allowed to break the block(s).
     */
    public boolean canBreakBlocks(IStructureConst structure, Cuboid cuboid0, Cuboid cuboid1, IPlayer responsible)
    {
        if (protectionHookManager.canSkipCheck())
            return true;
        try
        {
            return executor.runOnMainThread(() -> canBreakBlocks0(structure, cuboid0, cuboid1, responsible))
                           .get(500, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e)
               .log("Failed to check if blocks can be broken in cuboids %s and %s for user: '%s' for structure %s",
                    cuboid0, cuboid1, responsible, structure);
            return false;
        }
    }

    private boolean canBreakBlocks0(IStructureConst structure, Cuboid cuboid0, Cuboid cuboid1, IPlayer responsible)
    {
        final boolean access0 = canBreakBlocks0(structure, cuboid0, responsible);
        final boolean access1 = cuboid0.equals(cuboid1) || canBreakBlocks0(structure, cuboid1, responsible);
        return access0 && access1;
    }

    private boolean canBreakBlocks0(IStructureConst structure, Cuboid cuboid, IPlayer responsible)
    {
        // If the returned value is an empty Optional, the player is allowed to break blocks.
        return protectionHookManager.canBreakBlocksBetweenLocs(responsible, cuboid, structure.getWorld()).map(
            protectionCompat ->
            {
                log.atWarning().log("Player '%s' is not allowed to open structure '%s' (%d) here! Reason: %s",
                                    responsible, structure.getName(), structure.getUid(), protectionCompat);
                return false;
            }).orElse(true);
    }

    /**
     * Checks if an area is empty. "Empty" here means that there no blocks that are not air or liquid.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the structure will take up after the toggle.
     * @param currentCuboid
     *     The {@link Cuboid} representing the area the structure currently takes up. Any parts of the new cuboid
     *     overlapping this cuboid will be ignored.
     * @param player
     *     The {@link IPlayer} to notify of violations. May be null.
     * @param world
     *     The world to check the blocks in.
     * @return True if the area is not empty.
     */
    public boolean isLocationEmpty(Cuboid newCuboid, Cuboid currentCuboid, @Nullable IPlayer player, IWorld world)
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
                    // Ignore blocks that are currently part of the structure.
                    // It's expected and accepted for them to be in the way.
                    if (Util.between(xAxis, curMin.x(), curMax.x()) &&
                        Util.between(yAxis, curMin.y(), curMax.y()) &&
                        Util.between(zAxis, curMin.z(), curMax.z()))
                        continue;

                    if (!blockAnalyzer.isAirOrLiquid(locationFactory.create(world, xAxis, yAxis, zAxis)))
                    {
                        if (player == null)
                            return false;

                        final int xAxis0 = xAxis;
                        final int yAxis0 = yAxis;
                        final int zAxis0 = zAxis;

                        executor.runOnMainThread(
                            () -> highlightedBlockSpawner
                                .builder()
                                .forPlayer(player)
                                .withColor(Color.RED)
                                .forDuration(Duration.ofSeconds(4))
                                .atPosition(xAxis0 + 0.5, yAxis0, zAxis0 + 0.5)
                                .inWorld(world)
                                .spawn());
                        isEmpty = false;
                    }
                }
            }
        }
        return isEmpty;
    }

    /**
     * Checks if a {@link AbstractStructure} can be toggled or not.
     * <p>
     * It checks the following items:
     * <p>
     * - The {@link AbstractStructure} is not already being animated.
     * <p>
     * - The {@link AbstractStructure} is enabled.
     * <p>
     * - The {@link AbstractStructure} is not locked.
     * <p>
     * - All chunks this {@link AbstractStructure} might interact with are loaded.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param type
     *     The type of structure being toggled.
     * @param newCuboid
     *     The target cuboid of the structure.
     * @param actionType
     *     The type of action.
     * @return {@link StructureToggleResult#SUCCESS} if it can be toggled
     */
    private StructureToggleResult canBeToggled(
        IStructureConst structure, StructureType type, Cuboid newCuboid, StructureActionType actionType)
    {
        if (actionType == StructureActionType.OPEN && !structure.isOpenable())
            return StructureToggleResult.ALREADY_OPEN;
        else if (actionType == StructureActionType.CLOSE && !structure.isCloseable())
            return StructureToggleResult.ALREADY_CLOSED;

        if (structure.isLocked())
            return StructureToggleResult.LOCKED;

        if (!structureTypeManager.isStructureTypeEnabled(type))
            return StructureToggleResult.TYPE_DISABLED;

        if (!chunksLoaded(structure, newCuboid))
        {
            log.atWarning().log("Chunks for structure '%s' could not be not loaded!", structure.getName());
            return StructureToggleResult.ERROR;
        }

        return StructureToggleResult.SUCCESS;
    }

    private boolean chunksLoaded(IStructureConst structure, Cuboid newCuboid)
    {
        final var mode = config.loadChunksForToggle() ?
                         IChunkLoader.ChunkLoadMode.ATTEMPT_LOAD : IChunkLoader.ChunkLoadMode.VERIFY_LOADED;

        final var result = chunkLoader.checkChunks(structure.getWorld(), structure.getCuboid(), mode);
        if (result == IChunkLoader.ChunkLoadResult.FAIL)
            return false;

        return chunkLoader.checkChunks(structure.getWorld(), newCuboid, mode) != IChunkLoader.ChunkLoadResult.FAIL;
    }
}
