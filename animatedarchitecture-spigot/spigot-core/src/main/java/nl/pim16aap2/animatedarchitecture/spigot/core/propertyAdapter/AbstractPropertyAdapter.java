package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import de.themoep.inventorygui.GuiElement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Represents a property adapter for displaying and interacting with a property in a GUI on the Spigot platform.
 *
 * @param <T>
 *     The type of the property value.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPropertyAdapter<T>
{
    /**
     * The prefix for localization keys for GUI properties.
     */
    public static final String LOCALIZATION_GUI_PREFIX_PROPERTY = "gui.property.";

    /**
     * The suffix for localization keys for property titles.
     */
    public static final String LOCALIZATION_SUFFIX_TITLE = ".title";

    /**
     * The suffix for localization keys for property lore (descriptions).
     */
    public static final String LOCALIZATION_SUFFIX_LORE = ".lore";

    /**
     * The property this adapter is for.
     */
    @Getter
    private final Property<T> property;

    /**
     * Creates a GUI element to represent the property.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The GUI element.
     */
    public abstract GuiElement createGuiElement(PropertyGuiRequest<T> request);

    /**
     * Creates an ItemStack to display this property in the GUI.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The ItemStack to display.
     */
    protected ItemStack createItemStack(PropertyGuiRequest<T> request)
    {
        final ItemStack itemStack = new ItemStack(getMaterial(request));
        final var meta = itemStack.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(getTitle(request));
            meta.setLore(getLore(request));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * Gets the material to use for displaying this property in the GUI.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The material to use.
     */
    protected abstract Material getMaterial(PropertyGuiRequest<T> request);

    /**
     * Gets the display name for this property in the GUI.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The display name.
     */
    protected abstract String getTitle(PropertyGuiRequest<T> request);

    /**
     * Gets the lore (description) lines for this property in the GUI.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The lore lines.
     */
    protected abstract List<String> getLore(PropertyGuiRequest<T> request);
}
