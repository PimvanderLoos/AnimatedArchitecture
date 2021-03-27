package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.Pair;
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

    public static @NotNull ToolUserManager get()
    {
        return INSTANCE;
    }

    public void registerToolUser(final @NotNull ToolUser toolUser)
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

    public boolean isToolUser(final @NotNull IPPlayer player)
    {
        return isToolUser(player.getUUID());
    }

    public boolean isToolUser(final @NotNull UUID uuid)
    {
        return toolUsers.containsKey(uuid);
    }

    public @NotNull Optional<ToolUser> getToolUser(final @NotNull IPPlayer player)
    {
        return getToolUser(player.getUUID());
    }

    public @NotNull Optional<ToolUser> getToolUser(final @NotNull UUID uuid)
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

        final @NotNull TimerTask timerTask = new TimerTask()
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
        abortPair(playerUUID, toolUsers.get(playerUUID));
    }

    private void abortPair(final @NotNull UUID uuid, final @Nullable Pair<ToolUser, TimerTask> pair)
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
