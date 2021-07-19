package nl.pim16aap2.bigDoors.compatiblity;

import com.mojang.authlib.GameProfile;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.Util;
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

import static nl.pim16aap2.bigDoors.util.ReflectionUtils.*;

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

        try
        {
            classCraftOfflinePlayer = Class.forName(CRAFT_BASE + "CraftOfflinePlayer");
            classCraftWorld = Class.forName(CRAFT_BASE + "CraftWorld");
            classWorldServer = findFirstClass(NMS_BASE + "WorldServer", "net.minecraft.server.level.WorldServer");
            classEntityPlayer = findFirstClass(NMS_BASE + "EntityPlayer", "net.minecraft.server.level.EntityPlayer");
            classMinecraftServer = findFirstClass(NMS_BASE + "MinecraftServer", "net.minecraft.server.MinecraftServer");
            classPlayerInteractManager = findFirstClass(NMS_BASE + "PlayerInteractManager",
                                                        "net.minecraft.server.level.PlayerInteractManager");
            cTorEntityPlayerConstructor =
                Util.firstNonNull(() -> findCTor(false, classEntityPlayer, classMinecraftServer,
                                                 classWorldServer, GameProfile.class),
                                  () -> findCTor(false, classEntityPlayer, classMinecraftServer,
                                                 classWorldServer, GameProfile.class, classPlayerInteractManager));

            methodGetBukkitEntity = classEntityPlayer.getMethod("getBukkitEntity");
            methodGetHandle = classCraftWorld.getMethod("getHandle");
            methodGetProfile = classCraftOfflinePlayer.getMethod("getProfile");
            methodGetServer = classMinecraftServer.getMethod("getServer");

            Class<?> classNMSEntity = findFirstClass(NMS_BASE + "Entity", "net.minecraft.world.entity.Entity");

            fieldUuid = ReflectionUtils.getField(classNMSEntity, getModifiers(Modifier.PROTECTED), UUID.class);
            fieldUuid.setAccessible(true);

            fieldPlayerNameVar = GameProfile.class.getDeclaredField("name");
            fieldPlayerNameVar.setAccessible(true);

            cTorPlayerInteractManager = findCTor(false, classPlayerInteractManager, classWorldServer);
            if (cTorPlayerInteractManager == null)
            {
                Class<?> classNMSWorld = findFirstClass(false, NMS_BASE + "World");
                if (classNMSWorld != null)
                    cTorPlayerInteractManager = findCTor(false, classPlayerInteractManager, classNMSWorld);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | NoSuchFieldException e)
        {
            e.printStackTrace();
            return;
        }
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
            GameProfile gProfile = (GameProfile) methodGetProfile.invoke(coPlayer);
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
