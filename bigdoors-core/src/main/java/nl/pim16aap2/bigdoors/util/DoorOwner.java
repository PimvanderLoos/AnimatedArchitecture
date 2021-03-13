package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import org.jetbrains.annotations.NotNull;

/**
 * Contains all details needed about a doorOwner.
 *
 * @author Pim
 */
@AllArgsConstructor
public class DoorOwner
{
    /**
     * The UID of the door that is owned.
     */
    @Getter
    long doorUID;

    /**
     * The permission level at which the player owns the door.
     */
    @Getter
    int permission;

    /**
     * The {@link IPPlayer} object represented by this {@link DoorOwner}.
     */
    @Getter
    @NonNull IPPlayer player;

    /**
     * Constructor of {@link DoorOwner}.
     *
     * @param doorUID    The UID of the DoorBase.
     * @param permission The permission level at which the player owns the door.
     * @param playerData The {@link PPlayerData} of the player that owns the given door.
     */
    public DoorOwner(final long doorUID, final int permission, final @NonNull PPlayerData playerData)
    {
        this(doorUID, permission, BigDoors.get().getPlatform().getPPlayerFactory().create(playerData));
    }

    /**
     * Get a basic overview of this door owner. Useful for debugging.
     */
    @Override
    public @NotNull String toString()
    {
        return "doorUID: " + doorUID + ". player: " + getPlayer().toString() + ". Permission: " + permission;
    }

    @Override
    public final int hashCode()
    {
        return player.getUUID().hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        DoorOwner other = (DoorOwner) o;
        return player.equals(other.player) && doorUID == other.doorUID && permission == other.permission;
    }

    @Override
    public @NotNull DoorOwner clone()
    {
        // TODO: Clone player as well?
        return new DoorOwner(doorUID, getPermission(), getPlayer());
    }
}
