package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import com.google.common.flogger.StackSize;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
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

    @Override
    public synchronized void initialize()
    {
        final Instant now = Instant.now();
        final PluginSessionMetadata metadata = metadataProvider.getMetadata();
        final int uncleanCount = databaseManager
            .markActivePluginSessionsUnclean(now)
            .orTimeout(5, TimeUnit.SECONDS)
            .join();

        if (uncleanCount > 0)
            log.atWarn().log("Marked %d previous plugin session(s) as unclean.", uncleanCount);

        final UUID uuid = UuidCreator.getTimeOrderedEpoch();
        final Optional<PluginSession> session = databaseManager
            .startPluginSession(
                uuid,
                now,
                metadata.pluginVersion(),
                metadata.serverVersion(),
                metadata.minecraftVersion(),
                metadata.serverSoftware())
            .orTimeout(5, TimeUnit.SECONDS)
            .join();

        if (session.isEmpty())
            throw new IllegalStateException("Failed to create plugin session: " + uuid);

        currentSessionUuid = uuid;
        deleteOldCompletedAnimationRuns();
    }

    private void deleteOldCompletedAnimationRuns()
    {
        final int count = databaseManager
            .deleteCompletedAnimationRuns(Instant.now().minus(COMPLETED_ANIMATION_RUN_RETENTION))
            .orTimeout(5, TimeUnit.SECONDS)
            .join();
        if (count > 0)
            log.atInfo().log("Deleted %d old completed animation run(s).", count);
    }

    @Override
    public synchronized void shutDown()
    {
        final UUID uuid = currentSessionUuid;
        if (uuid == null)
        {
            log.atWarn().withStackTrace(StackSize.FULL).log("No active plugin session to close.");
            return;
        }

        final boolean closed = databaseManager
            .closePluginSession(uuid, Instant.now(), CLEAN_SHUTDOWN_REASON)
            .orTimeout(5, TimeUnit.SECONDS)
            .join();

        if (!closed)
            log.atWarn().log("Failed to close plugin session cleanly: %s", uuid);
        currentSessionUuid = null;
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
        return String.format(
            """
                CurrentSessionUuid: %s
                CompletedAnimationRunRetention: %s
                """,
            currentSessionUuid,
            COMPLETED_ANIMATION_RUN_RETENTION
        );
    }
}
