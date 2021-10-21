package nl.pim16aap2.bigDoors.compatiblity;

import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static nl.pim16aap2.bigDoors.reflection.ReflectionBuilder.*;

/**
 * Class used to create a fake-online player who is actually offline.
 *
 * @author Pim
 */
public class FakePlayerCreator
{
    public static final String FAKE_PLAYER_METADATA = "isBigDoorsFakePlayer";

    private Class<?> classCraftOfflinePlayer;
    private Class<?> classCraftWorld;
    private Class<?> classWorldServer;
    private Class<?> classEntityPlayer;
    private Class<?> classMinecraftServer;
    private Class<?> classPlayerInteractManager;
    private Class<?> classGameProfile;

    private Method methodGetProfile;
    private Method methodGetHandle;
    private Method methodGetServer;
    private Method methodGetBukkitEntity;

    private Constructor<?> cTorEntityPlayerConstructor;
    private @Nullable Constructor<?> cTorPlayerInteractManager;

    private Field fieldUuid;
    private Field fieldPlayerNameVar;

    private final BigDoors plugin;

    private boolean success = false;

    public FakePlayerCreator(final BigDoors plugin)
    {
        this.plugin = plugin;

        final String nmsBase = "net.minecraft.server." + BigDoors.get().getPackageVersion() + ".";
        final String craftBase = "org.bukkit.craftbukkit." + BigDoors.get().getPackageVersion() + ".";

        classCraftOfflinePlayer = findClass(craftBase + "CraftOfflinePlayer").get();
        classCraftWorld = findClass(craftBase + "CraftWorld").get();
        classWorldServer = findClass(nmsBase + "WorldServer", "net.minecraft.server.level.WorldServer").get();
        classEntityPlayer = findClass(nmsBase + "EntityPlayer", "net.minecraft.server.level.EntityPlayer").get();
        classMinecraftServer = findClass(nmsBase + "MinecraftServer", "net.minecraft.server.MinecraftServer").get();
        classPlayerInteractManager = findClass(nmsBase + "PlayerInteractManager",
                                               "net.minecraft.server.level.PlayerInteractManager").get();
        classGameProfile = findClass("com.mojang.authlib.GameProfile").get();

        cTorEntityPlayerConstructor = findConstructor()
            .inClass(classEntityPlayer)
            .withParameters(parameterBuilder()
                                .withRequiredParameters(classMinecraftServer, classWorldServer, classGameProfile)
                                .withOptionalParameters(classPlayerInteractManager)).get();

        methodGetBukkitEntity = findMethod().inClass(classEntityPlayer).withName("getBukkitEntity").get();
        methodGetHandle = findMethod().inClass(classCraftWorld).withName("getHandle").get();
        methodGetProfile = findMethod().inClass(classCraftOfflinePlayer).withName("getProfile").get();
        methodGetServer = findMethod().inClass(classMinecraftServer).withName("getServer").get();

        Class<?> classNMSEntity = findClass(nmsBase + "Entity", "net.minecraft.world.entity.Entity").get();

        fieldUuid = findField().inClass(classNMSEntity).ofType(UUID.class).withModifiers(Modifier.PROTECTED).get();
        fieldUuid.setAccessible(true);

        fieldPlayerNameVar = findField().inClass(classGameProfile).withName("name").get();
        fieldPlayerNameVar.setAccessible(true);

        final Class<?> classNMSWorld = findClass(nmsBase + "World", "net.minecraft.world.level.World").get();
        cTorPlayerInteractManager = findConstructor()
            .inClass(classPlayerInteractManager)
            .withParameters(parameterBuilder()
                                .withOptionalParameters(classWorldServer)
                                .withOptionalParameters(classNMSWorld)).setNullable().get();
        success = true;
    }

    public Player getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
    {
        if (!success || oPlayer == null || world == null)
            return null;

        Player player = null;

        try
        {
            Object coPlayer = classCraftOfflinePlayer.cast(oPlayer);
            Object gProfile = methodGetProfile.invoke(coPlayer);
            fieldPlayerNameVar.set(gProfile, playerName);

            Object craftServer = classCraftWorld.cast(world);
            Object worldServer = methodGetHandle.invoke(craftServer);
            Object minecraftServer = methodGetServer.invoke(worldServer);

            final Object ePlayer;
            if (cTorPlayerInteractManager == null)
                ePlayer = cTorEntityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile);
            else
            {
                Object playerInteractManager = cTorPlayerInteractManager.newInstance(worldServer);

                ePlayer = cTorEntityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile,
                                                                  playerInteractManager);
            }

            fieldUuid.set(ePlayer, oPlayer.getUniqueId());
            player = (Player) methodGetBukkitEntity.invoke(ePlayer);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e)
        {
            e.printStackTrace();
        }

        if (player != null)
            player.setMetadata(FAKE_PLAYER_METADATA, new FixedMetadataValue(plugin, true));

        return player;
    }
}
