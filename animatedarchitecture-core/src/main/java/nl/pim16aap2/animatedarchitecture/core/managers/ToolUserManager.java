package nl.pim16aap2.animatedarchitecture.core.managers;

import com.google.common.flogger.StackSize;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a class that manages {@link ToolUser}s.
 */
@Singleton
@Flogger
public final class ToolUserManager extends Restartable
{
    private final Map<UUID, ToolUserEntry> toolUsers = new ConcurrentHashMap<>();
    private final IExecutor executor;

    @Inject
    ToolUserManager(
        RestartableHolder holder,
        IExecutor executor)
    {
        super(holder);
        this.executor = executor;
    }

    /**
     * Registers a {@link ToolUser} for a player.
     *
     * @param toolUser
     *     The {@link ToolUser} to register.
     */
    public void registerToolUser(ToolUser toolUser)
    {
        final @Nullable ToolUserEntry replaced =
            toolUsers.put(toolUser.getPlayer().getUUID(), new ToolUserEntry(toolUser, null));

        if (replaced != null)
        {
            log.atInfo().log("Aborting previous ToolUser for user: %s (%s) because a new ToolUser was initiated!",
                toolUser.getPlayer().getName(), toolUser.getPlayer().getUUID());
            abortEntry(replaced);
        }
    }

    /**
     * Checks if a player is currently a tool user. See {@link #isToolUser(UUID)}.
     *
     * @param player
     *     The player to check.
     * @return True if the player is a tool user.
     */
    @SuppressWarnings("unused")
    public boolean isToolUser(IPlayer player)
    {
        return isToolUser(player.getUUID());
    }

    /**
     * Checks if a player is currently a tool user. A player is a tool user if they have an active {@link ToolUser}
     * process.
     *
     * @param uuid
     *     The UUID of the player to check.
     * @return True if the player is a tool user.
     */
    public boolean isToolUser(UUID uuid)
    {
        return toolUsers.containsKey(uuid);
    }

    /**
     * Gets the {@link ToolUser} for a player. See {@link #getToolUser(UUID)}.
     *
     * @param player
     *     The player to get the {@link ToolUser} for.
     * @return The {@link ToolUser} for the player, if they are a tool user. Otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings("unused")
    public Optional<ToolUser> getToolUser(IPlayer player)
    {
        return getToolUser(player.getUUID());
    }

    /**
     * Gets the {@link ToolUser} for a player.
     *
     * @param uuid
     *     The UUID of the player.
     * @return The {@link ToolUser} for the player, if they are a tool user. Otherwise, an empty {@link Optional}.
     */
    public Optional<ToolUser> getToolUser(UUID uuid)
    {
        return Optional.ofNullable(toolUsers.get(uuid)).map(pair -> pair.toolUser);
    }

    /**
     * Cancels an active tool user process for a player.
     *
     * @param player
     *     The player whose tool user process to cancel.
     * @return True if a process was cancelled.
     */
    public boolean cancelToolUser(IPlayer player)
    {
        return cancelToolUser(player.getUUID());
    }

    /**
     * Cancels an active tool user process for a player.
     *
     * @param uuid
     *     The UUID of the player whose tool user process to cancel.
     * @return True if a process was cancelled.
     */
    public boolean cancelToolUser(UUID uuid)
    {
        final @Nullable ToolUserEntry removed = toolUsers.remove(uuid);
        if (removed == null)
            return false;
        removed.toolUser.abort();
        return true;
    }

    @Override
    public void shutDown()
    {
        final Iterator<Map.Entry<UUID, ToolUserEntry>> it = toolUsers.entrySet().iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext())
        {
            final Map.Entry<UUID, ToolUserEntry> entry = it.next();
            abortEntry(entry.getValue());
        }

        if (!toolUsers.isEmpty())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Failed to properly remove ToolUsers!");
            toolUsers.forEach((uuid, pair) -> log.atSevere().log("Failed to abort ToolUer for user: %s", uuid));
            toolUsers.clear();
        }
    }

    /**
     * Starts a timer for a {@link ToolUser}. After the provided number of seconds have passed, the {@link ToolUser}
     * will be aborted if this h
     *
     * @param toolUser
     *     The {@link ToolUser} for which to start the timer.
     * @param time
     *     The amount of time (in milliseconds).
     */
    public void startToolUser(ToolUser toolUser, int time)
    {
        final @Nullable ToolUserEntry pair = toolUsers.get(toolUser.getPlayer().getUUID());
        if (pair == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Trying to start a tool user even though it wasn't registered, somehow!");
            return;
        }

        if (pair.toolUser != toolUser)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Trying to start a tool user while another instance is already running! Aborting...");
            abortToolUser(toolUser);
            return;
        }

        if (pair.timerTask != null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Trying to create a timer for a tool user even though it already has one! Aborting...");
            abortToolUser(toolUser);
            return;
        }

        final TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (toolUser.isActive())
                    toolUser.getPlayer().sendError("creator.base.error.timed_out");
                toolUser.abort();
            }
        };

        pair.timerTask = timerTask;
        executor.runSyncLater(timerTask, time);
    }

    /**
     * Stops and removes a player (defined by their {@link UUID}) if it is currently active. "Stop" here means that it
     * will make sure to properly clean up the {@link ToolUser}.
     * <p>
     * If no {@link ToolUser} is active for the given player, nothing happens.
     *
     * @param playerUUID
     *     The {@link UUID} of the player whose {@link ToolUser} to stop and remove.
     */
    public void abortToolUser(UUID playerUUID)
    {
        abortEntry(toolUsers.remove(playerUUID));
    }

    /**
     * Stops and removes a {@link ToolUser}. "Stop" here means that it will make sure to properly clean up the
     * {@link ToolUser}.
     *
     * @param toolUser
     *     The {@link ToolUser} to stop and remove.
     */
    public void abortToolUser(ToolUser toolUser)
    {
        final AtomicReference<@Nullable ToolUserEntry> removed = new AtomicReference<>(null);
        toolUsers.computeIfPresent(toolUser.getPlayer().getUUID(), (uuid, entry) ->
        {
            // Make sure we remove the specific ToolUser instance we want to remove, and not some other
            // ToolUser that might have been registered in the meantime.
            if (entry.toolUser != toolUser)
                return entry;

            removed.set(entry);
            return null;
        });

        final @Nullable ToolUserEntry removedEntry = removed.get();
        if (removedEntry == null)
            abortEntry(toolUser, null);
        else
            abortEntry(removedEntry);
    }

    /**
     * Aborts a {@link ToolUserEntry}.
     * <p>
     * See {@link #abortEntry(ToolUser, TimerTask)}.
     *
     * @param entry
     *     The {@link ToolUserEntry} to abort. If the entry is null, nothing happens.
     */
    private void abortEntry(@Nullable ToolUserEntry entry)
    {
        if (entry == null)
            return;
        abortEntry(entry.toolUser, entry.timerTask);
    }

    /**
     * Aborts a {@link ToolUser} process.
     * <p>
     * If the {@link ToolUser} is active, the player will be notified that the process was cancelled.
     * <p>
     * If a {@link TimerTask} is provided, it will be cancelled.
     *
     * @param toolUser
     *     The {@link ToolUser} to abort.
     * @param timerTask
     *     The {@link TimerTask} to cancel.
     */
    private void abortEntry(ToolUser toolUser, @Nullable TimerTask timerTask)
    {
        if (toolUser.isActive())
            toolUser.getPlayer().sendError("creator.base.error.creation_cancelled");

        if (timerTask != null)
            timerTask.cancel();

        toolUser.abort();
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    private static final class ToolUserEntry
    {
        @Getter
        private final ToolUser toolUser;

        @Getter
        private @Nullable TimerTask timerTask;
    }
}
