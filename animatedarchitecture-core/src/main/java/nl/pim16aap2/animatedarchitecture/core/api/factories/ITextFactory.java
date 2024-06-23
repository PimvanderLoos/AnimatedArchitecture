package nl.pim16aap2.animatedarchitecture.core.api.factories;


import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;

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
     * @return The new text instance.
     */
    Text newText();

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
        public Text newText()
        {
            return new Text(textComponentFactory);
        }

        private static ITextFactory getInstance()
        {
            return INSTANCE;
        }
    }
}
