package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import com.google.common.flogger.StackSize;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IFakePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;
import java.util.Set;

/**
 * Provides shared Spigot permission behavior that is independent of the selected permission backend.
 */
@CustomLog
abstract class AbstractPermissionsManagerSpigot implements IPermissionsManagerSpigot
{
    /**
     * Checks that synchronous backend permission checks are only performed for online real players.
     *
     * @param executor
     *     The executor instance to use for thread verification.
     * @param player
     *     The player to validate.
     * @param permission
     *     The permission node being checked.
     */
    protected final void validateSynchronousPlayer(IExecutor executor, Player player, String permission)
    {
        if (executor.isMainThread() &&
            (player instanceof IFakePlayer || !player.isOnline()))
        {
            throw new RuntimeException(String.format(
                "Failed to check permission '%s' for player '%s'! " +
                    "Cannot check permissions for offline players on the main thread! Online: %b, Fake: %b",
                permission,
                player.getName(),
                player.isOnline(),
                player instanceof IFakePlayer)
            );
        }
    }

    @Override
    public OptionalInt getMaxPermissionSuffix(Player player, String permissionBase)
    {
        final int permissionBaseLength = permissionBase.length();
        final Set<PermissionAttachmentInfo> playerPermissions = player.getEffectivePermissions();
        int ret = -1;
        for (final PermissionAttachmentInfo permission : playerPermissions)
        {
            if (permission.getPermission().startsWith(permissionBase))
            {
                final OptionalInt suffix = MathUtil.parseInt(permission
                    .getPermission()
                    .substring(permissionBaseLength));
                if (suffix.isPresent())
                    ret = Math.max(ret, suffix.getAsInt());
            }
        }
        return ret > 0 ? OptionalInt.of(ret) : OptionalInt.empty();
    }

    @Override
    public OptionalInt getMaxPermissionSuffix(IPlayer player, String permissionBase)
    {
        final Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            return OptionalInt.empty();
        }
        return getMaxPermissionSuffix(bukkitPlayer, permissionBase);
    }

    @Override
    public boolean hasPermission(IPlayer player, String permissionNode)
    {
        final Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            return false;
        }

        return hasPermission(bukkitPlayer, permissionNode);
    }

    @Override
    public boolean hasBypassPermissionsForAttribute(Player player, StructureAttribute structureAttribute)
    {
        return player.isOp() || hasPermission(player, structureAttribute.getAdminPermissionNode());
    }

    @Override
    public boolean hasBypassPermissionsForAttribute(IPlayer player, StructureAttribute structureAttribute)
    {
        final Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            return false;
        }

        return hasBypassPermissionsForAttribute(bukkitPlayer, structureAttribute);
    }

    @Override
    public boolean isOp(@Nullable Player player)
    {
        return player != null && player.isOp();
    }

    @Override
    public boolean isOp(IPlayer player)
    {
        return isOp(getBukkitPlayer(player));
    }

    @Override
    public boolean hasPermissionToCreateStructure(ICommandSender sender, StructureType type)
    {
        return sender
            .getPlayer()
            .map(player -> hasPermission(player, type.getCreationPermission()))
            .orElse(true);
    }

    private @Nullable Player getBukkitPlayer(IPlayer player)
    {
        final Player bukkitPlayer = PlayerFactorySpigot.unwrapPlayer(player);
        if (bukkitPlayer == null)
        {
            log.atError().withStackTrace(StackSize.FULL).log(
                "Failed to obtain BukkitPlayer for player: '%s'",
                player.asString()
            );
            return null;
        }
        return bukkitPlayer;
    }
}
