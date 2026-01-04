package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;

/**
 * Represents a named GUI element.
 *
 * @param name
 *     The name of the GUI element.
 * @param element
 *     The GUI element.
 */
public record NamedGuiElement(
    String name,
    GuiElement element
)
{
}
