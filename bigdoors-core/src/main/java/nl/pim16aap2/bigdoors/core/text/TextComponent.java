package nl.pim16aap2.bigdoors.core.text;

import java.util.Collections;
import java.util.List;

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
 * @param decorators
 *     A list of decorators that may be used to add additional decorations to text components. Note that such
 *     decorations are entirely optional.
 * @author Pim
 */
public record TextComponent(String on, String off, List<ITextDecorator> decorators)
{
    public static final TextComponent EMPTY = new TextComponent("", "");

    public TextComponent(String on, String off, List<ITextDecorator> decorators)
    {
        this.on = on;
        this.off = off;
        this.decorators = List.copyOf(decorators);
    }

    public TextComponent(String on, String off)
    {
        this(on, off, Collections.emptyList());
    }

    /**
     * Checks if this text component is empty.
     *
     * @return True if both the on and off strings are empty (i.e. size = 0).
     */
    boolean isEmpty()
    {
        return on.isEmpty() && off.isEmpty() && decorators().isEmpty();
    }
}
