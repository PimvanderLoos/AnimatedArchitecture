package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(5)
@ExtendWith(MockitoExtension.class)
@WithLogCapture
class AnimationRunManagerTest
{
    @InjectMocks
    private AnimationRunManager animationRunManager;

    @Mock
    private DatabaseManager databaseManager;
    @Mock
    private PluginSessionManager pluginSessionManager;

    @AfterEach
    void tearDown()
    {
        verifyNoMoreInteractions(
            databaseManager,
            pluginSessionManager
        );
    }

    @Test
    void registerRunStart_shouldThrowWhenNoActiveSession()
    {
        // setup
        when(pluginSessionManager.getCurrentSessionUuid()).thenReturn(Optional.empty());

        // execute & verify
        assertThatThrownBy(
            () -> animationRunManager.registerRunStart(1L, StructureActionType.OPEN, AnimationType.MOVE_BLOCKS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot start animation run without an active plugin session");

        verify(pluginSessionManager).getCurrentSessionUuid();
    }

    @Test
    void registerRunStart_shouldReturnRunUuidOnSuccess()
        throws Exception
    {
        // setup
        final UUID sessionUuid = UUID.randomUUID();
        when(pluginSessionManager.getCurrentSessionUuid()).thenReturn(Optional.of(sessionUuid));

        final AnimationRun run = newAnimationRun(UUID.randomUUID(), sessionUuid);
        when(databaseManager.startAnimationRun(any(), eq(sessionUuid), anyLong(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(run)));

        // execute
        final UUID resultUuid = animationRunManager
            .registerRunStart(1L, StructureActionType.OPEN, AnimationType.MOVE_BLOCKS)
            .get();

        // verify
        assertThat(resultUuid).isNotNull();

        verify(pluginSessionManager).getCurrentSessionUuid();
        verify(databaseManager).startAnimationRun(
            any(),
            eq(sessionUuid),
            eq(1L),
            eq(StructureActionType.OPEN),
            eq(AnimationType.MOVE_BLOCKS), any()
        );
    }

    @Test
    void registerRunStart_shouldThrowWhenDatabaseReturnsEmpty()
    {
        // setup
        final UUID sessionUuid = UUID.randomUUID();
        when(pluginSessionManager.getCurrentSessionUuid()).thenReturn(Optional.of(sessionUuid));
        when(databaseManager.startAnimationRun(any(), any(), anyLong(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // execute & verify
        assertThatThrownBy(
            () -> animationRunManager
                .registerRunStart(1L, StructureActionType.OPEN, AnimationType.MOVE_BLOCKS)
                .join())
            .cause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to create animation run");

        verify(pluginSessionManager).getCurrentSessionUuid();
        verify(databaseManager).startAnimationRun(
            any(),
            eq(sessionUuid),
            eq(1L),
            eq(StructureActionType.OPEN),
            eq(AnimationType.MOVE_BLOCKS),
            any()
        );
    }

    @Test
    void registerExpectedAnimatedBlockCount_shouldCallDatabase()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        when(databaseManager.updateAnimationRunExpectedAnimatedBlockCount(eq(runUuid), eq(10)))
            .thenReturn(CompletableFuture.completedFuture(true));

        // execute
        animationRunManager.registerExpectedAnimatedBlockCount(runUuid, 10);

        // verify
        verify(databaseManager).updateAnimationRunExpectedAnimatedBlockCount(runUuid, 10);
    }

    @Test
    void registerExpectedAnimatedBlockCount_shouldLogWarningWhenRunNotUpdated(LogCaptor logCaptor)
        throws Exception
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        when(databaseManager.updateAnimationRunExpectedAnimatedBlockCount(eq(runUuid), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // execute
        animationRunManager.registerExpectedAnimatedBlockCount(runUuid, 10);

        // Give the async chain time to complete before checking logs
        Thread.sleep(100);

        // verify
        assertThatLogCaptor(logCaptor)
            .atWarn()
            .singleWithMessageContaining("was not updated with expected animated block count");

        verify(databaseManager).updateAnimationRunExpectedAnimatedBlockCount(runUuid, 10);
    }

    @Test
    void registerExpectedAnimatedBlockCount_shouldLogErrorOnException(LogCaptor logCaptor)
        throws Exception
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        when(databaseManager.updateAnimationRunExpectedAnimatedBlockCount(eq(runUuid), anyInt()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DB error")));

        // execute
        animationRunManager.registerExpectedAnimatedBlockCount(runUuid, 10);

        // Give the async chain time to complete before checking logs
        Thread.sleep(100);

        // verify
        assertThatLogCaptor(logCaptor)
            .atError()
            .singleWithMessageContaining("Failed to set expected animated block count");

        verify(databaseManager).updateAnimationRunExpectedAnimatedBlockCount(runUuid, 10);
    }

    @Test
    void registerRunCompletion_shouldMarkRunAsCompleted()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        when(databaseManager.finishAnimationRun(eq(runUuid), eq(AnimationRunStatus.COMPLETED), any(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(true));

        // execute
        animationRunManager.registerRunCompletion(runUuid);

        // verify
        verify(databaseManager).finishAnimationRun(eq(runUuid), eq(AnimationRunStatus.COMPLETED), any(), isNull());
    }

    @Test
    void registerRunFailure_shouldMarkRunAsFailedWithDiagnosticMessage()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final String diagnosticMessage = "Something went wrong";
        when(databaseManager
            .finishAnimationRun(eq(runUuid), eq(AnimationRunStatus.FAILED), any(), eq(diagnosticMessage)))
            .thenReturn(CompletableFuture.completedFuture(true));

        // execute
        animationRunManager.registerRunFailure(runUuid, diagnosticMessage);

        // verify
        verify(databaseManager).finishAnimationRun(
            eq(runUuid),
            eq(AnimationRunStatus.FAILED),
            any(),
            eq(diagnosticMessage)
        );
    }

    @Test
    void registerRunCompletion_shouldLogWarningWhenRunNotUpdated(LogCaptor logCaptor)
        throws Exception
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        when(databaseManager.finishAnimationRun(any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // execute
        animationRunManager.registerRunCompletion(runUuid);

        Thread.sleep(100);

        // verify
        assertThatLogCaptor(logCaptor)
            .atWarn()
            .singleWithMessageContaining("was not updated to");

        verify(databaseManager).finishAnimationRun(eq(runUuid), eq(AnimationRunStatus.COMPLETED), any(), isNull());
    }

    @Test
    void recordRecoveredBlocks_shouldThrowWhenCountIsZero()
    {
        // execute & verify
        assertThatThrownBy(() -> animationRunManager.recordRecoveredBlocks(UUID.randomUUID(), 0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Recovered block count must be positive");
    }

    @Test
    void recordRecoveredBlocks_shouldThrowWhenCountIsNegative()
    {
        // execute & verify
        assertThatThrownBy(() -> animationRunManager.recordRecoveredBlocks(UUID.randomUUID(), -1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Recovered block count must be positive");
    }

    @Test
    void recordRecoveredBlocks_shouldCallDatabaseWithCorrectArgs()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final String message = "Recovered orphaned block";
        final UUID sessionUuid = UUID.randomUUID();
        final AnimationRun run = newAnimationRunWithRecovery(runUuid, sessionUuid, 1);
        final PluginSession session = newPluginSession(sessionUuid);
        when(databaseManager.addRecoveredBlockCount(eq(runUuid), eq(1), any(), eq(message)))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(new AnimationRecoveryContext(session, run))));

        // execute
        animationRunManager.recordRecoveredBlocks(runUuid, 1, message);

        // verify
        verify(databaseManager).addRecoveredBlockCount(eq(runUuid), eq(1), any(), eq(message));
    }

    @Test
    void recordRecoveredBlocks_shouldLogErrorWhenRunNotFound(LogCaptor logCaptor)
        throws Exception
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        when(databaseManager.addRecoveredBlockCount(any(), anyInt(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // execute
        animationRunManager.recordRecoveredBlocks(runUuid, 1, "test");

        Thread.sleep(100);

        // verify
        assertThatLogCaptor(logCaptor)
            .atError()
            .singleWithMessageContaining("unknown animation run");

        verify(databaseManager).addRecoveredBlockCount(eq(runUuid), eq(1), any(), eq("test"));
    }

    @Test
    void recordRecoveredBlocks_shouldLogErrorWhenExceedsExpectedCount(LogCaptor logCaptor)
        throws Exception
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final UUID sessionUuid = UUID.randomUUID();
        // recoveredBlockCount=5 exceeds expectedAnimatedBlockCount=3
        final AnimationRun run = newAnimationRunWithRecovery(runUuid, sessionUuid, 5);
        final PluginSession session = newPluginSession(sessionUuid);
        when(databaseManager.addRecoveredBlockCount(any(), anyInt(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(new AnimationRecoveryContext(session, run))));

        // execute
        animationRunManager.recordRecoveredBlocks(runUuid, 1, "test");

        Thread.sleep(100);

        // verify
        assertThatLogCaptor(logCaptor)
            .atError()
            .singleWithMessageContaining("exceeds the expected animated block count");

        verify(databaseManager).addRecoveredBlockCount(eq(runUuid), eq(1), any(), eq("test"));
    }

    private static AnimationRun newAnimationRun(UUID uuid, UUID sessionUuid)
    {
        return new AnimationRun(
            uuid,
            sessionUuid,
            1L,
            StructureActionType.OPEN,
            AnimationType.MOVE_BLOCKS,
            Instant.now(),
            null,
            AnimationRunStatus.ACTIVE,
            null,
            0,
            null,
            null,
            null
        );
    }

    private static AnimationRun newAnimationRunWithRecovery(UUID uuid, UUID sessionUuid, int recoveredBlockCount)
    {
        return new AnimationRun(
            uuid,
            sessionUuid,
            1L,
            StructureActionType.OPEN,
            AnimationType.MOVE_BLOCKS,
            Instant.now(),
            null,
            AnimationRunStatus.ACTIVE,
            3, // expectedAnimatedBlockCount
            recoveredBlockCount,
            Instant.now(),
            null,
            null
        );
    }

    private static PluginSession newPluginSession(UUID uuid)
    {
        return new PluginSession(
            uuid,
            Instant.now(),
            null,
            PluginSessionStatus.ACTIVE,
            null,
            "1.0.0",
            "1.21",
            "1.21",
            "Paper"
        );
    }
}
