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
    static final String FAKEPLAYERMETADATA = "isBigDoorsFakePlayer";

    private final String NMSbase;
    private final String CraftBase;
    private final BigDoorsSpigot plugin;
    private final Class<?> CraftOfflinePlayer;
    private final Class<?> CraftWorld;
    private final Class<?> World;
    private final Class<?> WorldServer;
    private final Class<?> EntityPlayer;
    private final Class<?> MinecraftServer;
    private final Class<?> PlayerInteractManager;
    private final Method getProfile;
    private final Method getHandle;
    private final Method getServer;
    private final Method getBukkitEntity;
    private final Constructor<?> EntityPlayerConstructor;
    private Constructor<?> PlayerInteractManagerConstructor;
    private final Field uuid;
    private boolean success = false;

    FakePlayerCreator(BigDoorsSpigot plugin)
        throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException
    {
        this.plugin = plugin;

        NMSbase = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        CraftBase = "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]
            + ".";

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

        World = getNMSClass("World");
        // TODO: ???
        PlayerInteractManagerConstructor = PlayerInteractManager.getConstructor(WorldServer);
        PlayerInteractManagerConstructor = PlayerInteractManager.getConstructor(World);
        success = true;
    }

    private Class<?> getNMSClass(String name)
        throws LinkageError, ClassNotFoundException
    {
        return Class.forName(NMSbase + name);
    }

    private Class<?> getCraftClass(String name)
        throws LinkageError, ClassNotFoundException
    {
        return Class.forName(CraftBase + name);
    }

    /**
     * Construct a fake-online {@link Player} from an {@link OfflinePlayer}.
     *
     * @param oPlayer The {@link OfflinePlayer} to use as base for the fake online {@link Player}.
     * @param world   The world the fake {@link Player} is supposedly in.
     * @return The fake-online {@link Player}
     */
    Optional<Player> getFakePlayer(OfflinePlayer oPlayer, World world)
    {
        if (!success)
            return Optional.empty();

        @Nullable Player player = null;

        try
        {
            Object coPlayer = CraftOfflinePlayer.cast(oPlayer);
            GameProfile gProfile = (GameProfile) getProfile.invoke(coPlayer);

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
            plugin.getPLogger().logThrowable(e);
        }

        if (player != null)
            player.setMetadata(FAKEPLAYERMETADATA, new FixedMetadataValue(plugin, true));

        return Optional.ofNullable(player);
    }
}
