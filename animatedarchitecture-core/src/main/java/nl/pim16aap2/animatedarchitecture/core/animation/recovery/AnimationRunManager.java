package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tracks individual animation runs for recovery diagnostics.
 */
@Singleton
@CustomLog
public final class AnimationRunManager
{
    private final DatabaseManager databaseManager;
    private final PluginSessionManager pluginSessionManager;

    /**
     * Creates an animation run manager.
     *
     * @param databaseManager
     *     The database manager used to persist animation run state.
     * @param pluginSessionManager
     *     The plugin session manager that owns the current session UUID.
     */
    @Inject
    public AnimationRunManager(DatabaseManager databaseManager, PluginSessionManager pluginSessionManager)
    {
        this.databaseManager = databaseManager;
        this.pluginSessionManager = pluginSessionManager;
    }

    /**
     * Registers the start of a new animation run.
     *
     * @param structureUid
     *     The UID of the structure being animated.
     * @param actionType
     *     The structure action type.
     * @param animationType
     *     The animation type.
     * @return The future animation run UUID.
     */
    public CompletableFuture<UUID> registerRunStart(
        long structureUid,
        StructureActionType actionType,
        AnimationType animationType)
    {
        final UUID sessionUuid = pluginSessionManager
            .getCurrentSessionUuid()
            .orElseThrow(() ->
                new IllegalStateException("Cannot start animation run without an active plugin session."));

        final UUID runUuid = UuidCreator.getTimeOrderedEpoch();
        return databaseManager
            .startAnimationRun(
                runUuid,
                sessionUuid,
                structureUid,
                actionType,
                animationType,
                Instant.now())
            .orTimeout(5, TimeUnit.SECONDS)
            .thenApply(run ->
            {
                if (run.isEmpty())
                    throw new IllegalStateException("Failed to create animation run: " + runUuid);
                return runUuid;
            });
    }

    /**
     * Stores the expected animated block count for an animation run.
     *
     * @param runUuid
     *     The animation run UUID.
     * @param expectedAnimatedBlockCount
     *     The number of animated blocks created for the run.
     */
    public void registerExpectedAnimatedBlockCount(UUID runUuid, int expectedAnimatedBlockCount)
    {
        @SuppressWarnings("unused") final var unused = databaseManager
            .updateAnimationRunExpectedAnimatedBlockCount(runUuid, expectedAnimatedBlockCount)
            .orTimeout(5, TimeUnit.SECONDS)
            .handle((updated, throwable) ->
            {
                if (throwable != null)
                {
                    log.atError().withCause(throwable).log(
                        "Failed to set expected animated block count for animation run %s.", runUuid);
                }
                else if (!updated)
                {
                    log.atWarn().log("Animation run %s was not updated with expected animated block count.", runUuid);
                }
                return null;
            });
    }

    /**
     * Marks an animation run as completed.
     *
     * @param runUuid
     *     The animation run UUID.
     */
    public void registerRunCompletion(UUID runUuid)
    {
        finishRun(runUuid, AnimationRunStatus.COMPLETED, null);
    }

    /**
     * Marks an animation run as failed.
     *
     * @param runUuid
     *     The animation run UUID.
     * @param diagnosticMessage
     *     Diagnostic details.
     */
    public void registerRunFailure(UUID runUuid, @Nullable String diagnosticMessage)
    {
        finishRun(runUuid, AnimationRunStatus.FAILED, diagnosticMessage);
    }

    private void finishRun(UUID runUuid, AnimationRunStatus status, @Nullable String diagnosticMessage)
    {
        @SuppressWarnings("unused") final var unused =
            databaseManager
                .finishAnimationRun(runUuid, status, Instant.now(), diagnosticMessage)
                .orTimeout(5, TimeUnit.SECONDS)
                .handle((updated, throwable) ->
                {
                    if (throwable != null)
                        log
                            .atError()
                            .withCause(throwable)
                            .log("Failed to finish animation run %s as %s.", runUuid, status);
                    else if (!updated)
                        log.atWarn().log("Animation run %s was not updated to %s.", runUuid, status);
                    return null;
                });
    }

    /**
     * Records recovery of orphaned animated blocks.
     *
     * @param runUuid
     *     The animation run UUID.
     * @param recoveredBlockCount
     *     The number of recovered blocks.
     * @param diagnosticMessage
     *     Diagnostic details.
     */
    public void recordRecoveredBlocks(UUID runUuid, int recoveredBlockCount, @Nullable String diagnosticMessage)
    {
        if (recoveredBlockCount < 1)
            throw new IllegalArgumentException("Recovered block count must be positive!");

        @SuppressWarnings("unused") final var unused =
            databaseManager
                .addRecoveredBlockCount(runUuid, recoveredBlockCount, Instant.now(), diagnosticMessage)
                .orTimeout(5, TimeUnit.SECONDS)
                .handle((context, throwable) ->
                {
                    if (throwable != null)
                    {
                        log.atError().withCause(throwable).log(
                            "Failed to record %d recovered blocks for animation run %s.",
                            recoveredBlockCount,
                            runUuid);
                    }
                    else if (context.isEmpty())
                    {
                        log.atError().log(
                            "Recorded %d recovered block(s) for unknown animation run '%s'.",
                            recoveredBlockCount,
                            runUuid);
                    }
                    else
                    {
                        logRecoveryContext(context.get(), recoveredBlockCount);
                    }
                    return null;
                });
    }

    private void logRecoveryContext(AnimationRecoveryContext context, int recoveredBlockCount)
    {
        final AnimationRun run = context.animationRun();
        final Integer expectedCount = run.expectedAnimatedBlockCount();
        if (expectedCount != null && run.recoveredBlockCount() > expectedCount)
        {
            log.atError().log(
                "Recovered %d/%d orphaned animated block(s) for animation run '%s' and structure '%d'. " +
                    "The recovered total exceeds the expected animated block count.",
                run.recoveredBlockCount(),
                expectedCount,
                run.uuid(),
                run.structureUid()
            );
            return;
        }

        final String expected = expectedCount == null ? "unknown" : expectedCount.toString();
        log.atDebug().log(
            "Recovered %d orphaned animated block(s) for animation run '%s' and structure '%d'. " +
                "Total recovered: %d/%s. Session status: %s.",
            recoveredBlockCount,
            run.uuid(),
            run.structureUid(),
            run.recoveredBlockCount(),
            expected,
            context.pluginSession().status()
        );
    }
}
