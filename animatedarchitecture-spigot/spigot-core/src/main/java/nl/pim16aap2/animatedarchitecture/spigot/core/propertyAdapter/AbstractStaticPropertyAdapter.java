package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Base class for all Spigot static property GUI adapters.
 *
 * @param <T>
 *     The type of the property value.
 */
public abstract class AbstractStaticPropertyAdapter<T> extends AbstractPropertyAdapter<T>
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
     * The localization key for the lore.
     */
    protected final String loreKey;

    /**
     * Creates a new StaticPropertyAdapter.
     * <p>
     * The title and the lore will be retrieved from the localizer using the keys
     * {@code gui.property.{property_key}.title} and {@code gui.property.{property_key}.lore}, where
     * {@code property_key} is the lower-case key of {@link Property#getNamespacedKey()}'s key.
     *
     * @param property
     *     The property this adapter is for.
     * @param material
     *     The material to use for displaying this property in the GUI.
     */
    protected AbstractStaticPropertyAdapter(
        Property<T> property,
        Material material
    )
    {
        super(property);
        this.material = material;

        final String baseKey =
            LOCALIZATION_GUI_PREFIX_PROPERTY + property.getNamespacedKey().getKey().toLowerCase(Locale.ROOT);
        this.loreKey = baseKey + LOCALIZATION_SUFFIX_LORE;
        this.titleKey = baseKey + LOCALIZATION_SUFFIX_TITLE;
    }

    @Override
    public final GuiElement createGuiElement(PropertyGuiRequest<T> request)
    {
        final ItemStack itemStack = createItemStack(request);
        return new StaticGuiElement(request.slotChar(), itemStack);
    }

    @Override
    protected Material getMaterial(PropertyGuiRequest<T> request)
    {
        return material;
    }

    @Override
    protected String getTitle(PropertyGuiRequest<T> request)
    {
        return request.localizer()
            .getMessage(titleKey, String.valueOf(request.propertyValue().value()));
    }

    @Override
    protected List<String> getLore(PropertyGuiRequest<T> request)
    {
        return request.localizer()
            .getMessage(loreKey, String.valueOf(request.propertyValue().value()))
            .lines()
            .toList();
    }
}
