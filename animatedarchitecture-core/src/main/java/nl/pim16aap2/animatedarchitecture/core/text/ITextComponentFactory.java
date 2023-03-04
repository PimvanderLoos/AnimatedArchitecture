package nl.pim16aap2.animatedarchitecture.core.text;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a factory for {@link TextComponent}s that can be used to create specialized text components with
 * additional {@link ITextDecorator}s.
 * <p>
 * The default implementations of the various methods create simple text components without adding anything special.
 */
@SuppressWarnings("unused")
public interface ITextComponentFactory
{
    /**
     * Creates a new {@link TextComponent} for a given type.
     *
     * @param type
     *     The type of the text.
     * @return A new component or null to apply no specific decoration to this component.
     */
    default @Nullable TextComponent newComponent(@Nullable TextType type)
    {
        return TextComponent.EMPTY;
    }

    /**
     * Creates a new {@link TextComponent} and attempts to add any required {@link ITextDecorator}s to execute a command
     * when the text is clicked.
     *
     * @param type
     *     The type of the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return A new component or null to apply no specific decoration to this component.
     */
    default @Nullable TextComponent newClickableTextComponent(
        @Nullable TextType type, String command, @Nullable String info)
    {
        return TextComponent.EMPTY;
    }

    /**
     * Creates a new {@link TextComponent} and attempts to add any required {@link ITextDecorator}s to execute a command
     * when the text is clicked.
     *
     * @param type
     *     The type of the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @return A new component or null to apply no specific decoration to this component.
     */
    default @Nullable TextComponent newClickableTextComponent(@Nullable TextType type, String command)
    {
        return newClickableTextComponent(type, command, null);
    }

    /**
     * Simple implementation of the factory interface that only creates basic components.
     */
    final class SimpleTextComponentFactory implements ITextComponentFactory
    {
        public static final ITextComponentFactory INSTANCE = new SimpleTextComponentFactory();

        private SimpleTextComponentFactory()
        {
        }
    }
}
