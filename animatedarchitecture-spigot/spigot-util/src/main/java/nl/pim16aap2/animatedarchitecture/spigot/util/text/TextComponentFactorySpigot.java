package nl.pim16aap2.animatedarchitecture.spigot.util.text;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.chat.BaseComponent;
import nl.pim16aap2.animatedarchitecture.core.text.ColorScheme;
import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextComponent;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of the text component factory for the Spigot platform.
 */
@ToString
@EqualsAndHashCode
public final class TextComponentFactorySpigot implements ITextComponentFactory
{
    private final ColorScheme<BaseComponent> colorScheme;

    public TextComponentFactorySpigot(ColorScheme<BaseComponent> colorScheme)
    {
        this.colorScheme = colorScheme;
    }

    private StyledTextDecorator newColoredTextDecorator(@Nullable TextType type)
    {
        return new StyledTextDecorator(colorScheme.getStyle(type));
    }

    @Override
    public TextComponent newComponent(@Nullable TextType type)
    {
        return new TextComponent(newColoredTextDecorator(type));
    }

    @Override
    public TextComponent newClickableTextComponent(
        @Nullable TextType type, String command, @Nullable String info)
    {
        return new TextComponent(
            new ClickableTextDecorator(command, info),
            newColoredTextDecorator(type));
    }
}
