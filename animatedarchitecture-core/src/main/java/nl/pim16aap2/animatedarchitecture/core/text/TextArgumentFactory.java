package nl.pim16aap2.animatedarchitecture.core.text;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Contains some quality-of-life features to aid the creation of new {@link TextArgument}s.
 */
public final class TextArgumentFactory
{
    @Getter
    private final ITextComponentFactory textComponentFactory;

    public TextArgumentFactory(ITextComponentFactory textComponentFactory)
    {
        this.textComponentFactory = textComponentFactory;
    }

    private TextArgument newTextArgument(Object argument, @Nullable TextType type)
    {
        return new TextArgument(argument, textComponentFactory.newComponent(type));
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for the {@link TextType#HIGHLIGHT}
     * text type.
     * <p>
     * See {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param argument
     *     The argument.
     * @return The new TextArgument.
     */
    public TextArgument highlight(Object argument)
    {
        return newTextArgument(argument, TextType.HIGHLIGHT);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for the {@link TextType#INFO} text
     * type.
     * <p>
     * See {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param argument
     *     The argument.
     * @return The new TextArgument.
     */
    public TextArgument info(Object argument)
    {
        return newTextArgument(argument, TextType.INFO);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for the {@link TextType#ERROR} text
     * type.
     * <p>
     * See {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param argument
     *     The argument.
     * @return The new TextArgument.
     */
    public TextArgument error(Object argument)
    {
        return newTextArgument(argument, TextType.ERROR);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for the {@link TextType#SUCCESS}
     * text type.
     * <p>
     * See {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param argument
     *     The argument.
     * @return The new TextArgument.
     */
    public TextArgument success(Object argument)
    {
        return newTextArgument(argument, TextType.SUCCESS);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for a clickable argument.
     * <p>
     * See {@link ITextComponentFactory#newClickableTextComponent(TextType, String, String)}.
     *
     * @param argument
     *     The argument.
     * @param type
     *     The type of the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return The new TextArgument.
     */
    public TextArgument clickable(Object argument, @Nullable TextType type, String command, @Nullable String info)
    {
        return new TextArgument(argument, textComponentFactory.newClickableTextComponent(type, command, info));
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for a clickable argument without
     * any information.
     * <p>
     * See {@link ITextComponentFactory#newClickableTextComponent(TextType, String, String)}.
     *
     * @param argument
     *     The argument.
     * @param type
     *     The type of the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @return The new TextArgument.
     */
    public TextArgument clickable(Object argument, @Nullable TextType type, String command)
    {
        return clickable(argument, type, command, null);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for a clickable argument for the
     * {@link TextType#CLICKABLE} text type.
     * <p>
     * See {@link ITextComponentFactory#newClickableTextComponent(TextType, String, String)}.
     *
     * @param argument
     *     The argument.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return The new TextArgument.
     */
    public TextArgument clickable(Object argument, String command, @Nullable String info)
    {
        return clickable(argument, TextType.CLICKABLE, command, info);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for a clickable argument without
     * any information for the {@link TextType#CLICKABLE} text type.
     * <p>
     * See {@link ITextComponentFactory#newClickableTextComponent(TextType, String, String)}.
     *
     * @param argument
     *     The argument.
     * @param command
     *     The command to execute when this text is clicked.
     * @return The new TextArgument.
     */
    public TextArgument clickable(Object argument, String command)
    {
        return clickable(argument, TextType.CLICKABLE, command, null);
    }
}
