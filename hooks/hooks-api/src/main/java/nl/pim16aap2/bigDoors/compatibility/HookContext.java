package nl.pim16aap2.bigDoors.compatibility;

import org.bukkit.plugin.java.JavaPlugin;

public final class HookContext
{
    private final JavaPlugin plugin;
    private final IProtectionCompatDefinition protectionCompatDefinition;
    private final IPermissionsManager permissionsManager;

    public HookContext(JavaPlugin plugin,
                       IProtectionCompatDefinition protectionCompatDefinition,
                       IPermissionsManager permissionsManager)
    {
        this.plugin = plugin;
        this.protectionCompatDefinition = protectionCompatDefinition;
        this.permissionsManager = permissionsManager;
    }

    public JavaPlugin getPlugin()
    {
        return plugin;
    }

    public IProtectionCompatDefinition getProtectionCompatDefinition()
    {
        return protectionCompatDefinition;
    }

    public IPermissionsManager getPermissionsManager()
    {
        return permissionsManager;
    }
}
