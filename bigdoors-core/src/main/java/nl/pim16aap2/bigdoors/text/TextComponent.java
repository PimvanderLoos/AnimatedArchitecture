package nl.pim16aap2.bigdoors.text;

/**
 * Represents a component in a piece of text. This can be a style such as "bold", or "green" or it can contain more
 * data.
 * <p>
 * Every component is stored by its enable and disable values. E.g. {@code on: <it>, off: </it>}.
 *
 * @param on
 *     The String that is used to enable this component. E.g. "{@code <it>}".
 * @param off
 *     The String that is used to disable this component. E.g. "{@code </it>}".
 * @author Pim
 */

public record TextComponent(String on, String off)
{
    public static final TextComponent EMPTY = new TextComponent("", "");
}
