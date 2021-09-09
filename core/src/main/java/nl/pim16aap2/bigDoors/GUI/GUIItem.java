package nl.pim16aap2.bigDoors.GUI;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorAttribute;
import nl.pim16aap2.bigDoors.util.DoorOwner;
import com.cryptomorin.xseries.XMaterial;

public class GUIItem
{
    private ItemStack is;
    private Door door;
    private ArrayList<String> lore;
    private int count;
    private byte data = 0;
    private String name;
    private Material mat;
    private DoorOwner doorOwner = null;
    private boolean missingHeadTexture;
    private DoorAttribute attribute = null;

    public GUIItem(Material mat, String name, ArrayList<String> lore, int count, byte data)
    {
        this.name = name;
        this.mat = mat;
        this.lore = lore;
        this.count = count;
        this.data = data;
        is = new ItemStack(mat, count, data);
        construct();
    }

    public GUIItem(Material mat, String name, ArrayList<String> lore, int count)
    {
        this.name = name;
        this.mat = mat;
        this.lore = lore;
        this.count = count;
        is = new ItemStack(mat, count);
        construct();
    }

    public GUIItem(ItemStack is, String name, ArrayList<String> lore, int count)
    {
        this.name = name;
        this.lore = lore;
        this.count = count;
        this.is = is;
        is.setAmount(count);
        construct();
    }

    private void construct()
    {
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(meta);
    }

    public boolean missingHeadTexture()
    {
        return missingHeadTexture;
    }

    public ItemStack getItemStack()
    {
        return is;
    }

    public void setDoorAttribute(DoorAttribute atr)
    {
        attribute = atr;
    }

    public DoorAttribute getDoorAttribute()
    {
        return attribute;
    }

    public void setDoor(Door door)
    {
        this.door = door;
    }

    public Door getDoor()
    {
        return door;
    }

    public Material getMaterial()
    {
        return mat;
    }

    public String getName()
    {
        return name;
    }

    public ArrayList<String> getLore()
    {
        return lore;
    }

    public int getCount()
    {
        return count;
    }

    public byte getData()
    {
        return data;
    }

    public DoorOwner getDoorOwner()
    {
        return doorOwner;
    }
}
