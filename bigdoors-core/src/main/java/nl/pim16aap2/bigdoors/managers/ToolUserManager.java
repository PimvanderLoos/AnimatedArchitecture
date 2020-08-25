package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Pair;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolUserManager extends Restartable
{
    @NotNull
    private static final ToolUserManager INSTANCE = new ToolUserManager(BigDoors.get().getPlatform());
    @NotNull
    private static final Map<UUID, Pair<ToolUser, TimerTask>> toolUsers = new ConcurrentHashMap<>();

    private ToolUserManager(final @NotNull IRestartableHolder holder)
    {
        super(holder);
    }

    @NotNull
    public static ToolUserManager get()
    {
        return INSTANCE;
    }

    public void registerToolUser(final @NotNull ToolUser toolUser)
    {
        final @Nullable Pair<ToolUser, TimerTask> result =
            toolUsers.putIfAbsent(toolUser.getPlayer().getUUID(), new Pair<>(toolUser, null));

        if (result != null)
        {
            PLogger.get().info("Aborting previous ToolUser for user: " +
                                   toolUser.getPlayer().getName() + " (" + toolUser.getPlayer().getUUID() + ") " +
                                   "because a new ToolUser was initiated.");
            abortPair(result);
        }
    }

    public boolean isToolUser(final @NotNull IPPlayer player)
    {
        return isToolUser(player.getUUID());
    }

    public boolean isToolUser(final @NotNull UUID uuid)
    {
        return toolUsers.containsKey(uuid);
    }

    @NotNull
    public Optional<ToolUser> getToolUser(final @NotNull IPPlayer player)
    {
        return getToolUser(player.getUUID());
    }

    @NotNull
    public Optional<ToolUser> getToolUser(final @NotNull UUID uuid)
    {
        return Optional.ofNullable(toolUsers.get(uuid)).map(pair -> pair.first);
    }

    @Override
    public void restart()
    {
        final @NotNull Iterator<Map.Entry<UUID, Pair<ToolUser, TimerTask>>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            final @NotNull Map.Entry<UUID, Pair<ToolUser, TimerTask>> entry = it.next();
            abortPair(entry.getValue());
        }

        if (toolUsers.size() != 0)
        {
            PLogger.get().logException(new IllegalStateException("Failed to properly remove ToolUsers!"));
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
     * @param toolUser The {@link ToolUser} to stop and remove.
     */
    public void abortToolUser(final @NotNull ToolUser toolUser)
    {
        abortToolUser(toolUser.getPlayer().getUUID());
    }

    /**
     * Starts a timer for a {@link ToolUser}. After the provided number of seconds have passed, the {@link ToolUser}
     * will be aborted if this h
     *
     * @param toolUser The {@link ToolUser} for which to start the timer.
     * @param time     The amount of time (in seconds).
     */
    public void startToolUser(final @NotNull ToolUser toolUser, final int time)
    {
        final @Nullable Pair<ToolUser, TimerTask> pair = toolUsers.get(toolUser.getPlayer().getUUID());
        if (pair == null)
        {
            PLogger.get().logException(
                new IllegalStateException("Trying to start a tool user even though it wasn't registered, somehow!"));
            return;
        }
        if (pair.second != null)
        {
            PLogger.get().logException(
                new IllegalStateException(
                    "Trying to create a timer for a tooluser even though it already has one! Aborting..."));
            abortPair(toolUsers.get(toolUser.getPlayer().getUUID()));
            return;
        }

        final @NotNull TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                System.out.println("ABORTING TASK!!");
                toolUser.shutdown();
            }
        };

        pair.second = timerTask;
        BigDoors.get().getPlatform().newPExecutor().runSyncLater(timerTask, time);
    }

    /**
     * Stops and removes an {@link IPPlayer} if it is currently active. "Stop" here means that it will make sure to
     * properly clean up the {@link ToolUser}.
     *
     * @param player The {@link IPPlayer} whose {@link ToolUser} to stop and remove.
     */
    public void abortToolUser(final @NotNull IPPlayer player)
    {
        abortToolUser(player.getUUID());
    }

    /**
     * Stops and removes a player (defined by their {@link UUID}) if it is currently active. "Stop" here means that it
     * will make sure to properly clean up the {@link ToolUser}.
     * <p>
     * If no {@link ToolUser} is active for the given player, nothing happens.
     *
     * @param playerUUID The {@link UUID} of the player whose {@link ToolUser} to stop and remove.
     */
    public void abortToolUser(final @NotNull UUID playerUUID)
    {
        abortPair(toolUsers.get(playerUUID));
    }

    private void abortPair(final @Nullable Pair<ToolUser, TimerTask> pair)
    {
        if (pair == null)
            return;

        if (pair.first != null)
        {
            pair.first.getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                                       .getString(Message.CREATOR_GENERAL_CANCELLED));
            pair.first.shutdown();
        }

        if (pair.second != null)
            pair.second.cancel();
    }

    /**
     * Removes a {@link ToolUser} from the registered list. Note that it does not shut down the {@link ToolUser}! If you
     * want to properly stop the {@link ToolUser}, you should use {@link #abortToolUser(ToolUser)} instead.
     *
     * @param toolUser The {@link ToolUser} to remove.
     */
    public void removeToolUser(final @NotNull ToolUser toolUser)
    {
        toolUsers.remove(toolUser.getPlayer().getUUID());
    }
}
