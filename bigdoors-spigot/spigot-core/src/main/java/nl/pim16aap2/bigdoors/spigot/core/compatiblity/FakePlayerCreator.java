package nl.pim16aap2.bigdoors.spigot.core.compatiblity;

import lombok.extern.flogger.Flogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Class used to create a fake-online player who is actually offline.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class FakePlayerCreator
{
    public static final String FAKE_PLAYER_METADATA = "isBigDoorsFakePlayer";

    FakePlayerCreator(JavaPlugin plugin)
    {
    }

    /**
     * TODO: Rewrite fake player creation
     *
     * @deprecated
     */
    @Deprecated
    public Optional<Player> getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
    {
        throw new UnsupportedOperationException();
    }
}
