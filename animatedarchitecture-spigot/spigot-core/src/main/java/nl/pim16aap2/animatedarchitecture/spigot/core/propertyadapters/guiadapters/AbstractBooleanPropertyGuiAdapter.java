package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.EqualsAndHashCode;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Abstract base class for boolean property adapters.
 */
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractBooleanPropertyGuiAdapter extends AbstractPropertyGuiAdapter<Boolean>
{
    /**
     * State key for the true value.
     * <p>
     * This is used to identify the GUI state representing the true value of the boolean property.
     */
    protected static final String STATE_TRUE_KEY = "TRUE";

    /**
     * State key for the false value.
     * <p>
     * This is used to identify the GUI state representing the false value of the boolean property.
     */
    protected static final String STATE_FALSE_KEY = "FALSE";

    protected AbstractBooleanPropertyGuiAdapter(Property<Boolean> property)
    {
        super(property);
    }

    /**
     * Gets the GUI state for the true value of the boolean property.
     * <p>
     * See {@link #getState(PropertyGuiRequest)}.
     *
     * @param stateKey
     *     The state key. This will be {@link #STATE_TRUE_KEY}.
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The GUI state for the true value.
     */
    protected abstract GuiStateElement.State getTrueState(String stateKey, PropertyGuiRequest request);

    /**
     * Gets the GUI state for the false value of the boolean property.
     * <p>
     * See {@link #getState(PropertyGuiRequest)}.
     *
     * @param stateKey
     *     The state key. This will be {@link #STATE_FALSE_KEY}.
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The GUI state for the false value.
     */
    protected abstract GuiStateElement.State getFalseState(String stateKey, PropertyGuiRequest request);

    /**
     * Gets the current state of the boolean property.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The current state of the boolean property.
     */
    protected abstract boolean getState(PropertyGuiRequest request);

    /**
     * Gets the material to use for the given state.
     *
     * @param state
     *     The state of the boolean property.
     * @return The material to use for the given state.
     */
    protected abstract Material getMaterial(boolean state);

    /**
     * Gets the material to use for the given state.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The material to use for the given state.
     */
    protected Material getMaterial(PropertyGuiRequest request)
    {
        return getMaterial(getState(request));
    }

    @Override
    public Material getRemovingMaterial(PropertyGuiRequest request)
    {
        return getMaterial(request);
    }

    @Override
    public Material getAddingMaterial(PropertyGuiRequest request)
    {
        return getMaterial(request);
    }

    @Override
    protected abstract String getTitle(PropertyGuiRequest request);

    protected abstract List<String> getLore(boolean state, PropertyGuiRequest request);

    /**
     * Creates a read-only GUI element for the boolean property.
     * <p>
     * This method is used when a viewer does not have permission to edit the property.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The read-only GUI element.
     */
    protected final GuiElement createReadOnlyElement(PropertyGuiRequest request)
    {
        final ItemStack itemStack = createItemStack(
            getMaterial(getState(request)),
            getTitle(request),
            getLore(getState(request), request)
        );
        return new StaticGuiElement(request.slotChar(), itemStack);
    }

    @Override
    public final GuiElement createGuiElement(PropertyGuiRequest request)
    {
        if (!canEdit(request.permissionLevel()))
        {
            return createReadOnlyElement(request);
        }

        final GuiStateElement element = new GuiStateElement(
            request.slotChar(),
            () -> getStateKey(request),
            getTrueState(STATE_TRUE_KEY, request),
            getFalseState(STATE_FALSE_KEY, request)
        );
        element.setState(getStateKey(request));
        return element;
    }

    private String getStateKey(PropertyGuiRequest request)
    {
        return getState(request) ? STATE_TRUE_KEY : STATE_FALSE_KEY;
    }
}
