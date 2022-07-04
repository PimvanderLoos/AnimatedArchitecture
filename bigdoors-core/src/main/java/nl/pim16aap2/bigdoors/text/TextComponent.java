package nl.pim16aap2.bigdoors.text;

import lombok.Getter;

/**
 * Represents a component in a piece of text. This can be a style such as "bold", or "green" or it can contain more
 * data.
 * <p>
 * Every component is stored by its enable and disable values. E.g. {@code on: <it>, off: </it>}.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@Getter
public final class TextComponent
{
    /**
     * The String that is used to enable this component. E.g. {@code <it>}.
     */
    private final String on;

    /**
     * The String that is used to disable this component. E.g. {@code </it>}.
     */
    private final String off;

    public static final TextComponent EMPTY = new TextComponent("", "");

    /**
     * Creates a new text style.
     *
     * @param on
     *     The String that is used to enable this component. E.g. {@code <it>}.
     * @param off
     *     The String that is used to disable this component. E.g. {@code </it>}.
     */
    public TextComponent(String on, String off)
    {
        this.on = on;
        this.off = off;
    }

    /**
     * Creates a new text component without any 'off' value. This is useful when using
     * {@link ColorScheme.ColorSchemeBuilder#setDefaultDisable(String)} as that will set the default value.
     *
     * @param on
     *     The String to enable this component.
     */
    public TextComponent(String on)
    {
        this(on, "");
    }
}
