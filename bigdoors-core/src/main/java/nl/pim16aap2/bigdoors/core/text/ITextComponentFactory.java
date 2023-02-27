package nl.pim16aap2.bigdoors.core.text;

/**
 * Represents a factory for {@link TextComponent}s that can be used to create specialized text components with
 * additional {@link ITextDecorator}s.
 * <p>
 * The default implementations of the various methods create simple text components without adding anything special.
 */
public interface ITextComponentFactory
{
    /**
     * Can an existing text component.
     * <p>
     * If no updates are required, this method may return the original component.
     *
     * @param textComponent
     *     The component that may be updated.
     * @return A new component, or original component if no modifications are necessary.
     */
    default TextComponent updateComponent(TextComponent textComponent)
    {
        return textComponent;
    }

    /**
     * Simple implementation of the factory interface that only creates basic components.
     */
    final class SimpleTextComponentFactory implements ITextComponentFactory
    {}
}
