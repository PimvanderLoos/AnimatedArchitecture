package nl.pim16aap2.animatedarchitecture.core.managers;

import com.google.common.flogger.StackSize;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
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

@Singleton
@Flogger
public final class ToolUserManager extends Restartable
{
    private final Map<UUID, ToolUserEntry> toolUsers = new ConcurrentHashMap<>();
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final IExecutor executor;

    @Inject
    public ToolUserManager(
        RestartableHolder holder,
        ILocalizer localizer,
        ITextFactory textFactory,
        IExecutor executor)
    {
        super(holder);
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.executor = executor;
    }

    public void registerToolUser(ToolUser toolUser)
    {
        final @Nullable ToolUserEntry result =
            toolUsers.put(toolUser.getPlayer().getUUID(), new ToolUserEntry(toolUser, null));

        if (result != null)
        {
            log.atInfo().log("Aborting previous ToolUser for user: %s (%s) because a new ToolUser was initiated!",
                             toolUser.getPlayer().getName(), toolUser.getPlayer().getUUID());
            abortPair(toolUser.getPlayer().getUUID(), result);
        }
    }

    @SuppressWarnings("unused")
    public boolean isToolUser(IPlayer player)
    {
        return isToolUser(player.getUUID());
    }

    public boolean isToolUser(UUID uuid)
    {
        return toolUsers.containsKey(uuid);
    }

    @SuppressWarnings("unused")
    public Optional<ToolUser> getToolUser(IPlayer player)
    {
        return getToolUser(player.getUUID());
    }

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
            abortPair(entry.getKey(), entry.getValue());
        }

        if (!toolUsers.isEmpty())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Failed to properly remove ToolUsers!");
            toolUsers.forEach((uuid, pair) -> log.atSevere().log("Failed to abort ToolUer for user: %s", uuid));
            toolUsers.clear();
        }
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
        abortToolUser(toolUser.getPlayer().getUUID());
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
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Trying to start a tool user even though it wasn't registered, somehow!");
            return;
        }

        if (pair.timerTask != null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Trying to create a timer for a tool user even though it already has one! Aborting...");
            abortToolUser(toolUser.getPlayer().getUUID());
            return;
        }

        final TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (toolUser.isActive())
                    toolUser.getPlayer().sendMessage(textFactory, TextType.ERROR,
                                                     localizer.getMessage("creator.base.error.timed_out"));
                toolUser.abort();
            }
        };

        pair.timerTask = timerTask;
        executor.runSyncLater(timerTask, time);
    }

    /**
     * Stops and removes an {@link IPlayer} if it is currently active. "Stop" here means that it will make sure to
     * properly clean up the {@link ToolUser}.
     *
     * @param player
     *     The {@link IPlayer} whose {@link ToolUser} to stop and remove.
     */
    @SuppressWarnings("unused")
    public void abortToolUser(IPlayer player)
    {
        abortToolUser(player.getUUID());
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
        abortPair(playerUUID, toolUsers.get(playerUUID));
    }

    private void abortPair(UUID uuid, @Nullable ToolUserEntry pair)
    {
        toolUsers.remove(uuid);

        if (pair == null)
            return;

        if (pair.toolUser.isActive())
            pair.toolUser.getPlayer()
                         .sendError(textFactory, localizer.getMessage("creator.base.error.creation_cancelled"));
        pair.toolUser.abort();

        if (pair.timerTask != null)
            pair.timerTask.cancel();
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    private static final class ToolUserEntry
    {
        @Getter
        private ToolUser toolUser;
        @Getter
        private @Nullable TimerTask timerTask;
    }
}
