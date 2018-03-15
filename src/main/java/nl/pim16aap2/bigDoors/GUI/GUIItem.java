package nl.pim16aap2.bigDoors.GUI;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUIItem
{
	private ItemStack is;
	public GUIItem(Material mat, String name, ArrayList<String> lore, int count, byte data)
	{
		this.is = new ItemStack(mat, count, data);
		construct(name, lore);
	}
	
	public GUIItem(Material mat, String name, ArrayList<String> lore, int count)
	{
		this.is = new ItemStack(mat, count);
		construct(name, lore);
	}
	
	private void construct(String name, ArrayList<String> lore)
	{
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		is.setItemMeta(meta);
	}
	
	public ItemStack getItemStack()
	{
		return is;
	}
}
