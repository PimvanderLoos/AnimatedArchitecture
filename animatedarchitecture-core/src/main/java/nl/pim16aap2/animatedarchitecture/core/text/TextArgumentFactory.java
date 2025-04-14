package nl.pim16aap2.animatedarchitecture.core.text;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.jetbrains.annotations.Nullable;

/**
 * Contains some quality-of-life features to aid the creation of new {@link TextArgument}s.
 */
public final class TextArgumentFactory
{
    @Getter
    private final ITextComponentFactory textComponentFactory;
    private final @Nullable PersonalizedLocalizer personalizedLocalizer;

    public TextArgumentFactory(
        ITextComponentFactory textComponentFactory,
        @Nullable PersonalizedLocalizer personalizedLocalizer)
    {
        this.textComponentFactory = textComponentFactory;
        this.personalizedLocalizer = personalizedLocalizer;
    }

    /**
     * Localizes the provided key using the {@link PersonalizedLocalizer} of this factory.
     *
     * @param key
     *     The key to localize.
     * @return The localized key or the key itself if the localizer is null.
     */
    public String localized(String key)
    {
        return personalizedLocalizer == null ? key : personalizedLocalizer.getMessage(key);
    }

    /**
     * Shortcut for creating a new {@link TextArgument} with a {@link TextComponent} for the provided text type.
     * <p>
     * See {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param argument
     *     The argument.
     * @param type
     *     The text type to use for the argument.
     * @return The new TextArgument.
     */
    public TextArgument newTextArgument(@Nullable Object argument, @Nullable TextType type)
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
    public TextArgument highlight(@Nullable Object argument)
    {
        return newTextArgument(argument, TextType.HIGHLIGHT);
    }

    /**
     * Shortcut for localizing a String using {@link #localized(String)} and creating a new {@link TextArgument} with a
     * {@link TextComponent} for the {@link TextType#HIGHLIGHT} text type.
     *
     * @param key
     *     The key to localize and whose value to highlight.
     * @return The new TextArgument.
     */
    public TextArgument localizedHighlight(String key)
    {
        return highlight(localized(key));
    }

    /**
     * Shortcut for localizing a structure type using {@link #localized(String)} and creating a new {@link TextArgument}
     * with a {@link TextComponent} for the {@link TextType#HIGHLIGHT} text type.
     *
     * @param structureType
     *     The structure type to localize and highlight.
     * @return The new TextArgument.
     */
    public TextArgument localizedHighlight(StructureType structureType)
    {
        return highlight(structureType.getLocalizationKey());
    }

    /**
     * Shortcut for localizing a structure type using {@link #localized(String)} and creating a new {@link TextArgument}
     * with a {@link TextComponent} for the {@link TextType#HIGHLIGHT} text type.
     *
     * @param structure
     *     The structure whose type to localize and highlight.
     * @return The new TextArgument.
     */
    public TextArgument localizedHighlight(IStructureConst structure)
    {
        return localizedHighlight(structure.getType());
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
    public TextArgument info(@Nullable Object argument)
    {
        return newTextArgument(argument, TextType.INFO);
    }

    /**
     * Shortcut for localizing a String using {@link #localized(String)} and creating a new {@link TextArgument} with a
     * {@link TextComponent} for the {@link TextType#INFO} text type.
     *
     * @param key
     *     The key to localize and whose value to info.
     * @return The new TextArgument.
     */
    public TextArgument localizedInfo(String key)
    {
        return info(localized(key));
    }

    /**
     * Shortcut for localizing a structure type using {@link #localized(String)} and creating a new {@link TextArgument}
     * with a {@link TextComponent} for the {@link TextType#INFO} text type.
     *
     * @param structureType
     *     The structure type to localize and info.
     * @return The new TextArgument.
     */
    public TextArgument localizedInfo(StructureType structureType)
    {
        return info(structureType.getLocalizationKey());
    }

    /**
     * Shortcut for localizing a structure type using {@link #localized(String)} and creating a new {@link TextArgument}
     * with a {@link TextComponent} for the {@link TextType#INFO} text type.
     *
     * @param structure
     *     The structure whose type to localize and info.
     * @return The new TextArgument.
     */
    public TextArgument localizedInfo(IStructureConst structure)
    {
        return localizedInfo(structure.getType());
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
    public TextArgument error(@Nullable Object argument)
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
    public TextArgument success(@Nullable Object argument)
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
