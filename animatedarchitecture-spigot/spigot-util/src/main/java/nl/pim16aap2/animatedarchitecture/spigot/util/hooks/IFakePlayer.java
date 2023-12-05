package nl.pim16aap2.animatedarchitecture.spigot.util.hooks;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * The interface that is implemented by all fake players to allow easy identification.
 * <p>
 * It also contains some internal methods that are used by the generated subclass. It is not recommended to use these
 * methods.
 */
public interface IFakePlayer
{
    OfflinePlayer getOfflinePlayer0();

    /**
     * Gets the location of the player.
     * <p>
     * Please consider using {@link Player#getLocation()} or {@link Player#getLocation(Location)} instead.
     * <p>
     * Those methods create defensive copies of the location; this method does not.
     *
     * @return The location of the player.
     */
    Location getLocation0();

    @SuppressWarnings("unused")
    default int hashCode0()
    {
        return Objects.hash(getOfflinePlayer0(), getLocation0());
    }

    @SuppressWarnings("unused")
    default boolean equals0(Object other)
    {
        if (!(other instanceof IFakePlayer player))
            return false;
        return Objects.equals(getOfflinePlayer0(), player.getOfflinePlayer0()) &&
            Objects.equals(getLocation0(), player.getLocation0());
    }

    @SuppressWarnings("unused")
    default String toString0()
    {
        return "FakePlayer{" +
            "offlinePlayer=" + getOfflinePlayer0() +
            ", location=" + getLocation0() +
            '}';
    }
}
