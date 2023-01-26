package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.experimental.Delegate;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.commands.PermissionsStatus;
import nl.pim16aap2.bigdoors.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
public final class OfflinePPlayerSpigot implements IPPlayer
{
    @Delegate
    private final PPlayerData playerData;
    private final @Nullable OfflinePlayer spigotPlayer;

    public OfflinePPlayerSpigot(PPlayerData playerData, @Nullable OfflinePlayer spigotPlayer)
    {
        this.playerData = playerData;
        this.spigotPlayer = spigotPlayer;
    }

    public OfflinePPlayerSpigot(PPlayerData playerData)
    {
        this(playerData, Bukkit.getOfflinePlayer(playerData.getUUID()));
    }

    /**
     * Calling this method has no effect, as there is no player to send the message to.
     */
    @Override
    @Deprecated
    public void sendMessage(Text text)
    {
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(String permission)
    {
        return CompletableFuture.completedFuture(isOp());
    }

    @Override
    public CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new PermissionsStatus(isOp(), isOp()));
    }

    @Override
    public boolean isOnline()
    {
        return false;
    }

    /**
     * Gets the bukkit {@link OfflinePlayer} represented by this {@link IPPlayer} if it exists.
     *
     * @return The Bukkit player.
     */
    public @Nullable OfflinePlayer getBukkitPlayer()
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
        return getUUID().equals(((OfflinePPlayerSpigot) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    @Override
    public Optional<IPLocation> getLocation()
    {
        return Optional.empty();
    }
}
