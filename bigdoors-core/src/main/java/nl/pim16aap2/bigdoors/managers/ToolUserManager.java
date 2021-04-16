package nl.pim16aap2.bigdoors.managers;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.pair.Pair;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolUserManager extends Restartable
{
    private static final @NonNull Map<UUID, Pair<ToolUser, TimerTask>> toolUsers = new ConcurrentHashMap<>();

    public ToolUserManager(final @NonNull IRestartableHolder holder)
    {
        super(holder);
    }

    public void registerToolUser(final @NonNull ToolUser toolUser)
    {
        final @Nullable Pair<ToolUser, TimerTask> result =
            toolUsers.putIfAbsent(toolUser.getPlayer().getUUID(), new Pair<>(toolUser, null));

        if (result != null)
        {
            BigDoors.get().getPLogger().info("Aborting previous ToolUser for user: " +
                                                 toolUser.getPlayer().getName() + " (" +
                                                 toolUser.getPlayer().getUUID() + ") " +
                                                 "because a new ToolUser was initiated.");
            abortPair(toolUser.getPlayer().getUUID(), result);
        }
    }

    public boolean isToolUser(final @NonNull IPPlayer player)
    {
        return isToolUser(player.getUUID());
    }

    public boolean isToolUser(final @NonNull UUID uuid)
    {
        return toolUsers.containsKey(uuid);
    }

    public @NonNull Optional<ToolUser> getToolUser(final @NonNull IPPlayer player)
    {
        return getToolUser(player.getUUID());
    }

    public @NonNull Optional<ToolUser> getToolUser(final @NonNull UUID uuid)
    {
        return Optional.ofNullable(toolUsers.get(uuid)).map(pair -> pair.first);
    }

    @Override
    public void restart()
    {
        final @NonNull Iterator<Map.Entry<UUID, Pair<ToolUser, TimerTask>>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            final @NonNull Map.Entry<UUID, Pair<ToolUser, TimerTask>> entry = it.next();
            abortPair(entry.getKey(), entry.getValue());
        }

        if (toolUsers.size() != 0)
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalStateException("Failed to properly remove ToolUsers!"));
            toolUsers.forEach((uuid, pair) -> BigDoors.get().getPLogger().severe(uuid.toString()));
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
    public void abortToolUser(final @NonNull ToolUser toolUser)
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
    public void startToolUser(final @NonNull ToolUser toolUser, final int time)
    {
        final @Nullable Pair<ToolUser, TimerTask> pair = toolUsers.get(toolUser.getPlayer().getUUID());
        if (pair == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Trying to start a tool user even though it wasn't registered, somehow!"));
            return;
        }

        if (pair.second != null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException(
                    "Trying to create a timer for a tooluser even though it already has one! Aborting..."));
            abortToolUser(toolUser.getPlayer().getUUID());
            return;
        }

        final @NonNull TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (toolUser.isActive())
                    toolUser.getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                                             .getString(Message.CREATOR_GENERAL_TIMEOUT));
                toolUser.shutdown();
            }
        };

        pair.second = timerTask;
        BigDoors.get().getPlatform().getPExecutor().runSyncLater(timerTask, time);
    }

    /**
     * Stops and removes an {@link IPPlayer} if it is currently active. "Stop" here means that it will make sure to
     * properly clean up the {@link ToolUser}.
     *
     * @param player The {@link IPPlayer} whose {@link ToolUser} to stop and remove.
     */
    public void abortToolUser(final @NonNull IPPlayer player)
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
    public void abortToolUser(final @NonNull UUID playerUUID)
    {
        abortPair(playerUUID, toolUsers.get(playerUUID));
    }

    private void abortPair(final @NonNull UUID uuid, final @Nullable Pair<ToolUser, TimerTask> pair)
    {
        toolUsers.remove(uuid);

        if (pair == null)
            return;

        if (pair.first != null)
        {
            if (pair.first.isActive())
                pair.first.getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                                           .getString(Message.CREATOR_GENERAL_CANCELLED));
            pair.first.shutdown();
        }

        if (pair.second != null)
            pair.second.cancel();
    }
}
