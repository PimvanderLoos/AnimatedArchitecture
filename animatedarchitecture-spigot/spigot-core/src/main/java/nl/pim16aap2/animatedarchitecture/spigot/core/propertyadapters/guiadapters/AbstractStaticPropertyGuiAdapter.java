package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Base class for all Spigot static property GUI adapters.
 *
 * @param <T>
 *     The type of the property value.
 */
public abstract class AbstractStaticPropertyGuiAdapter<T> extends AbstractPropertyGuiAdapter<T>
{
    /**
     * The material to use for displaying this property in the GUI.
     */
    protected final Material material;

    /**
     * The localization key for the title.
     */
    protected final String titleKey;

    /**
     * The localization key for the lore when the user can edit the property.
     */
    protected final String loreEditableKey;

    /**
     * The localization key for the lore when read-only.
     */
    protected final String loreReadOnlyKey;

    /**
     * Creates a new static property GUI adapter.
     * <p>
     * The title and the lore will be retrieved from the localizer using the keys
     * {@code gui.property.{property_key}.title} and {@code gui.property.{property_key}.lore}, where
     * {@code property_key} is the lower-case key of {@link Property#getNamespacedKey()}'s key.
     *
     * @param property
     *     The property this adapter is for.
     * @param material
     *     The material to use for displaying this property in the GUI.
     * @param titleKey
     *     The localization key for the title.
     * @param loreEditableKey
     *     The localization key for the lore when the user can edit the property.
     * @param loreReadOnlyKey
     *     The localization key for the lore when read-only.
     */
    protected AbstractStaticPropertyGuiAdapter(
        Property<T> property,
        Material material,
        String titleKey,
        String loreEditableKey,
        String loreReadOnlyKey
    )
    {
        super(property);
        this.material = material;
        this.loreEditableKey = loreEditableKey;
        this.loreReadOnlyKey = loreReadOnlyKey;
        this.titleKey = titleKey;
    }

    @Override
    public Material getRemovingMaterial(PropertyGuiRequest<T> request)
    {
        return material;
    }

    @Override
    public Material getAddingMaterial(PropertyGuiRequest<T> request)
    {
        return material;
    }

    /**
     * Gets the action to perform when this GUI element is clicked.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @param element
     *     The element that this action will be attached to.
     *     <p>
     *     This can be used to modify the element in the action, e.g., to update its item stack.
     * @return The action to perform, or null if no action should be performed.
     */
    protected GuiElement.@Nullable Action getAction(
        PropertyGuiRequest<T> request,
        StaticGuiElement element
    )
    {
        return null;
    }

    @Override
    public final GuiElement createGuiElement(PropertyGuiRequest<T> request)
    {
        final String title = getTitle(request);
        final List<String> lore = getLore(request);
        final ItemStack itemStack = createItemStack(material, title, lore);

        final StaticGuiElement element = new StaticGuiElement(request.slotChar(), itemStack);

        final GuiElement.Action action = getAction(request, element);
        element.setAction(action);

        return element;
    }

    @Override
    protected String getTitle(PropertyGuiRequest<T> request)
    {
        return request.localizer()
            .getMessage(titleKey, String.valueOf(getPropertyValue(request)));
    }

    protected List<String> getLore(PropertyGuiRequest<T> request)
    {
        final String loreKey = canEdit(request.permissionLevel())
            ? loreEditableKey
            : loreReadOnlyKey;

        return request.localizer()
            .getMessage(loreKey, String.valueOf(getPropertyValue(request)))
            .lines()
            .toList();
    }
}
