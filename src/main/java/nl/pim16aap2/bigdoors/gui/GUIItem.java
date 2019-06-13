package nl.pim16aap2.bigdoors.gui;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;

class GUIItem
{
    private ItemStack is;
    private DoorBase door;
    private ArrayList<String> lore;
    private int count;
    private String name;
    private Material mat;
    private DoorOwner doorOwner = null;
    private boolean missingHeadTexture;
    private DoorAttribute attribute = null;
    private Object specialValue;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GUIItem: ");
        if (is != null)
            sb.append("\nItemStack: " + is.toString());
        if (doorOwner != null)
            sb.append("\nDoorOwner: " + doorOwner.toString());
        if (specialValue != null)
            sb.append("\nSpecialValue: " + specialValue);
        if (lore != null)
            sb.append("\nLore: " + lore.toString());
        if (name != null)
            sb.append("\nName: " + name);

        return sb.toString();
    }

    public GUIItem(Material mat, String name, ArrayList<String> lore, int count, Object specialValue)
    {
        this.name = name;
        this.mat = mat;
        this.lore = lore;
        this.count = count;
        this.specialValue = specialValue;
        is = new ItemStack(mat, count);
        construct();
    }

    public GUIItem(Material mat, String name, ArrayList<String> lore, int count)
    {
        this(mat, name, lore, count, null);
    }

    public GUIItem(ItemStack is, String name, ArrayList<String> lore, int count, Object specialValue)
    {
        this.name = name;
        this.lore = lore;
        this.count = count;
        this.is = is;
        this.specialValue = specialValue;
        is.setAmount(count);
        construct();
    }

    public GUIItem(ItemStack is, String name, ArrayList<String> lore, int count)
    {
        this(is, name, lore, count, null);
    }

    public GUIItem(BigDoors plugin, DoorOwner doorOwner, Player guiOwner)
    {
        this.doorOwner = doorOwner;
        count = doorOwner.getPermission() == 0 ? 1 : doorOwner.getPermission();
        name = doorOwner.getPlayerName() == null ? doorOwner.getPlayerUUID().toString() : doorOwner.getPlayerName();

//        Location loc = guiOwner.getLocation();
//        is = plugin.getPlayerHead(doorOwner.getPlayerUUID(), doorOwner.getPlayerName(), loc.getBlockX(),
//                                  loc.getBlockY(), loc.getBlockZ(), guiOwner);
        is = plugin.getPlayerHead(doorOwner.getPlayerUUID(), doorOwner.getPlayerName(), Bukkit.getOfflinePlayer(doorOwner.getPlayerUUID()));
        if (is == null)
        {
            is = new ItemStack(Material.PLAYER_HEAD, 1);
            missingHeadTexture = true;
        }
        else
            missingHeadTexture = false;

        name = doorOwner.getPlayerName();
        lore = null;
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

    public void setDoor(DoorBase door)
    {
        this.door = door;
    }

    public DoorBase getDoor()
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

    public DoorOwner getDoorOwner()
    {
        return doorOwner;
    }

    public Object getSpecialValue()
    {
        return specialValue;
    }

    public void setSpecialValue(Object specialValue)
    {
        this.specialValue = specialValue;
    }
}
