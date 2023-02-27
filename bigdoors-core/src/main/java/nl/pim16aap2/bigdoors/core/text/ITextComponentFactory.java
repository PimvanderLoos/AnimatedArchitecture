package nl.pim16aap2.bigdoors.core.text;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a factory for {@link TextComponent}s that can be used to create specialized text components with
 * additional {@link ITextDecorator}s.
 * <p>
 * The default implementations of the various methods create simple text components without adding anything special.
 */
public interface ITextComponentFactory
{
    /**
     * Updates an existing text component.
     * <p>
     * If no updates are required, this method may return the original component.
     *
     * @param textComponent
     *     The original text component.
     * @return A new component, or the original component if no modifications are necessary.
     */
    default @Nullable TextComponent updateComponent(TextComponent textComponent)
    {
        return textComponent;
    }

    /**
     * Updates an existing text component to attempt to add any required {@link ITextDecorator}s to execute a command
     * when the text is clicked.
     * <p>
     * If no updates are required, this method may return the original component.
     *
     * @param textComponent
     *     The original text component.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return A new component, or the original component if it could not be updated..
     */
    @SuppressWarnings("unused")
    default @Nullable TextComponent updateComponentWithCommand(
        TextComponent textComponent, String command, @Nullable String info)
    {
        return textComponent;
    }

    /**
     * Simple implementation of the factory interface that only creates basic components.
     */
    final class SimpleTextComponentFactory implements ITextComponentFactory
    {}
}
