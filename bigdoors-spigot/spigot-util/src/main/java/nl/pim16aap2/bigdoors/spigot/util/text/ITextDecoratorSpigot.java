package nl.pim16aap2.bigdoors.spigot.util.text;

import net.md_5.bungee.api.chat.BaseComponent;
import nl.pim16aap2.bigdoors.core.text.ITextDecorator;

/**
 * Interface for text decorators used on the Spigot platform.
 */
public interface ITextDecoratorSpigot extends ITextDecorator
{
    /**
     * Decorates an array of base components.
     *
     * @param components
     *     The components to decorate.
     */
    @SuppressWarnings("PMD.UseVarargs")
    void decorateComponents(BaseComponent[] components);
}
