package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tracks the lifecycle of the current AnimatedArchitecture plugin session.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
public final class PluginSessionManager extends Restartable implements IDebuggable
{
    private static final String CLEAN_SHUTDOWN_REASON = "plugin disabled cleanly";
    private static final Duration COMPLETED_ANIMATION_RUN_RETENTION = Duration.ofHours(24);

    private final DatabaseManager databaseManager;
    private final IPluginSessionMetadataProvider metadataProvider;

    private volatile @Nullable UUID currentSessionUuid;

    /**
     * Creates a plugin session manager.
     *
     * @param restartableHolder
     *     The restartable holder.
     * @param databaseManager
     *     The database manager used to persist session state.
     * @param metadataProvider
     *     The provider for session metadata.
     * @param debuggableRegistry
     *     The debuggable registry.
     */
    @Inject
    public PluginSessionManager(
        RestartableHolder restartableHolder,
        DatabaseManager databaseManager,
        IPluginSessionMetadataProvider metadataProvider,
        DebuggableRegistry debuggableRegistry)
    {
        super(restartableHolder);

        this.databaseManager = databaseManager;
        this.metadataProvider = metadataProvider;

        debuggableRegistry.registerDebuggable(this);
    }

    private void markOldOpenSessionsAsUnclean(Instant timestamp)
    {
        databaseManager
            .markActivePluginSessionsUnclean(timestamp)
            .orTimeout(5, TimeUnit.SECONDS)
            .thenAccept(uncleanCount ->
            {
                if (uncleanCount > 0)
                {
                    log.atWarn().log(
                        "Marked %d previous plugin session(s) as unclean due to active sessions at startup.",
                        uncleanCount
                    );
                }
            })
            .handleExceptional(ex -> log.atError().withCause(ex).log("Failed to close old sessions!"));
    }

    @Override
    public synchronized void initialize()
    {
        markOldOpenSessionsAsUnclean(Instant.now());

        final PluginSessionMetadata metadata = metadataProvider.getMetadata();

        final UUID uuid = UuidCreator.getTimeOrderedEpoch();
        final PluginSession pluginSession = databaseManager
            .startPluginSession(
                uuid,
                Instant.now(),
                metadata.pluginVersion(),
                metadata.serverVersion(),
                metadata.minecraftVersion(),
                metadata.serverSoftware())
            .orTimeout(10, TimeUnit.SECONDS)
            .join()
            .orElseThrow(() -> new IllegalStateException("Failed to create plugin session: " + uuid));

        currentSessionUuid = pluginSession.uuid();
        deleteOldCompletedAnimationRuns();
    }

    private void deleteOldCompletedAnimationRuns()
    {
        databaseManager
            .deleteCompletedAnimationRuns(Instant.now().minus(COMPLETED_ANIMATION_RUN_RETENTION))
            .orTimeout(5, TimeUnit.SECONDS)
            .thenAccept(deleted ->
                {
                    if (deleted > 0)
                    {
                        log.atInfo().log("Deleted %d old completed animation run(s).", deleted);
                    }
                }
            ).handleExceptional(ex ->
                log.atError().withCause(ex).log("Failed to delete old completed animation runs.")
            );
    }

    @Override
    public synchronized void shutDown()
    {
        final UUID uuid = currentSessionUuid;
        if (uuid == null)
        {
            throw new IllegalStateException("No active plugin session to close.");
        }

        currentSessionUuid = null;

        databaseManager
            .closePluginSession(uuid, Instant.now(), CLEAN_SHUTDOWN_REASON)
            .orTimeout(5, TimeUnit.SECONDS)
            .thenAccept(closed ->
            {
                if (!closed)
                {
                    log.atWarn().log("Failed to close plugin session cleanly: %s", uuid);
                }
            })
            .join();
    }

    /**
     * Gets the current session UUID.
     *
     * @return The current session UUID.
     */
    public Optional<UUID> getCurrentSessionUuid()
    {
        return Optional.ofNullable(currentSessionUuid);
    }

    @Override
    public String getDebugInformation()
    {
        return String.format("""
                CurrentSessionUuid: %s
                CompletedAnimationRunRetention: %s
                """,
            currentSessionUuid,
            COMPLETED_ANIMATION_RUN_RETENTION
        );
    }
}
