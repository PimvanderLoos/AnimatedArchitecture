package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolUserManager
{
    private static final ToolUserManager INSTANCE = new ToolUserManager();
    private static final Map<UUID, ToolUser> toolUsers = new ConcurrentHashMap<>();

    private ToolUserManager()
    {

    }

    public static ToolUserManager get()
    {
        return INSTANCE;
    }

    public <T extends ToolUser<T>> void registerToolUser(ToolUser<T> toolUser)
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
}
