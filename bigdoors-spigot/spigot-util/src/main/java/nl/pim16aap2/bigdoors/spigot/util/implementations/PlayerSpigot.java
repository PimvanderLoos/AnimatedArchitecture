package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.core.commands.PermissionsStatus;
import nl.pim16aap2.bigdoors.core.text.Text;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPlayer} for the Spigot platform.
 *
 * @author Pim
 */
@Flogger
public final class PlayerSpigot implements IPlayer
{
    private final Player spigotPlayer;

    public PlayerSpigot(Player spigotPlayer)
    {
        this.spigotPlayer = spigotPlayer;
    }

    @Override
    public UUID getUUID()
    {
        return spigotPlayer.getUniqueId();
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return spigotPlayer.isOp() || spigotPlayer.hasPermission("bigdoors.admin.bypass_protection_plugins");
    }

    @Override
    public Optional<ILocation> getLocation()
    {
        return Optional.of(SpigotAdapter.wrapLocation(spigotPlayer.getLocation()));
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(String permission)
    {
        return CompletableFuture.completedFuture(spigotPlayer.hasPermission(permission));
    }

    @Override
    public CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new PermissionsStatus(
            command.getUserPermission().map(spigotPlayer::hasPermission).orElse(false),
            command.getAdminPermission().map(spigotPlayer::hasPermission).orElse(false)));
    }

    @Override
    public boolean isOnline()
    {
        return true;
    }

    @Override
    public int getStructureSizeLimit()
    {
        if (spigotPlayer.isOp())
            return Integer.MAX_VALUE;
        return SpigotUtil.getHighestPermissionSuffix(spigotPlayer, "bigdoors.door_size_limit.");
    }

    @Override
    public int getStructureCountLimit()
    {
        if (spigotPlayer.isOp())
            return Integer.MAX_VALUE;
        return SpigotUtil.getHighestPermissionSuffix(spigotPlayer, "bigdoors.doors_owned_limit.");
    }

    @Override
    public boolean isOp()
    {
        return spigotPlayer.isOp();
    }

    @Override
    public String getName()
    {
        return spigotPlayer.getName();
    }

    @Override
    public void sendMessage(Text text)
    {
        spigotPlayer.sendMessage(text.toString());
    }

    /**
     * Gets the bukkit player represented by this {@link IPlayer}
     *
     * @return The Bukkit player.
     */
    public Player getBukkitPlayer()
    {
        return spigotPlayer;
    }

    @Override
    public String toString()
    {
        return asString();
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        return getUUID().equals(((PlayerSpigot) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }
}
