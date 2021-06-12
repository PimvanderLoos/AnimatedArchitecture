package nl.pim16aap2.bigDoors.NMS;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import nl.pim16aap2.bigDoors.compatibility.IFakePlayerCreator;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftOfflinePlayer;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class FakePlayerCreator_V1_17_R1 implements IFakePlayerCreator
{
    private static final Field GAME_PROFILE_SET_NAME;
    private static final Field ENTITY_UUID;
    private static final boolean INIT_SUCCESSFUL;
    private static final MinecraftServer MINECRAFT_SERVER;
    static
    {
        GAME_PROFILE_SET_NAME = getField(GameProfile.class, "name");
        MINECRAFT_SERVER = ((CraftServer) Bukkit.getServer()).getServer();
        ENTITY_UUID = getField(Entity.class, "aj");

        INIT_SUCCESSFUL =
            GAME_PROFILE_SET_NAME != null
            && MINECRAFT_SERVER != null
            && ENTITY_UUID != null
        ;
    }

    private final JavaPlugin plugin;

    public FakePlayerCreator_V1_17_R1(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public Player getFakePlayer(OfflinePlayer oPlayer, String playerName, World world)
    {
        if (!INIT_SUCCESSFUL)
            return null;
        try
        {
            GameProfile gameProfile = ((CraftOfflinePlayer) oPlayer).getProfile();
            GAME_PROFILE_SET_NAME.set(gameProfile, playerName);
            WorldServer worldServer = ((CraftWorld) world).getHandle();

            EntityPlayer fakePlayer = new EntityPlayer(MINECRAFT_SERVER, worldServer, gameProfile);

            ENTITY_UUID.set(fakePlayer, oPlayer.getUniqueId());

            Player onlineBukkitPlayer = fakePlayer.getBukkitEntity();

            if (onlineBukkitPlayer != null)
                onlineBukkitPlayer.setMetadata(FAKEPLAYERMETADATA, new FixedMetadataValue(plugin, true));

            return onlineBukkitPlayer;
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private static Field getField(Class<?> clz, String name)
    {
        try
        {
            Field field = clz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
