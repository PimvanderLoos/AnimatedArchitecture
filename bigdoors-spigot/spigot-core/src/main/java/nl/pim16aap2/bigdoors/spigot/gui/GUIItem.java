package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Represents an item in a BigDoors GUI.
 */
class GUIItem
{
    private ItemStack is;
    private AbstractDoorBase door;
    private List<String> lore;
    private int count;
    private String name;
    private DoorOwner doorOwner = null;
    private boolean missingHeadTexture;
    private DoorAttribute attribute = null;
    private Object specialValue;

    /**
     * Constructs a new {@link GUIItem}.
     *
     * @param mat          The material of this item.
     * @param name         The display name of this item.
     * @param lore         The lore of this item.
     * @param count        The number of items on this stack.
     * @param specialValue An unspecified special value that can be used for various purposes.
     */
    GUIItem(Material mat, String name, List<String> lore, int count, Object specialValue)
    {
        this.name = name;
        this.lore = lore;
        this.count = count;
        this.specialValue = specialValue;
        is = new ItemStack(mat, count);
        construct();
    }

    /**
     * Constructs a new {@link GUIItem}.
     *
     * @param mat   The material of this item.
     * @param name  The display name of this item.
     * @param lore  The lore of this item.
     * @param count The number of items on this stack.
     */
    public GUIItem(Material mat, String name, List<String> lore, int count)
    {
        this(mat, name, lore, count, null);
    }

    /**
     * Constructs a new {@link GUIItem}.
     *
     * @param is           The ItemStack to use.
     * @param name         The display name of this item.
     * @param lore         The lore of this item.
     * @param count        The number of items on this stack.
     * @param specialValue An unspecified special value that can be used for various purposes.
     */
    public GUIItem(ItemStack is, String name, List<String> lore, int count, Object specialValue)
    {
        this.name = name;
        this.lore = lore;
        this.count = count;
        this.is = is;
        this.specialValue = specialValue;
        is.setAmount(count);
        construct();
    }

    /**
     * Constructs a new {@link GUIItem}.
     *
     * @param name  The display name of this item.
     * @param lore  The lore of this item.
     * @param count The number of items on this stack.
     */
    public GUIItem(ItemStack is, String name, List<String> lore, int count)
    {
        this(is, name, lore, count, null);
    }

    /**
     * Creates a player head without a special head texture for a given {@link DoorOwner}.
     *
     * @param doorOwner The {@link DoorOwner}.
     */
    public GUIItem(final @NotNull DoorOwner doorOwner)
    {
        this.doorOwner = doorOwner;
        count = Math.max(1, doorOwner.getPermission());
        name = doorOwner.getPlayerName();
        is = new ItemStack(Material.PLAYER_HEAD, 1);
        name = doorOwner.getPlayerName();
        lore = null;
        construct();
    }

    /**
     * Constructs a new ItemStack from the data received in the constructor.
     */
    private void construct()
    {
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(meta);
    }

    /**
     * Checks if this {@link GUIItem}'s is missing its player head texture. It is only applicable if a player head is
     * used.
     *
     * @return True if the player's head texture is missing.
     */
    public boolean missingHeadTexture()
    {
        return missingHeadTexture;
    }

    /**
     * Gets the {@link ItemStack} of this {@link GUIItem}.
     *
     * @return The ItemStack.
     */
    @NotNull
    public ItemStack getItemStack()
    {
        return is;
    }

    /**
     * Gets the {@link DoorAttribute} associated with this {@link GUIItem}.
     *
     * @return The {@link DoorAttribute} associated with this {@link GUIItem}. Returns null if unavailable.
     */
    @NotNull
    public Optional<DoorAttribute> getDoorAttribute()
    {
        return Optional.ofNullable(attribute);
    }

    /**
     * Sets the {@link DoorAttribute} associated with this {@link GUIItem}.
     *
     * @param atr The {@link DoorAttribute} to be associated with this {@link GUIItem}.
     */
    public void setDoorAttribute(final @NotNull DoorAttribute atr)
    {
        attribute = atr;
    }

    /**
     * Gets the {@link AbstractDoorBase} associated with this {@link GUIItem}.
     *
     * @return The {@link AbstractDoorBase} associated with this {@link GUIItem}. Returns null if unavailable.
     */
    @Nullable
    public AbstractDoorBase getDoor()
    {
        return door;
    }

    /**
     * Sets the {@link AbstractDoorBase} associated with this {@link GUIItem}.
     *
     * @param door The {@link AbstractDoorBase} to be associated with this {@link GUIItem}.
     */
    public void setDoor(final @NotNull AbstractDoorBase door)
    {
        this.door = door;
    }

    /**
     * Gets the name of this {@link GUIItem}.
     *
     * @return The name of this {@link GUIItem}.
     */
    @NotNull
    public String getName()
    {
        return name;
    }

    /**
     * Gets the lore of this {@link GUIItem}.
     *
     * @return The lore of this {@link GUIItem}.
     */
    @NotNull
    public List<String> getLore()
    {
        return lore;
    }

    /**
     * Gets the stacksize of this {@link GUIItem}.
     *
     * @return The stacksize of this {@link GUIItem}.
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Gets the {@link DoorOwner} of this {@link GUIItem}.
     *
     * @return The {@link DoorOwner} of this {@link GUIItem}.
     */
    public DoorOwner getDoorOwner()
    {
        return doorOwner;
    }

    /**
     * Gets the special value of this {@link GUIItem}.
     *
     * @return The special value of this {@link GUIItem}.
     */
    public Object getSpecialValue()
    {
        return specialValue;
    }

    /**
     * Sets the special value of this {@link GUIItem}.
     *
     * @param specialValue The special value of this {@link GUIItem}.
     */
    public void setSpecialValue(Object specialValue)
    {
        this.specialValue = specialValue;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GUIItem: ");
        if (is != null)
            sb.append("\nItemStack: ").append(is.toString());
        if (doorOwner != null)
            sb.append("\nDoorOwner: ").append(doorOwner.toString());
        if (specialValue != null)
            sb.append("\nSpecialValue: ").append(specialValue);
        if (lore != null)
            sb.append("\nLore: ").append(lore.toString());
        if (name != null)
            sb.append("\nName: ").append(name);

        return sb.toString();
    }
}
