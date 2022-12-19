package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.text.ColorScheme;
import nl.pim16aap2.bigdoors.text.Text;

/**
 * Factory class that can be used to create new {@link Text} instances.
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
     * Represents a very simply {@link ITextFactory} that only creates unstyled
     */
    class SimpleTextFactory implements ITextFactory
    {
        private static final ITextFactory INSTANCE = new SimpleTextFactory();

        private final ColorScheme colorScheme = ColorScheme.builder().build();

        private SimpleTextFactory()
        {
        }

        @Override
        public Text newText()
        {
            return new Text(colorScheme);
        }

        private static ITextFactory getInstance()
        {
            return INSTANCE;
        }
    }
}
