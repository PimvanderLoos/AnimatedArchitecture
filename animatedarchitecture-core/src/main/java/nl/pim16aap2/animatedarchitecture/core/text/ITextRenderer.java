package nl.pim16aap2.animatedarchitecture.core.text;

import java.util.stream.Stream;

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
     * Retrieves a stream of all decorators that are an instance of a given type.
     * <p>
     * This stream is a filtered (and cast) subset of the decorators in the component.
     *
     * @param clz
     *     The decorator class whose instances to retrieve.
     * @param component
     *     The component whose decorators to retrieve.
     * @param <U>
     *     The type of decorator to retrieve.
     * @return The stream of text decorators of the provided type.
     */
    default <U extends ITextDecorator> Stream<U> getDecoratorsOfType(Class<U> clz, TextComponent component)
    {
        return component.decorators().stream()
                        .filter(clz::isInstance)
                        .map(clz::cast);
    }

    /**
     * Implementation of a text renderer that can be used to render {@link Text} objects to Strings.
     */
    class StringRenderer implements ITextRenderer<String>
    {
        private final StringBuilder sb;

        public StringRenderer(int size)
        {
            sb = new StringBuilder(size);
        }

        @Override
        public void process(String text)
        {
            sb.append(text);
        }

        @Override
        public void process(String text, TextComponent component)
        {
            process(text);
        }

        @Override
        public String getRendered()
        {
            return sb.toString();
        }
    }
}
