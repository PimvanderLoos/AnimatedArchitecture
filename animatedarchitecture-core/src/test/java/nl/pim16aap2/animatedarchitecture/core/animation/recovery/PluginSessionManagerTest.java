package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(5)
@ExtendWith(MockitoExtension.class)
@WithLogCapture
class PluginSessionManagerTest
{
    @Mock
    private RestartableHolder restartableHolder;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private IPluginSessionMetadataProvider metadataProvider;

    @Mock
    private DebuggableRegistry debuggableRegistry;

    private PluginSessionManager pluginSessionManager;

    @BeforeEach
    void setUp()
    {
        pluginSessionManager = new PluginSessionManager(
            restartableHolder,
            databaseManager,
            metadataProvider,
            debuggableRegistry
        );
    }

    @Test
    void constructor_shouldRegisterWithRestartableHolderAndDebuggableRegistry()
    {
        // verify
        verify(restartableHolder).registerRestartable(pluginSessionManager);
        verify(debuggableRegistry).registerDebuggable(pluginSessionManager);
    }

    @Test
    void getCurrentSessionUuid_shouldBeEmptyBeforeInitialization()
    {
        // execute & verify
        assertThat(pluginSessionManager.getCurrentSessionUuid()).isEmpty();
    }

    @Test
    void initialize_shouldCreateNewSession()
    {
        // setup
        stubSuccessfulInitialize();

        // execute
        pluginSessionManager.initialize();

        // verify
        assertThat(pluginSessionManager.getCurrentSessionUuid()).isPresent();
    }

    @Test
    void initialize_shouldMarkOldSessionsAsUnclean()
    {
        // setup
        stubSuccessfulInitialize();

        // execute
        pluginSessionManager.initialize();

        // verify
        verify(databaseManager).markActivePluginSessionsUnclean(any());
    }

    @Test
    void initialize_shouldDeleteOldCompletedAnimationRuns()
    {
        // setup
        stubSuccessfulInitialize();

        // execute
        pluginSessionManager.initialize();

        // verify
        verify(databaseManager).deleteCompletedAnimationRuns(any());
    }

    @Test
    void initialize_shouldLogWarningWhenOldSessionsWereFound(LogCaptor logCaptor)
    {
        // setup
        stubSuccessfulInitialize();
        when(databaseManager.markActivePluginSessionsUnclean(any()))
            .thenReturn(CompletableFuture.completedFuture(3));

        // execute
        pluginSessionManager.initialize();

        // verify
        assertThatLogCaptor(logCaptor)
            .atWarn()
            .singleWithMessageContaining("Marked 3 previous plugin session(s) as unclean");
    }

    @Test
    void initialize_shouldLogInfoWhenOldAnimationRunsWereDeleted(LogCaptor logCaptor)
    {
        // setup
        stubSuccessfulInitialize();
        when(databaseManager.deleteCompletedAnimationRuns(any()))
            .thenReturn(CompletableFuture.completedFuture(5));

        // execute
        pluginSessionManager.initialize();

        // verify
        assertThatLogCaptor(logCaptor)
            .atInfo()
            .singleWithMessageContaining("Deleted 5 old completed animation run(s)");
    }

    @Test
    void initialize_shouldThrowWhenSessionCreationFails()
    {
        // setup
        when(databaseManager.markActivePluginSessionsUnclean(any()))
            .thenReturn(CompletableFuture.completedFuture(0));
        when(metadataProvider.getMetadata()).thenReturn(newPluginSessionMetadata());
        when(databaseManager.startPluginSession(any(), any(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // execute & verify
        assertThatThrownBy(() -> pluginSessionManager.initialize())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to create plugin session");
    }

    @Test
    void shutDown_shouldCloseActiveSession()
    {
        // setup
        stubSuccessfulInitialize();
        when(databaseManager.closePluginSession(any(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(true));

        pluginSessionManager.initialize();

        // execute
        pluginSessionManager.shutDown();

        // verify
        verify(databaseManager).closePluginSession(any(), any(), anyString());
        assertThat(pluginSessionManager.getCurrentSessionUuid()).isEmpty();
    }

    @Test
    void shutDown_shouldLogWarningWhenSessionCloseFails(LogCaptor logCaptor)
    {
        // setup
        stubSuccessfulInitialize();
        when(databaseManager.closePluginSession(any(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(false));

        pluginSessionManager.initialize();

        // execute
        pluginSessionManager.shutDown();

        // verify
        assertThatLogCaptor(logCaptor)
            .atWarn()
            .singleWithMessageContaining("Failed to close plugin session cleanly");
    }

    @Test
    void shutDown_shouldThrowWhenNoActiveSession()
    {
        // execute & verify
        assertThatThrownBy(() -> pluginSessionManager.shutDown())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No active plugin session to close");
    }

    @Test
    void getDebugInformation_shouldIncludeSessionUuidAndRetention()
    {
        // execute & verify
        final String debugInfo = pluginSessionManager.getDebugInformation();

        assertThat(debugInfo)
            .contains("CurrentSessionUuid")
            .contains("CompletedAnimationRunRetention");
    }

    private void stubSuccessfulInitialize()
    {
        final PluginSession session = newPluginSession(UUID.randomUUID());
        when(databaseManager.markActivePluginSessionsUnclean(any()))
            .thenReturn(CompletableFuture.completedFuture(0));
        when(databaseManager.deleteCompletedAnimationRuns(any()))
            .thenReturn(CompletableFuture.completedFuture(0));
        when(metadataProvider.getMetadata()).thenReturn(newPluginSessionMetadata());
        when(databaseManager.startPluginSession(any(), any(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));
    }

    private static PluginSessionMetadata newPluginSessionMetadata()
    {
        return PluginSessionMetadata.builder()
            .pluginVersion("1.0.0")
            .serverVersion("1.21")
            .minecraftVersion("1.21")
            .serverSoftware("Paper")
            .build();
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
