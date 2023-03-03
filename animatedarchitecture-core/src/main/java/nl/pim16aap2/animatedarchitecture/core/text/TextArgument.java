package nl.pim16aap2.animatedarchitecture.core.text;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an argument in a piece of text.
 * <p>
 * For example given a String "Hello there, {0}", the TextArgument can be used to provide the substitution for "{0}".
 *
 * @param text
 *     The text of the argument.
 * @param component
 *     The optional text component to use for the text. This can be used to override the decorations of the surrounding
 *     text for the argument.
 */
public record TextArgument(String text, @Nullable TextComponent component)
{
    @SuppressWarnings("unused")
    public TextArgument(String text)
    {
        this(text, null);
    }
}
