package nl.pim16aap2.animatedarchitecture.spigot.util.text;

import net.md_5.bungee.api.chat.BaseComponent;
import nl.pim16aap2.animatedarchitecture.core.text.ITextDecorator;

/**
 * Interface for text decorators used on the Spigot platform.
 */
public interface ITextDecoratorSpigot extends ITextDecorator
{
    /**
     * Applies the decoration of this decorator to the provided component.
     *
     * @param component
     *     The component to decorate.
     */
    void decorateComponent(BaseComponent component);
}
