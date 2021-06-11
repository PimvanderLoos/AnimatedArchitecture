package nl.pim16aap2.bigDoors.compatiblity;

import com.mojang.authlib.GameProfile;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class used to create a fake-online player who is actually offline.
 *
 * @author Pim
 */
public class FakePlayerCreator
{
    public static final String FAKEPLAYERMETADATA = "isBigDoorsFakePlayer";

    private final String NMSbase;
    private final String CraftBase;
    private Class<?> CraftOfflinePlayer;
    private Class<?> CraftWorld;
    private Class<?> World;
    private Class<?> WorldServer;
    private Class<?> EntityPlayer;
    private Class<?> MinecraftServer;
    private Class<?> PlayerInteractManager;
    private Method getProfile;
    private Method getHandle;
    private Method getServer;
    private Method getBukkitEntity;
    private Constructor<?> EntityPlayerConstructor;
    private Constructor<?> PlayerInteractManagerConstructor;
    private Field uuid;
    private Field playerNameVar;

    private final BigDoors plugin;

    private boolean success = false;

    private Class<?> getNMSClass(String name) throws ClassNotFoundException
    {
        return Class.forName(NMSbase + name);
    }

    private Class<?> getCraftClass(String name) throws ClassNotFoundException
    {
        return Class.forName(CraftBase + name);
    }

    public FakePlayerCreator(final BigDoors plugin)
    {
        this.plugin = plugin;
        this.NMSbase = null;
        this.CraftBase = null;
        success = true;
    }

    /**
     * Construct a fake-online {@link Player} from an {@link OfflinePlayer}.
     *
     * @param oPlayer    The {@link OfflinePlayer} to use as base for the fake online
     *                   {@link Player}.
     * @param playerName The name of the player.
     * @param world      The world the fake {@link Player} is supposedly in.
     * @return The fake-online {@link Player}
     */
    public Player getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
    {
        if (!success || oPlayer == null || world == null)
            return null;

        Player player = null;

        try
        {
            Object coPlayer = CraftOfflinePlayer.cast(oPlayer);
            GameProfile gProfile = (GameProfile) getProfile.invoke(coPlayer);
            playerNameVar.set(gProfile, playerName);

            Object craftServer = CraftWorld.cast(world);
            Object worldServer = getHandle.invoke(craftServer);
            Object minecraftServer = getServer.invoke(worldServer);
            Object playerInteractManager = PlayerInteractManagerConstructor.newInstance(worldServer);

            Object ePlayer = EntityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile,
                                                                 playerInteractManager);
            uuid.set(ePlayer, oPlayer.getUniqueId());
            player = (Player) getBukkitEntity.invoke(ePlayer);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e)
        {
            e.printStackTrace();
        }

        if (player != null)
            player.setMetadata(FAKEPLAYERMETADATA, new FixedMetadataValue(plugin, true));

        return player;
    }
}
