package nl.pim16aap2.bigdoors.spigot.compatiblity;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.reflection.ReflectionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Class used to create a fake-online player who is actually offline.
 *
 * @author Pim
 */
@Singleton
@Flogger
class FakePlayerCreator
{
    static final String FAKE_PLAYER_METADATA = "isBigDoorsFakePlayer";

    private final JavaPlugin plugin;

    private final String nmsBase;
    private final String craftBase;
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

    FakePlayerCreator(JavaPlugin plugin)
        throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException
    {
        this.plugin = plugin;

        final String packageName = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        nmsBase = "net.minecraft.server." + packageName;
        craftBase = "org.bukkit.craftbukkit." + packageName;

        craftOfflinePlayer = getCraftClass("CraftOfflinePlayer");
        craftWorld = getCraftClass("CraftWorld");
        final Class<?> worldServer = getNMSClass("WorldServer");
        final Class<?> entityPlayer = getNMSClass("EntityPlayer");
        final Class<?> minecraftServer = getNMSClass("MinecraftServer");
        final Class<?> playerInteractManager = getNMSClass("PlayerInteractManager");
        final Class<?> classGameProfile = ReflectionBuilder.findClass("com.mojang.authlib.GameProfile").getRequired();
        entityPlayerConstructor = entityPlayer.getConstructor(minecraftServer, worldServer, classGameProfile,
                                                              playerInteractManager);
        getBukkitEntity = entityPlayer.getMethod("getBukkitEntity");
        getHandle = craftWorld.getMethod("getHandle");
        getProfile = craftOfflinePlayer.getMethod("getProfile");
        getServer = minecraftServer.getMethod("getServer");
        uuid = getNMSClass("Entity").getDeclaredField("uniqueID");
        uuid.setAccessible(true);

        final Class<?> world = getNMSClass("World");
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
            final Object coPlayer = craftOfflinePlayer.cast(oPlayer);
            final Object gProfile = getProfile.invoke(coPlayer);

            final Object craftServer = craftWorld.cast(world);
            final Object worldServer = getHandle.invoke(craftServer);
            final Object minecraftServer = getServer.invoke(worldServer);
            final Object playerInteractManager = playerInteractManagerConstructor.newInstance(worldServer);

            final Object ePlayer = entityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile,
                                                                       playerInteractManager);
            uuid.set(ePlayer, oPlayer.getUniqueId());
            player = (Player) getBukkitEntity.invoke(ePlayer);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }

        if (player != null)
            player.setMetadata(FAKE_PLAYER_METADATA, new FixedMetadataValue(plugin, true));

        return Optional.ofNullable(player);
    }
}
