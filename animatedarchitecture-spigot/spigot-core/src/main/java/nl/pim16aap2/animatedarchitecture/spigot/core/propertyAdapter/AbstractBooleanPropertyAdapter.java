package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import org.bukkit.inventory.ItemStack;

/**
 * Abstract base class for boolean property adapters.
 */
public abstract class AbstractBooleanPropertyAdapter extends AbstractPropertyAdapter<Boolean>
{
    protected AbstractBooleanPropertyAdapter(Property<Boolean> property)
    {
        super(property);
    }

    /**
     * Determines if the property can be edited based on the given permission level.
     *
     * @param permissionLevel
     *     The permission level to check.
     * @return True if the property can be edited, false otherwise.
     */
    protected boolean canEdit(PermissionLevel permissionLevel)
    {
        return getProperty().hasAccessLevel(permissionLevel, PropertyAccessLevel.EDIT);
    }

    /**
     * Creates a read-only GUI element for the boolean property.
     * <p>
     * This method is used when a viewer does not have permission to edit the property.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The read-only GUI element.
     */
    protected final GuiElement createReadOnlyElement(PropertyGuiRequest<Boolean> request)
    {
        final ItemStack itemStack = createItemStack(request);
        return new StaticGuiElement(request.slotChar(), itemStack);
    }

    @Override
    public final GuiElement createGuiElement(PropertyGuiRequest<Boolean> request)
    {
        return createReadOnlyElement(request);
//        final var localizer = viewer.getPersonalizedLocalizer();
//        final GuiStateElement element = new GuiStateElement(
//            slotChar,
//            () -> structure.isLocked() ? "isLocked" : "isUnlocked",
//            new GuiStateElement.State(
//                change -> lockButtonExecute(true, change, structure, viewer),
//                "isLocked",
//                new ItemStack(Material.RED_STAINED_GLASS_PANE),
//                localizer.getMessage(
//                    "gui.info_page.attribute.unlock",
//                    localizer.getMessage(structure.getType().getLocalizationKey()))
//            ),
//            new GuiStateElement.State(
//                change -> lockButtonExecute(false, change, structure, viewer),
//                "isUnlocked",
//                new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
//                localizer.getMessage(
//                    "gui.info_page.attribute.lock",
//                    localizer.getMessage(structure.getType().getLocalizationKey()))
//            )
//        );
//        element.setState(structure.isLocked() ? "isLocked" : "isUnlocked");
//        return element;
    }
}
