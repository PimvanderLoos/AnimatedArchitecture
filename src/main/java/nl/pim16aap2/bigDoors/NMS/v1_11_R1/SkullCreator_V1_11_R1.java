package nl.pim16aap2.bigDoors.NMS.v1_11_R1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.MinecraftServer;
import net.minecraft.server.v1_11_R1.PlayerInteractManager;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.HeadManager;
import nl.pim16aap2.bigDoors.util.Skull;

public class SkullCreator_V1_11_R1 extends HeadManager
{
    public SkullCreator_V1_11_R1(BigDoors plugin)
    {
        super(plugin);
    }

    @Override
    public String[] getFromPlayer(Player playerBukkit)
    {
        EntityPlayer playerNMS = ((CraftPlayer) playerBukkit).getHandle();
        GameProfile profile = playerNMS.getProfile();
        Property property = profile.getProperties().get("textures").iterator().next();
        String texture = property.getValue();
        String signature = property.getSignature();
        return new String[] { texture, signature };
    }

    @Override
    public void createSkull(int x, int y, int z, String name, UUID playerUUID, Player p)
    {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            String[] a = getFromName(name, p);
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            GameProfile profile = new GameProfile(UUID.randomUUID(), name);

            profile.getProperties().put("textures", new Property("textures", a[0], a[1]));

            EntityPlayer npc = new EntityPlayer(server, server.getWorldServer(0), profile,
                                                new PlayerInteractManager(server.getWorldServer(0)));

            npc.setPosition(x, y, z);

            byte[] dec = Base64.getDecoder().decode(a[0]);
            String s = new String(dec);

            s = s.substring(s.indexOf("l\":\"") + 1);
            s = s.substring(0, s.indexOf("\"}}}"));
            s = s.substring(s.indexOf("\""));
            s = s.substring(1);
            s = s.substring(1);
            s = s.substring(1);

            ItemStack skull = (Skull.getCustomSkull(s));
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            sm.setDisplayName(name);
            skull.setItemMeta(sm);
            end();

            headMap.put(playerUUID, skull);
        });
    }

    @Override
    protected String[] getFromName(String name, Player p)
    {
        try
        {
            if (!map.contains(name))
            {
                URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
                try
                {
                    String uuid = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();
                    URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid
                        + "?unsigned=false");
                    InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
                    JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties")
                        .getAsJsonArray().get(0).getAsJsonObject();
                    String texture = textureProperty.get("value").getAsString();
                    String signature = textureProperty.get("signature").getAsString();
                    map.put(name, textureProperty);

                    return new String[] { texture, signature };
                }
                catch (IllegalStateException e)
                {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &3Player not found in mojang database."));
                }

            }
            else
            {
                JsonObject textureProperty = map.get(name);
                String texture = textureProperty.get("value").getAsString();
                String signature = textureProperty.get("signature").getAsString();
                return new String[] { texture, signature };
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not get skin data from session servers!");
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
