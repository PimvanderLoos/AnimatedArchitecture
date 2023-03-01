package nl.pim16aap2.bigdoors.core.text;

/**
 * Represents the different types of {@link Text} used by CAP. Every type of text can have its own
 * {@link TextComponent}.
 *
 * @author Pim
 */
public final class TextType
{
    public static final TextType ERROR = new TextType();
    public static final TextType INFO = new TextType();
    public static final TextType HIGHLIGHT = new TextType();
    public static final TextType SUCCESS = new TextType();
}
