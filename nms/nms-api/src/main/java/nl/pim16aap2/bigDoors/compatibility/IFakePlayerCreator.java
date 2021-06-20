package nl.pim16aap2.bigDoors.compatibility;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface IFakePlayerCreator
{
    String FAKEPLAYERMETADATA = "isBigDoorsFakePlayer";

    /**
     * Construct a fake-online {@link Player} from an {@link OfflinePlayer}.
     *
     * @param oPlayer    The {@link OfflinePlayer} to use as base for the fake online {@link Player}.
     * @param playerName The name of the player.
     * @param world      The world the fake {@link Player} is supposedly in.
     * @return The fake-online {@link Player}
     */
    Player getFakePlayer(OfflinePlayer oPlayer, String playerName, World world);
}
