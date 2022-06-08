package nl.pim16aap2.bigDoors.compatibility;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

/**
 * Class used to create a fake-online player who is actually offline.
 *
 * @author Pim
 */
public class FakePlayerCreator
{
    public static final String FAKE_PLAYER_METADATA = "isBigDoorsFakePlayer";

    private final @Nullable FakePlayerInstantiator fakePlayerInstantiator;

    public FakePlayerCreator(final JavaPlugin plugin)
    {
        fakePlayerInstantiator = createFakePlayerInstantiator(plugin);
    }

    @Nullable Player getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
    {
        return fakePlayerInstantiator == null ? null : fakePlayerInstantiator.getFakePlayer(oPlayer, playerName, world);
    }

    private @Nullable FakePlayerInstantiator createFakePlayerInstantiator(JavaPlugin plugin)
    {
        try
        {
            return new FakePlayerInstantiator(plugin);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
