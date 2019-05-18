package nl.pim16aap2.bigDoors.util;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.org.apache.commons.codec.binary.Base64;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import nl.pim16aap2.bigDoors.HeadManager;

public enum Skull
{

    ARROW_LEFT("MHF_ArrowLeft"), ARROW_RIGHT("MHF_ArrowRight"), ARROW_UP("MHF_ArrowUp"), ARROW_DOWN("MHF_ArrowDown"),
    QUESTION("MHF_Question"), EXCLAMATION("MHF_Exclamation"), CAMERA("FHG_Cam"),

    ZOMBIE_PIGMAN("MHF_PigZombie"), PIG("MHF_Pig"), SHEEP("MHF_Sheep"), BLAZE("MHF_Blaze"), CHICKEN("MHF_Chicken"),
    COW("MHF_Cow"), SLIME("MHF_Slime"), SPIDER("MHF_Spider"), SQUID("MHF_Squid"), VILLAGER("MHF_Villager"),
    OCELOT("MHF_Ocelot"), HEROBRINE("MHF_Herobrine"), LAVA_SLIME("MHF_LavaSlime"), MOOSHROOM("MHF_MushroomCow"),
    GOLEM("MHF_Golem"), GHAST("MHF_Ghast"), ENDERMAN("MHF_Enderman"), CAVE_SPIDER("MHF_CaveSpider"),

    CACTUS("MHF_Cactus"), CAKE("MHF_Cake"), CHEST("MHF_Chest"), MELON("MHF_Melon"), LOG("MHF_OakLog"),
    PUMPKIN("MHF_Pumpkin"), TNT("MHF_TNT"), DYNAMITE("MHF_TNT2");

    private static final Base64 base64 = new Base64();
    private String id;

    private Skull(String id)
    {
        this.id = id;
    }

    /**
     * Return a skull that has a custom texture specified by url.
     *
     * @param url skin url
     * @return itemstack
     */
    public static ItemStack getCustomSkull(String url)
    {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        PropertyMap propertyMap = profile.getProperties();
        if (propertyMap == null)
        {
            throw new IllegalStateException("Profile doesn't contain a property map");
        }
        byte[] encodedData = base64.encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        propertyMap.put("textures", new Property("textures", new String(encodedData)));
        ItemStack head = new ItemStack(XMaterial.PLAYER_HEAD.parseMaterial(), 1, (short) 3);
        ItemMeta headMeta = head.getItemMeta();
        Class<?> headMetaClass = headMeta.getClass();
        HeadManager.getField(headMetaClass, "profile", GameProfile.class).set(headMeta, profile);
        head.setItemMeta(headMeta);
        return head;
    }

    /**
     * Return a skull of a player.
     *
     * @param name player's name
     * @return itemstack
     */
    public static ItemStack getPlayerSkull(String name)
    {
        ItemStack itemStack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setOwner(name);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Return the skull's id.
     *
     * @return id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Return the skull of the enum.
     *
     * @return itemstack
     */
    public ItemStack getSkull()
    {
        ItemStack itemStack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setOwner(id);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

}
