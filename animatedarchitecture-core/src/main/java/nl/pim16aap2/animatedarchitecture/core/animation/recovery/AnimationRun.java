package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable data for a single animation run.
 *
 * @param uuid
 *     The public identifier of the animation run.
 * @param sessionUuid
 *     The plugin session that created this animation run.
 * @param structureUid
 *     The UID of the structure being animated.
 * @param actionType
 *     The requested structure action.
 * @param animationType
 *     The type of animation that was executed.
 * @param startedAt
 *     The time at which the animation run started.
 * @param endedAt
 *     The time at which the animation run ended, if available.
 * @param status
 *     The current animation run status.
 * @param expectedAnimatedBlockCount
 *     The number of animated blocks expected for this run, if known.
 * @param recoveredBlockCount
 *     The number of orphaned animated blocks recovered for this run.
 * @param lastRecoveredAt
 *     The most recent time at which an orphaned animated block was recovered for this run, if any.
 * @param recoveryCompletedAt
 *     The time at which the recovered block count reached the expected animated block count, if known.
 * @param diagnosticMessage
 *     Optional diagnostic details for failure or recovery.
 */
public record AnimationRun(
    UUID uuid,
    UUID sessionUuid,
    long structureUid,
    StructureActionType actionType,
    AnimationType animationType,
    Instant startedAt,
    @Nullable Instant endedAt,
    AnimationRunStatus status,
    @Nullable Integer expectedAnimatedBlockCount,
    int recoveredBlockCount,
    @Nullable Instant lastRecoveredAt,
    @Nullable Instant recoveryCompletedAt,
    @Nullable String diagnosticMessage
)
{
}
