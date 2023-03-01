package nl.pim16aap2.bigdoors.core.text;

import nl.pim16aap2.bigdoors.core.util.Util;

import java.util.Collections;
import java.util.List;

/**
 * Represents a component in a piece of text that has zero or more {@link ITextDecorator} to add features such as colors
 * or clickable links, etc.
 *
 * @param decorators
 *     A list of decorators that may be used to add additional decorations to text components. Note that such
 *     decorations are entirely optional. The renderer used for rendering the text is free to determine which decorates
 *     are supported.
 * @author Pim
 */
public record TextComponent(List<ITextDecorator> decorators)
{
    /**
     * Text component that does not contain any decorators.
     */
    public static final TextComponent EMPTY = new TextComponent();

    public TextComponent(List<ITextDecorator> decorators)
    {
        this.decorators = List.copyOf(decorators);
    }

    public TextComponent(ITextDecorator... decorators)
    {
        this(List.of(decorators));
    }

    public TextComponent()
    {
        this(Collections.emptyList());
    }

    /**
     * Creates a new text component with additional decorators.
     *
     * @param append
     *     The decorators to append.
     * @return The new text component.
     */
    public TextComponent withDecorators(List<ITextDecorator> append)
    {
        return withDecorators(append.toArray(ITextDecorator[]::new));
    }

    /**
     * Creates a new text component with additional decorators.
     *
     * @param append
     *     The decorators to append.
     * @return The new text component.
     */
    public TextComponent withDecorators(ITextDecorator... append)
    {
        // Use arrays to avoid having to create multiple lists.
        // Now we only have 1 List.of call, meaning it won't create
        // another copy in the ctor.
        final ITextDecorator[] base = this.decorators.toArray(ITextDecorator[]::new);
        final ITextDecorator[] merged = Util.concatArrays(base, append);
        return new TextComponent(List.of(merged));
    }

    /**
     * Checks if this text component is empty.
     *
     * @return True if both the on and off strings are empty (i.e. size = 0).
     */
    boolean isEmpty()
    {
        return this.decorators().isEmpty();
    }
}
