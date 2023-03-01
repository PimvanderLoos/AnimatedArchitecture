package nl.pim16aap2.bigdoors.spigot.util.text;

import lombok.EqualsAndHashCode;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

/**
 * Represents a text decorator for the Spigot platform that can add styles to text.
 * <p>
 * Such styles include things like color, italics, etc.
 */
@EqualsAndHashCode
public final class StyledTextDecorator implements ITextDecoratorSpigot
{
    /**
     * The base component to use as template for the style of the section. Its formatting will be copied to input
     * components.
     */
    private final BaseComponent style;

    public StyledTextDecorator(BaseComponent style)
    {
        this.style = style;
    }

    @Override
    public void decorateComponent(BaseComponent component)
    {
        component.copyFormatting(style, ComponentBuilder.FormatRetention.ALL, false);
    }
}
