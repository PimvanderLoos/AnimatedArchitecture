package nl.pim16aap2.animatedarchitecture.core.api.factories;


import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Factory class that can be used to create new {@link Text} instances.
 * <p>
 * Certain things like colors and styles are set differently in different platforms. This factory allows for easy
 * creation of texts that are styled according to the platform.
 * <p>
 * If you only need unstyled texts, you can use {@link #getSimpleTextFactory()}. This will return a simple factory that
 * only creates unstyled texts.
 */
public interface ITextFactory
{
    /**
     * Creates a new {@link Text} instance.
     *
     * @param personalizedLocalizer
     *     the string localizer to use for this text.
     * @return The new text instance.
     */
    Text newText(@Nullable PersonalizedLocalizer personalizedLocalizer);

    default Text newText()
    {
        return newText(null);
    }

    /**
     * Gets a simple {@link ITextFactory} that only produces unstyled texts.
     */
    static ITextFactory getSimpleTextFactory()
    {
        return SimpleTextFactory.getInstance();
    }

    /**
     * Represents a very simply {@link ITextFactory} that only creates unstyled texts.
     */
    final class SimpleTextFactory implements ITextFactory
    {
        private static final ITextFactory INSTANCE = new SimpleTextFactory();

        private final ITextComponentFactory textComponentFactory =
            ITextComponentFactory.SimpleTextComponentFactory.INSTANCE;

        private SimpleTextFactory()
        {
        }

        @Override
        public Text newText(@Nullable PersonalizedLocalizer personalizedLocalizer)
        {
            return new Text(textComponentFactory, personalizedLocalizer);
        }

        private static ITextFactory getInstance()
        {
            return INSTANCE;
        }
    }
}
