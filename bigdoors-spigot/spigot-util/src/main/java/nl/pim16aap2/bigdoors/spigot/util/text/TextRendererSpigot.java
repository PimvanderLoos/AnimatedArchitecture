package nl.pim16aap2.bigdoors.spigot.util.text;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import nl.pim16aap2.bigdoors.core.text.ITextRenderer;
import nl.pim16aap2.bigdoors.core.text.Text;
import nl.pim16aap2.bigdoors.core.text.TextComponent;

import java.util.stream.Stream;

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

    private Stream<ITextDecoratorSpigot> getDecorators(TextComponent component)
    {
        return component.decorators().stream()
                        .filter(ITextDecoratorSpigot.class::isInstance)
                        .map(ITextDecoratorSpigot.class::cast);
    }

    private BaseComponent[] toBaseComponents(String text, TextComponent component)
    {
        return net.md_5.bungee.api.chat.TextComponent.fromLegacyText(component.on() + text + component.off());
    }

    @Override
    public void process(String text, TextComponent component)
    {
        final BaseComponent[] components = toBaseComponents(text, component);
        getDecorators(component).forEach(decorator -> decorator.decorateComponents(components));
        builder.append(components, ComponentBuilder.FormatRetention.NONE);
    }

    @Override
    public BaseComponent[] getRendered()
    {
        return builder.create();
    }
}
