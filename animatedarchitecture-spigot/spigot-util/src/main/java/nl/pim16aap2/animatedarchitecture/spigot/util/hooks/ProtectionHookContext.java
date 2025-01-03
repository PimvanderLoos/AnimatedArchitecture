package nl.pim16aap2.animatedarchitecture.spigot.util.hooks;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class contains the context for a protection hook.
 */
@Getter
public final class ProtectionHookContext
{
    private final JavaPlugin animatedArchitecturePlugin;
    private final IProtectionHookSpigotSpecification specification;
    private final IPermissionsManagerSpigot permissionsManager;
    private final IExecutor executor;

    public ProtectionHookContext(
        JavaPlugin animatedArchitecturePlugin,
        IProtectionHookSpigotSpecification specification,
        IPermissionsManagerSpigot permissionsManager,
        IExecutor executor)
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.specification = specification;
        this.permissionsManager = permissionsManager;
        this.executor = executor;
    }
}
