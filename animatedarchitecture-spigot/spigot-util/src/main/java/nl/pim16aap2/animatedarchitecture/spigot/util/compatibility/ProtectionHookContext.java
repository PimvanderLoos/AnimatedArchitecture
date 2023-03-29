package nl.pim16aap2.animatedarchitecture.spigot.util.compatibility;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class contains the context for a protection hook.
 */
public final class ProtectionHookContext
{
    private final JavaPlugin plugin;
    private final IProtectionHookSpigotSpecification specification;
    private final IPermissionsManagerSpigot permissionsManager;

    public ProtectionHookContext(
        JavaPlugin plugin,
        IProtectionHookSpigotSpecification specification,
        IPermissionsManagerSpigot permissionsManager)
    {
        this.plugin = plugin;
        this.specification = specification;
        this.permissionsManager = permissionsManager;
    }

    public JavaPlugin getPlugin()
    {
        return plugin;
    }

    public IProtectionHookSpigotSpecification getSpecification()
    {
        return specification;
    }

    public IPermissionsManagerSpigot getPermissionsManager()
    {
        return permissionsManager;
    }
}
