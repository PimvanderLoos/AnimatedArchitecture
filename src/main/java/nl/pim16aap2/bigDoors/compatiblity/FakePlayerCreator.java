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
public class FakePlayerCreator implements IFakePlayerCreator
{
    private static final String PACKAGE_VERSION_NAME = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static final String NMS_BASE = "net.minecraft.server." + PACKAGE_VERSION_NAME + ".";
    private static final String CRAFT_BASE = "org.bukkit.craftbukkit." + PACKAGE_VERSION_NAME + ".";

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
        return Class.forName(NMS_BASE + name);
    }

    private Class<?> getCraftClass(String name) throws ClassNotFoundException
    {
        return Class.forName(CRAFT_BASE + name);
    }

    public FakePlayerCreator(final BigDoors plugin)
    {
        this.plugin = plugin;

        try
        {
            CraftOfflinePlayer = getCraftClass("CraftOfflinePlayer");
            CraftWorld = getCraftClass("CraftWorld");
            WorldServer = getNMSClass("WorldServer");
            EntityPlayer = getNMSClass("EntityPlayer");
            MinecraftServer = getNMSClass("MinecraftServer");
            PlayerInteractManager = getNMSClass("PlayerInteractManager");
            EntityPlayerConstructor = EntityPlayer.getConstructor(MinecraftServer, WorldServer, GameProfile.class,
                                                                  PlayerInteractManager);
            getBukkitEntity = EntityPlayer.getMethod("getBukkitEntity");
            getHandle = CraftWorld.getMethod("getHandle");
            getProfile = CraftOfflinePlayer.getMethod("getProfile");
            getServer = MinecraftServer.getMethod("getServer");
            uuid = getNMSClass("Entity").getDeclaredField("uniqueID");
            uuid.setAccessible(true);

            playerNameVar = GameProfile.class.getDeclaredField("name");
            playerNameVar.setAccessible(true);

            World = getNMSClass("World");
            try
            {
                PlayerInteractManagerConstructor = PlayerInteractManager.getConstructor(WorldServer);
            }
            catch (Exception e)
            {
                PlayerInteractManagerConstructor = PlayerInteractManager.getConstructor(World);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | NoSuchFieldException e)
        {
            e.printStackTrace();
            return;
        }
        success = true;
    }

    @Override public Player getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
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
