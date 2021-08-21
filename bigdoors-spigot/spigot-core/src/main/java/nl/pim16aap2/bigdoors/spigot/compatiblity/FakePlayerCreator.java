package nl.pim16aap2.bigdoors.spigot.compatiblity;

import com.mojang.authlib.GameProfile;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Class used to create a fake-online player who is actually offline.
 *
 * @author Pim
 */
class FakePlayerCreator
{
    static final String FAKE_PLAYER_METADATA = "isBigDoorsFakePlayer";

    private final String nmsBase;
    private final String craftBase;
    private final BigDoorsSpigot plugin;
    private final Class<?> craftOfflinePlayer;
    private final Class<?> craftWorld;
    private final Method getProfile;
    private final Method getHandle;
    private final Method getServer;
    private final Method getBukkitEntity;
    private final Constructor<?> entityPlayerConstructor;
    private final Constructor<?> playerInteractManagerConstructor;
    private final Field uuid;
    private final boolean success;

    FakePlayerCreator(BigDoorsSpigot plugin)
        throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException
    {
        this.plugin = plugin;

        nmsBase = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        craftBase = "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]
            + ".";

        craftOfflinePlayer = getCraftClass("CraftOfflinePlayer");
        craftWorld = getCraftClass("CraftWorld");
        Class<?> worldServer = getNMSClass("WorldServer");
        Class<?> entityPlayer = getNMSClass("EntityPlayer");
        Class<?> minecraftServer = getNMSClass("MinecraftServer");
        Class<?> playerInteractManager = getNMSClass("PlayerInteractManager");
        entityPlayerConstructor = entityPlayer.getConstructor(minecraftServer, worldServer, GameProfile.class,
                                                              playerInteractManager);
        getBukkitEntity = entityPlayer.getMethod("getBukkitEntity");
        getHandle = craftWorld.getMethod("getHandle");
        getProfile = craftOfflinePlayer.getMethod("getProfile");
        getServer = minecraftServer.getMethod("getServer");
        uuid = getNMSClass("Entity").getDeclaredField("uniqueID");
        uuid.setAccessible(true);

        Class<?> world = getNMSClass("World");
        // TODO: wtf is this???
//        PlayerInteractManagerConstructor = playerInteractManager.getConstructor(worldServer);
        playerInteractManagerConstructor = playerInteractManager.getConstructor(world);
        success = true;
    }

    private Class<?> getNMSClass(String name)
        throws LinkageError, ClassNotFoundException
    {
        return Class.forName(nmsBase + name);
    }

    private Class<?> getCraftClass(String name)
        throws LinkageError, ClassNotFoundException
    {
        return Class.forName(craftBase + name);
    }

    /**
     * Construct a fake-online {@link Player} from an {@link OfflinePlayer}.
     *
     * @param oPlayer
     *     The {@link OfflinePlayer} to use as base for the fake online {@link Player}.
     * @param world
     *     The world the fake {@link Player} is supposedly in.
     * @return The fake-online {@link Player}
     */
    Optional<Player> getFakePlayer(OfflinePlayer oPlayer, World world)
    {
        if (!success)
            return Optional.empty();

        @Nullable Player player = null;

        try
        {
            Object coPlayer = craftOfflinePlayer.cast(oPlayer);
            GameProfile gProfile = (GameProfile) getProfile.invoke(coPlayer);

            Object craftServer = craftWorld.cast(world);
            Object worldServer = getHandle.invoke(craftServer);
            Object minecraftServer = getServer.invoke(worldServer);
            Object playerInteractManager = playerInteractManagerConstructor.newInstance(worldServer);

            Object ePlayer = entityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile,
                                                                 playerInteractManager);
            uuid.set(ePlayer, oPlayer.getUniqueId());
            player = (Player) getBukkitEntity.invoke(ePlayer);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e)
        {
            plugin.getPLogger().logThrowable(e);
        }

        if (player != null)
            player.setMetadata(FAKE_PLAYER_METADATA, new FixedMetadataValue(plugin, true));

        return Optional.ofNullable(player);
    }
}
