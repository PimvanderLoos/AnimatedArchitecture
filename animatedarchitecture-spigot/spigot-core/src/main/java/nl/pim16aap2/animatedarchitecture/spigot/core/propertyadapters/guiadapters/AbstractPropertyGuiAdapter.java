package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import de.themoep.inventorygui.GuiElement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Represents a property adapter for displaying and interacting with a property in a GUI on the Spigot platform.
 *
 * @param <T>
 *     The type of the property value.
 */
@CustomLog
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPropertyGuiAdapter<T>
{
    /**
     * The property this adapter is for.
     */
    @Getter
    private final Property<T> property;

    /**
     * Gets the current value of the property from the structure in the request.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The current value of the property, or null if not set.
     */
    protected @Nullable T getPropertyValue(PropertyGuiRequest<T> request)
    {
        return request.structure().getPropertyValue(getProperty()).value();
    }

    /**
     * Creates a GUI element to represent the property.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The GUI element.
     */
    public abstract GuiElement createGuiElement(PropertyGuiRequest<T> request);

    /**
     * Creates an ItemStack with the given material, title, and lore.
     *
     * @param material
     *     The material of the item stack.
     * @param title
     *     The title of the item stack.
     * @param lore
     *     The lore (description) lines of the item stack.
     * @return The created ItemStack.
     */
    protected ItemStack createItemStack(
        Material material,
        String title,
        List<String> lore
    )
    {
        final ItemStack itemStack = new ItemStack(material);
        final var meta = itemStack.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(title);
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * Gets the display name for this property in the GUI.
     *
     * @param request
     *     The property GUI request containing context for creating the GUI element.
     * @return The display name.
     */
    protected abstract String getTitle(PropertyGuiRequest<T> request);

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
     * Handles an exceptional situation that occurred while processing a player's action.
     *
     * @param ex
     *     The exception that occurred.
     * @param player
     *     The player involved in the action.
     *     <p>
     *     This player will be notified of the error with a generic error message.
     * @param context
     *     A description of the context in which the exception occurred.
     */
    protected void handleExceptional(Throwable ex, WrappedPlayer player, String context)
    {
        player.sendError("commands.base.error.generic");
        log.atError().withCause(ex).log("Failed to handle action '%s' for player '%s'", context, player);
    }
}
