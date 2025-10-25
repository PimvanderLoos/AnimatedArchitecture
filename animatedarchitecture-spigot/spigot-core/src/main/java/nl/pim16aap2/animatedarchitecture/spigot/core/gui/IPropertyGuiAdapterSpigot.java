package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import nl.pim16aap2.animatedarchitecture.core.api.IPropertyGuiAdapter;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Spigot-specific extension of {@link IPropertyGuiAdapter} that provides GUI representation using Bukkit/Spigot APIs.
 *
 * @param <T>
 *     The type of the property value.
 */
public interface IPropertyGuiAdapterSpigot<T> extends IPropertyGuiAdapter<T>
{
    /**
     * Gets the material to use for displaying this property in the GUI.
     *
     * @param propertyValue
     *     The current value of the property.
     * @return The material to use.
     */
    Material getMaterial(IPropertyValue<T> propertyValue);

    /**
     * Gets the display name for this property in the GUI.
     *
     * @param propertyValue
     *     The current value of the property.
     * @param viewer
     *     The command sender viewing the property.
     * @return The display name.
     */
    String getDisplayName(IPropertyValue<T> propertyValue, ICommandSender viewer);

    /**
     * Gets the lore (description) lines for this property in the GUI.
     *
     * @param propertyValue
     *     The current value of the property.
     * @param viewer
     *     The command sender viewing the property.
     * @return The lore lines.
     */
    List<String> getLore(IPropertyValue<T> propertyValue, ICommandSender viewer);

    /**
     * Creates an ItemStack to display this property in the GUI.
     *
     * @param propertyValue
     *     The current value of the property.
     * @param viewer
     *     The command sender viewing the property.
     * @return The ItemStack to display.
     */
    default ItemStack createItemStack(IPropertyValue<T> propertyValue, ICommandSender viewer)
    {
        final ItemStack itemStack = new ItemStack(getMaterial(propertyValue));
        final var meta = itemStack.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(getDisplayName(propertyValue, viewer));
            meta.setLore(getLore(propertyValue, viewer));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
