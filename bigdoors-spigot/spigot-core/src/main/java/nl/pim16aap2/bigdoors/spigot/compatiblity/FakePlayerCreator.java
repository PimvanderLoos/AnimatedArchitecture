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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static nl.pim16aap2.reflection.ReflectionBuilder.*;

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

    private final JavaPlugin plugin;

    private final Class<?> classCraftOfflinePlayer;
    private final Class<?> classCraftWorld;

    private final Method methodGetProfile;
    private final Method methodGetHandle;
    private final Method methodGetServer;
    private final Method methodGetBukkitEntity;

    private final Constructor<?> cTorEntityPlayerConstructor;
    private final @Nullable Constructor<?> cTorPlayerInteractManager;

    private final Field fieldUuid;
    private final Field fieldPlayerNameVar;

    FakePlayerCreator(JavaPlugin plugin)
    {
        this.plugin = plugin;

        final String packageVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        final String nmsBase = "net.minecraft.server." + packageVersion + ".";
        final String craftBase = "org.bukkit.craftbukkit." + packageVersion + ".";

        classCraftOfflinePlayer = ReflectionBuilder.findClass(craftBase + "CraftOfflinePlayer").get();
        classCraftWorld = findClass(craftBase + "CraftWorld").get();

        final Class<?> classWorldServer =
            findClass(nmsBase + "WorldServer", "net.minecraft.server.level.WorldServer").get();
        final Class<?> classEntityPlayer =
            findClass(nmsBase + "EntityPlayer", "net.minecraft.server.level.EntityPlayer").get();
        final Class<?> classMinecraftServer =
            findClass(nmsBase + "MinecraftServer", "net.minecraft.server.MinecraftServer").get();
        final Class<?> classPlayerInteractManager =
            findClass(nmsBase + "PlayerInteractManager", "net.minecraft.server.level.PlayerInteractManager").get();
        final Class<?> classNMSEntity =
            findClass(nmsBase + "Entity", "net.minecraft.world.entity.Entity").get();
        final Class<?> classGameProfile =
            findClass("com.mojang.authlib.GameProfile").get();

        cTorEntityPlayerConstructor = findConstructor()
            .inClass(classEntityPlayer)
            .withParameters(parameterBuilder()
                                .withRequiredParameters(classMinecraftServer, classWorldServer, classGameProfile)
                                .withOptionalParameters(classPlayerInteractManager)).get();

        methodGetBukkitEntity = findMethod().inClass(classEntityPlayer).withName("getBukkitEntity").get();
        methodGetHandle = findMethod().inClass(classCraftWorld).withName("getHandle").get();
        methodGetProfile = findMethod().inClass(classCraftOfflinePlayer).withName("getProfile").get();
        methodGetServer = findMethod().inClass(classMinecraftServer).withName("getServer").get();

        fieldUuid = findField().inClass(classNMSEntity).ofType(UUID.class).withModifiers(Modifier.PROTECTED).get();
        fieldUuid.setAccessible(true);

        fieldPlayerNameVar = findField().inClass(classGameProfile).withName("name").get();
        fieldPlayerNameVar.setAccessible(true);

        final Class<?> classNMSWorld = findClass(nmsBase + "World", "net.minecraft.world.level.World").get();
        cTorPlayerInteractManager = findConstructor().inClass(classPlayerInteractManager)
                                                     .withParameters(parameterBuilder()
                                                                         .withOptionalParameters(classWorldServer)
                                                                         .withOptionalParameters(classNMSWorld))
                                                     .getNullable();
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
    public Optional<Player> getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
    {
        @Nullable Player player = null;

        try
        {
            final Object coPlayer = classCraftOfflinePlayer.cast(oPlayer);
            final Object gProfile = methodGetProfile.invoke(coPlayer);
            fieldPlayerNameVar.set(gProfile, playerName);

            final Object craftServer = classCraftWorld.cast(world);
            final Object worldServer = methodGetHandle.invoke(craftServer);
            final Object minecraftServer = methodGetServer.invoke(worldServer);

            final Object ePlayer;
            if (cTorPlayerInteractManager == null)
                ePlayer = cTorEntityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile);
            else
            {
                final Object playerInteractManager = cTorPlayerInteractManager.newInstance(worldServer);
                ePlayer = cTorEntityPlayerConstructor.newInstance(minecraftServer, worldServer, gProfile,
                                                                  playerInteractManager);
            }

            fieldUuid.set(ePlayer, oPlayer.getUniqueId());
            player = (Player) methodGetBukkitEntity.invoke(ePlayer);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to create fake player!");
        }

        if (player != null)
            player.setMetadata(FAKE_PLAYER_METADATA, new FixedMetadataValue(plugin, true));

        return Optional.ofNullable(player);
    }
}
