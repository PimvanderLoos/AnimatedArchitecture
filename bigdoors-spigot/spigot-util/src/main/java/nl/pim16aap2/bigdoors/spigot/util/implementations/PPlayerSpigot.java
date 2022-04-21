package nl.pim16aap2.bigdoors.spigot.util.implementations;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.commands.PermissionsStatus;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
@Flogger
public final class PPlayerSpigot implements IPPlayer
{
    private final Player spigotPlayer;

    public PPlayerSpigot(Player spigotPlayer)
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
        // TODO: IMPLEMENT THIS
        log.at(Level.SEVERE).withStackTrace(StackSize.FULL).log("Method not implemented!");
        return true;
    }

    @Override
    public Optional<IPLocation> getLocation()
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
    public int getDoorSizeLimit()
    {
        // TODO: IMPLEMENT THIS
        log.at(Level.SEVERE).withStackTrace(StackSize.FULL).log("Method not implemented!");
        return Integer.MAX_VALUE;
    }

    @Override
    public int getDoorCountLimit()
    {
        // TODO: IMPLEMENT THIS
        log.at(Level.SEVERE).withStackTrace(StackSize.FULL).log("Method not implemented!");
        return Integer.MAX_VALUE;
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
    public void sendMessage(Level level, String message)
    {
        spigotPlayer.sendMessage(message);
    }

    /**
     * Gets the bukkit player represented by this {@link IPPlayer}
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
        return getUUID().equals(((PPlayerSpigot) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }
}
