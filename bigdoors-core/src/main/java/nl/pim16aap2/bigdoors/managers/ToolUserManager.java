package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolUserManager extends Restartable
{
    private static final ToolUserManager INSTANCE = new ToolUserManager(BigDoors.get().getPlatform());
    private static final Map<UUID, ToolUser> toolUsers = new ConcurrentHashMap<>();

    private ToolUserManager(final @NotNull IRestartableHolder holder)
    {
        super(holder);
    }

    public static ToolUserManager get()
    {
        return INSTANCE;
    }

    public void registerToolUser(ToolUser toolUser)
    {
        ToolUser result = toolUsers.putIfAbsent(toolUser.getPlayer().getUUID(), toolUser);
        if (result != null)
        {
            PLogger.get().info("Aborting previous ToolUser for user: " +
                                   toolUser.getPlayer().getName() + " (" + toolUser.getPlayer().getUUID() + ") " +
                                   "because a new ToolUser was initiated.");
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

    public Optional<ToolUser> getToolUser(final @NotNull IPPlayer player)
    {
        return getToolUser(player.getUUID());
    }

    public Optional<ToolUser> getToolUser(final @NotNull UUID uuid)
    {
        return Optional.ofNullable(toolUsers.get(uuid));
    }

    @Override
    public void restart()
    {
        Iterator<Map.Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().shutdown();
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
        final @Nullable ToolUser tu = toolUsers.remove(toolUser.getPlayer().getUUID());
        if (tu != null)
            tu.shutdown();
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
