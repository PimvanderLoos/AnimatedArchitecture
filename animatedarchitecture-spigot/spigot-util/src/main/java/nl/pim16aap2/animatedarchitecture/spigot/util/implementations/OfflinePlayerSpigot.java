package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import lombok.experimental.Delegate;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandDefinition;
import nl.pim16aap2.animatedarchitecture.core.commands.PermissionsStatus;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPlayer} for the Spigot platform.
 */
public final class OfflinePlayerSpigot implements IPlayer
{
    @Delegate
    private final PlayerData playerData;
    private final @Nullable OfflinePlayer spigotPlayer;

    public OfflinePlayerSpigot(PlayerData playerData, @Nullable OfflinePlayer spigotPlayer)
    {
        this.playerData = playerData;
        this.spigotPlayer = spigotPlayer;
    }

    public OfflinePlayerSpigot(PlayerData playerData)
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
     * Gets the bukkit {@link OfflinePlayer} represented by this {@link IPlayer} if it exists.
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
        return getUUID().equals(((OfflinePlayerSpigot) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    @Override
    public Optional<ILocation> getLocation()
    {
        return Optional.empty();
    }
}
