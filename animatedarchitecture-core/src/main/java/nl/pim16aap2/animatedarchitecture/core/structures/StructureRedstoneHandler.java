package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jspecify.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Handles all redstone-related logic for a {@link Structure}.
 * <p>
 * This class owns the redstone verification pipeline: it captures a lightweight snapshot of the structure's
 * redstone-relevant state, performs chunk checks and power queries outside any structure lock, validates that the
 * snapshot is still current, and dispatches toggle actions if appropriate.
 * <p>
 * The handler maintains a version counter that is incremented whenever redstone-relevant state changes. If a snapshot
 * becomes stale (version mismatch), its result is discarded and a single coalesced re-verification is scheduled
 * instead.
 * <p>
 * This class does not acquire the structure's read/write lock directly. It receives consistent snapshots via a
 * {@link Supplier} that the {@link Structure} provides, which internally acquires a short-lived read lock.
 *
 * @see Structure
 */
@CustomLog
@ThreadSafe
@ExtensionMethod(CompletableFutureExtensions.class)
final class StructureRedstoneHandler
{
    private final Structure structure;
    private final long uid;
    private final IWorld world;
    private final StructureOwner primeOwner;
    private final IConfig config;
    private final Supplier<RedstoneSnapshot> snapshotSupplier;
    private final IRedstoneManager redstoneManager;
    private final IChunkLoader chunkLoader;
    private final StructureActivityManager structureActivityManager;
    private final StructureAnimationRequestBuilder toggleRequestBuilder;
    private final IPlayerFactory playerFactory;
    private final IExecutor executor;

    /**
     * Version counter incremented on every redstone-relevant state change. Used to detect stale snapshots.
     */
    private final AtomicLong version = new AtomicLong(0);

    /**
     * Guard that ensures at most one coalesced verification is scheduled at a time.
     */
    private final AtomicBoolean verificationScheduled = new AtomicBoolean(false);

    StructureRedstoneHandler(
        Structure structure,
        long uid,
        IWorld world,
        StructureOwner primeOwner,
        IConfig config,
        Supplier<RedstoneSnapshot> snapshotSupplier,
        IRedstoneManager redstoneManager,
        IChunkLoader chunkLoader,
        StructureActivityManager structureActivityManager,
        StructureAnimationRequestBuilder toggleRequestBuilder,
        IPlayerFactory playerFactory,
        IExecutor executor)
    {
        this.structure = structure;
        this.uid = uid;
        this.world = world;
        this.primeOwner = primeOwner;
        this.config = config;
        this.snapshotSupplier = snapshotSupplier;
        this.redstoneManager = redstoneManager;
        this.chunkLoader = chunkLoader;
        this.structureActivityManager = structureActivityManager;
        this.toggleRequestBuilder = toggleRequestBuilder;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    /**
     * Returns the current version of the redstone-relevant state.
     * <p>
     * This is intended to be called by the snapshot supplier while it holds the structure's read lock, so the version
     * is captured atomically with the rest of the snapshot.
     *
     * @return The current version.
     */
    long currentVersion()
    {
        return version.get();
    }

    /**
     * Signals that redstone-relevant state has changed.
     * <p>
     * Increments the version counter and schedules a coalesced verification. Safe to call from any thread, including
     * while holding the structure's write lock (the actual verification runs asynchronously).
     */
    void onStateChanged()
    {
        version.incrementAndGet();
        scheduleVerification();
    }

    /**
     * Handles the chunk of the power block (or rotation point) being loaded.
     * <p>
     * Captures a snapshot of the structure's redstone-relevant state, checks that the power block and rotation point
     * chunks are loaded, and if so, queries the redstone power state and applies the appropriate action.
     */
    void onChunkLoad()
    {
        if (shouldIgnoreRedstone())
            return;

        final RedstoneSnapshot snapshot = snapshotSupplier.get();

        if (!isChunkLoaded(snapshot.powerBlock()))
            return;

        // TODO: We should probably make the checks a bit more comprehensive.
        //       Instead of checking if a few chunks with specific points are loaded, we should check if all chunks
        //       that the structure will interact with are loaded.
        if (snapshot.rotationPoint() != null && !isChunkLoaded(snapshot.rotationPoint()))
            return;

        final var status = redstoneManager.isBlockPowered(world, snapshot.powerBlock());
        applyRedstoneAction(snapshot, status);
    }

    /**
     * Verifies the redstone state of this structure.
     * <p>
     * Captures a snapshot, checks that the power block chunk is loaded, queries the block's power state, and applies
     * the appropriate toggle action. If the snapshot is stale by the time the action is applied, the result is
     * discarded and a fresh verification is scheduled.
     */
    void verifyRedstoneState()
    {
        if (shouldIgnoreRedstone())
            return;

        final RedstoneSnapshot snapshot = snapshotSupplier.get();

        if (!isChunkLoaded(snapshot.powerBlock()))
            return;

        final var status = redstoneManager.isBlockPowered(world, snapshot.powerBlock());
        applyRedstoneAction(snapshot, status);
    }

    /**
     * Handles a change in redstone power.
     * <p>
     * This is the fast path for listener-provided power state. It assumes:
     * <ol>
     *     <li>The chunk containing the power block is loaded.</li>
     *     <li>The provided {@code isPowered} value reflects the power state of the power block in the world.</li>
     * </ol>
     * <p>
     * If you cannot guarantee these assumptions, use {@link #verifyRedstoneState()} instead.
     *
     * @param isPowered
     *     The power state of the power block in the world.
     */
    void onRedstoneChange(boolean isPowered)
    {
        if (shouldIgnoreRedstone())
            return;

        final RedstoneSnapshot snapshot = snapshotSupplier.get();
        final var status = isPowered
            ? IRedstoneManager.RedstoneStatus.POWERED
            : IRedstoneManager.RedstoneStatus.UNPOWERED;

        applyRedstoneAction(snapshot, status);
    }

    /**
     * Determines and executes the appropriate redstone action based on the snapshot and power status.
     * <p>
     * If the snapshot is stale (its version does not match the current version), the action is discarded and a fresh
     * verification is scheduled instead.
     *
     * @param snapshot
     *     The captured redstone-relevant state.
     * @param status
     *     The redstone power status.
     */
    private void applyRedstoneAction(RedstoneSnapshot snapshot, IRedstoneManager.RedstoneStatus status)
    {
        if (status == IRedstoneManager.RedstoneStatus.DISABLED)
            return;

        if (snapshot.version() != version.get())
        {
            scheduleVerification();
            return;
        }

        if (status == IRedstoneManager.RedstoneStatus.UNPOWERED && snapshot.canMovePerpetually())
        {
            structureActivityManager.stopAnimatorsWithWriteAccess(uid);
            return;
        }

        final StructureActionType actionType;
        if (snapshot.canMovePerpetually())
        {
            actionType = StructureActionType.TOGGLE;
        }
        else if (snapshot.openStatus().isSet())
        {
            final boolean isOpen = Boolean.TRUE.equals(snapshot.openStatus().value());
            if (status == IRedstoneManager.RedstoneStatus.POWERED && !isOpen)
            {
                actionType = StructureActionType.OPEN;
            }
            else if (status == IRedstoneManager.RedstoneStatus.UNPOWERED && isOpen)
            {
                actionType = StructureActionType.CLOSE;
            }
            else
            {
                log.atTrace().log(
                    "Aborted toggle attempt with %s redstone for openable structure: %d",
                    status,
                    uid
                );
                return;
            }
        }
        else
        {
            log.atTrace().log(
                "Aborted toggle attempt with %s redstone for structure: %d",
                status,
                uid
            );
            return;
        }

        toggleRequestBuilder
            .builder()
            .structure(structure)
            .structureActionCause(StructureActionCause.REDSTONE)
            .structureActionType(actionType)
            .messageReceiverServer()
            .responsible(playerFactory.create(primeOwner.playerData()))
            .build()
            .execute()
            .orTimeout(30, TimeUnit.SECONDS)
            .handleExceptional(ex ->
                log.atError().withCause(ex).log(
                    "Toggle structure %d with redstone status %s",
                    uid,
                    status
                ));
    }

    /**
     * @return True if this structure should ignore all redstone interaction.
     */
    private boolean shouldIgnoreRedstone()
    {
        return uid < 1 || !config.allowRedstone();
    }

    private boolean isChunkLoaded(IVector3D position)
    {
        return chunkLoader.checkChunk(world, position, IChunkLoader.ChunkLoadMode.VERIFY_LOADED)
            == IChunkLoader.ChunkLoadResult.PASS;
    }

    /**
     * Schedules a single coalesced verification. If a verification is already scheduled, this call is a no-op,
     * preventing verification storms from rapid state changes.
     */
    private void scheduleVerification()
    {
        if (verificationScheduled.compareAndSet(false, true))
        {
            executor.runAsyncLater(
                () ->
                {
                    verificationScheduled.set(false);
                    verifyRedstoneState();
                },
                1
            );
        }
    }

    /**
     * Lightweight snapshot of the structure state needed for redstone decisions.
     * <p>
     * This intentionally captures only redstone-decision-relevant fields, not the full {@link StructureSnapshot}, to
     * keep the read-lock hold time minimal.
     *
     * @param powerBlock
     *     The position of the power block.
     * @param openStatus
     *     The open/closed status of the structure.
     * @param rotationPoint
     *     The rotation point, if set. Used for chunk-load pre-flight checks.
     * @param canMovePerpetually
     *     Whether this structure can move perpetually (e.g. flags, windmills).
     * @param version
     *     The redstone state version at the time of capture, used for staleness detection.
     */
    record RedstoneSnapshot(
        Vector3Di powerBlock,
        IPropertyValue<Boolean> openStatus,
        @Nullable Vector3Di rotationPoint,
        boolean canMovePerpetually,
        long version)
    {
    }
}
