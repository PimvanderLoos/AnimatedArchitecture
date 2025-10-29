package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.bukkit.Material;

import java.util.List;

/**
 * Represents a property adapter for {@link Property#OPEN_STATUS}.
 */
@Singleton
public final class PropertyAdapterOpenStatus extends AbstractBooleanPropertyAdapter
{
    public static final Property<Boolean> PROPERTY = Property.OPEN_STATUS;

    public static final Material MATERIAL_OPEN = Material.OAK_DOOR;
    public static final Material MATERIAL_CLOSED = Material.DARK_OAK_DOOR;

    public static final String TITLE_KEY = "gui.property.open_status.title";
    public static final String LORE_EDITABLE_KEY = "gui.property.open_status.lore.editable";
    public static final String LORE_READONLY_KEY = "gui.property.open_status.lore.readonly";

    @Inject
    public PropertyAdapterOpenStatus()
    {
        super(PROPERTY);
    }

    private boolean isOpen(IPropertyValue<Boolean> propertyValue)
    {
        return Boolean.TRUE.equals(propertyValue.value());
    }

    @Override
    protected Material getMaterial(PropertyGuiRequest<Boolean> request)
    {
        return isOpen(request.propertyValue()) ? MATERIAL_OPEN : MATERIAL_CLOSED;
    }

    @Override
    protected String getTitle(PropertyGuiRequest<Boolean> request)
    {
        return request.localizer().getMessage(TITLE_KEY);
    }

    @Override
    protected List<String> getLore(PropertyGuiRequest<Boolean> request)
    {
        final ILocalizer localizer = request.localizer();

        final String statusKey = isOpen(request.propertyValue()) ?
            "constants.open_status.open" :
            "constants.open_status.closed";
        final String localizedStatus = localizer.getMessage(statusKey);

        final String loreKey = canEdit(request.permissionLevel())
            ? LORE_EDITABLE_KEY
            : LORE_READONLY_KEY;

        return localizer.getMessage(loreKey, localizedStatus)
            .lines()
            .toList();
    }
}
