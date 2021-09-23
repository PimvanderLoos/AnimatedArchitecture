package nl.pim16aap2.bigdoors.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.pair.PairNullable;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Singleton
@Flogger
public final class ToolUserManager extends Restartable
{
    private final Map<UUID, PairNullable<ToolUser, TimerTask>> toolUsers = new ConcurrentHashMap<>();
    private final ILocalizer localizer;
    private final IPExecutor executor;

    @Inject
    public ToolUserManager(RestartableHolder holder, ILocalizer localizer, IPExecutor executor)
    {
        super(holder);
        this.localizer = localizer;
        this.executor = executor;
    }

    public void registerToolUser(ToolUser toolUser)
    {
        final @Nullable PairNullable<ToolUser, TimerTask> result =
            toolUsers.put(toolUser.getPlayer().getUUID(), new PairNullable<>(toolUser, null));

        if (result != null)
        {
            log.at(Level.INFO).log("Aborting previous ToolUser for user: %s (%s) because a new ToolUser was initiated!",
                                   toolUser.getPlayer().getName(), toolUser.getPlayer().getUUID());
            abortPair(toolUser.getPlayer().getUUID(), result);
        }
    }

    @SuppressWarnings("unused")
    public boolean isToolUser(IPPlayer player)
    {
        return isToolUser(player.getUUID());
    }

    public boolean isToolUser(UUID uuid)
    {
        return toolUsers.containsKey(uuid);
    }

    @SuppressWarnings("unused")
    public Optional<ToolUser> getToolUser(IPPlayer player)
    {
        return getToolUser(player.getUUID());
    }

    public Optional<ToolUser> getToolUser(UUID uuid)
    {
        return Optional.ofNullable(toolUsers.get(uuid)).map(pair -> pair.first);
    }

    @Override
    public void restart()
    {
        final Iterator<Map.Entry<UUID, PairNullable<ToolUser, TimerTask>>> it = toolUsers.entrySet().iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext())
        {
            final Map.Entry<UUID, PairNullable<ToolUser, TimerTask>> entry = it.next();
            abortPair(entry.getKey(), entry.getValue());
        }

        if (!toolUsers.isEmpty())
        {
            log.at(Level.SEVERE).withCause(new IllegalStateException("Failed to properly remove ToolUsers!")).log();
            toolUsers.forEach((uuid, pair) -> log.at(Level.SEVERE)
                                                 .log("Failed to abort ToolUer for user: %s", uuid.toString()));
            toolUsers.clear();
        }
    }

    @Override
    public void shutdown()
    {
        restart();
    }

    /**
     * Stops and removes a {@link ToolUser}. "Stop" here means that it will make sure to properly clean up the {@link
     * ToolUser}.
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
     *     The amount of time (in seconds).
     */
    public void startToolUser(ToolUser toolUser, int time)
    {
        final @Nullable PairNullable<ToolUser, TimerTask> pair = toolUsers.get(toolUser.getPlayer().getUUID());
        if (pair == null)
        {
            log.at(Level.SEVERE).withCause(
                   new IllegalStateException("Trying to start a tool user even though it wasn't registered, somehow!"))
               .log();
            return;
        }

        if (pair.second != null)
        {
            log.at(Level.SEVERE).withCause(
                new IllegalStateException(
                    "Trying to create a timer for a tool user even though it already has one! Aborting...")).log();
            abortToolUser(toolUser.getPlayer().getUUID());
            return;
        }

        final TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (toolUser.isActive())
                    toolUser.getPlayer().sendMessage(localizer.getMessage("creator.base.error.timed_out"));
                toolUser.shutdown();
            }
        };

        pair.second = timerTask;
        executor.runSyncLater(timerTask, time);
    }

    /**
     * Stops and removes an {@link IPPlayer} if it is currently active. "Stop" here means that it will make sure to
     * properly clean up the {@link ToolUser}.
     *
     * @param player
     *     The {@link IPPlayer} whose {@link ToolUser} to stop and remove.
     */
    @SuppressWarnings("unused")
    public void abortToolUser(IPPlayer player)
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

    private void abortPair(UUID uuid, @Nullable PairNullable<ToolUser, TimerTask> pair)
    {
        toolUsers.remove(uuid);

        if (pair == null)
            return;

        if (pair.first != null)
        {
            if (pair.first.isActive())
                pair.first.getPlayer().sendMessage(localizer.getMessage("creator.base.error.creation_cancelled"));
            pair.first.shutdown();
        }

        if (pair.second != null)
            pair.second.cancel();
    }
}
