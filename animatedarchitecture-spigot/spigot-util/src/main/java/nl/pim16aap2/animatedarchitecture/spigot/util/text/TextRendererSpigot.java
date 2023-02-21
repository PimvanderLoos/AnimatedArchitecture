package nl.pim16aap2.animatedarchitecture.spigot.util.text;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import nl.pim16aap2.animatedarchitecture.core.text.ITextRenderer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextComponent;

/**
 * Implementation of a text renderer that can be used by a {@link Text} object to render the text to
 * {@link BaseComponent[]}.
 * <p>
 * See {@link Text#render(ITextRenderer)}.
 * <p>
 * This renderer supports rendering any implementation of {@link ITextDecoratorSpigot}.
 */
public class TextRendererSpigot implements ITextRenderer<BaseComponent[]>
{
    private final ComponentBuilder builder = new ComponentBuilder();

    @Override
    public void process(String text)
    {
        builder.append(text);
    }

    @Override
    public void process(String text, TextComponent component)
    {
        final BaseComponent textComponent = new net.md_5.bungee.api.chat.TextComponent(text);

        getDecoratorsOfType(ITextDecoratorSpigot.class, component)
            .forEach(decorator -> decorator.decorateComponent(textComponent));

        builder.append(textComponent, ComponentBuilder.FormatRetention.NONE);
    }

    @Override
    public BaseComponent[] getRendered()
    {
        return builder.create();
    }
}
