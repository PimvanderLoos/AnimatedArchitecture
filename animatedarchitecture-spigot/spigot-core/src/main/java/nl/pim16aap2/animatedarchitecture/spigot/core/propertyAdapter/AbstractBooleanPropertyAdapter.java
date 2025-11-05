package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import lombok.EqualsAndHashCode;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;

/**
 * Abstract base class for boolean property adapters.
 */
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractBooleanPropertyAdapter extends AbstractPropertyAdapter<Boolean>
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

    protected AbstractBooleanPropertyAdapter(Property<Boolean> property)
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
    protected abstract GuiStateElement.State getTrueState(String stateKey, PropertyGuiRequest<Boolean> request);

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
    protected abstract GuiStateElement.State getFalseState(String stateKey, PropertyGuiRequest<Boolean> request);

    /**
     * Gets the current state of the boolean property.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The current state of the boolean property.
     */
    protected abstract boolean getState(PropertyGuiRequest<Boolean> request);

//    /**
//     * Creates a read-only GUI element for the boolean property.
//     * <p>
//     * This method is used when a viewer does not have permission to edit the property.
//     *
//     * @param request
//     *     The property GUI request containing context for creating the GUI element.
//     * @return The read-only GUI element.
//     */
//    protected final GuiElement createReadOnlyElement(PropertyGuiRequest<Boolean> request)
//    {
//        final ItemStack itemStack = createItemStack(request);
//        return new StaticGuiElement(request.slotChar(), itemStack);
//    }

    @Override
    public final GuiElement createGuiElement(PropertyGuiRequest<Boolean> request)
    {
//        if (!canEdit(request.permissionLevel()))
//        {
//            return createReadOnlyElement(request);
//        }

        final GuiStateElement element = new GuiStateElement(
            request.slotChar(),
            () -> getStateKey(request),
            getTrueState(STATE_TRUE_KEY, request),
            getFalseState(STATE_FALSE_KEY, request)
        );
        element.setState(getStateKey(request));
        return element;
    }

    private String getStateKey(PropertyGuiRequest<Boolean> request)
    {
        return getState(request) ? STATE_TRUE_KEY : STATE_FALSE_KEY;
    }
}
