package nl.pim16aap2.bigdoors.core.text;

/**
 * Represents an interface that is used to render {@link Text} to other formats.
 * <p>
 * For example, to Strings. See {@link StringRenderer}.
 * <p>
 * The renderer receives pieces of either styled or unstyled text in the correct order. For styled text, a renderer may
 * choose to process the embedded {@link ITextDecorator}s if any exist. However, this is not required.
 *
 * @param <T>
 *     The output type of the renderer.
 */
public interface ITextRenderer<T>
{
    /**
     * Processes a piece of unstyled text.
     *
     * @param text
     *     The text to process.
     */
    void process(String text);

    /**
     * Processes a piece of styled text.
     *
     * @param text
     *     The text to process.
     * @param component
     *     The component containing additional information for the text processing.
     */
    void process(String text, TextComponent component);

    /**
     * @return The rendered result.
     */
    T getRendered();

    /**
     * Implementation of a text renderer that can be used to render {@link Text} objects to Strings.
     */
    final class StringRenderer implements ITextRenderer<String>
    {
        private final StringBuilder sb;

        public StringRenderer(int styledSize)
        {
            sb = new StringBuilder(styledSize);
        }

        @Override
        public void process(String text)
        {
            sb.append(text);
        }

        @Override
        public void process(String text, TextComponent component)
        {
            sb.append(component.on()).append(text).append(component.off());
        }

        @Override
        public String getRendered()
        {
            return sb.toString();
        }
    }
}
