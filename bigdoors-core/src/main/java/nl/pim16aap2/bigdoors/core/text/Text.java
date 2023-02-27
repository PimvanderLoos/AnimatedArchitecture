package nl.pim16aap2.bigdoors.core.text;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Represents a piece of text with styled sections.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class Text
{
    /**
     * The {@link ColorScheme} used to add styles to sections of this text.
     */
    private final ColorScheme colorScheme;

    /**
     * The {@link StringBuilder} backing this {@link Text} object. All strings appended to this {@link Text} will be
     * stored here.
     */
    private final StringBuilder stringBuilder = new StringBuilder();

    /**
     * The list of {@link StyledSection}s.
     */
    private List<StyledSection> styledSections = new ArrayList<>();

    /**
     * The total size of the string held by this object. This is used by {@link #toString()} to instantiate a
     * {@link StringBuilder} of the right size.
     * <p>
     * This value includes both the size of {@link #stringBuilder} as well as the total size of all the
     * {@link #styledSections}.
     */
    private int styledSize = 0;

    private final ITextComponentFactory textComponentFactory;

    public Text(ColorScheme colorScheme, ITextComponentFactory textComponentFactory)
    {
        this.colorScheme = colorScheme;
        this.textComponentFactory = textComponentFactory;
    }

    public Text(ColorScheme colorScheme)
    {
        this(colorScheme, new ITextComponentFactory.SimpleTextComponentFactory());
    }

    // CopyConstructor
    public Text(Text other)
    {
        this.colorScheme = other.colorScheme;
        this.stringBuilder.append(other.stringBuilder);
        other.styledSections.forEach(section -> this.styledSections.add(new StyledSection(section)));
        this.styledSize = other.styledSize;
        this.textComponentFactory = other.textComponentFactory;
    }

    /**
     * Gets the length of the raw text without any styles applied to it.
     *
     * @return The length of the raw text without any styles applied to it.
     */
    public int getLength()
    {
        return stringBuilder.length();
    }

    /**
     * The length of the text including all the styles.
     *
     * @return The length of the text including all the styles.
     */
    public int getStyledLength()
    {
        return styledSize;
    }

    /**
     * Gets a subsection from this {@link Text}.
     * <p>
     * See {@link StringBuilder#substring(int, int)}.
     * <p>
     * Any styles/decorators that have at least 1 character inside this range will be copied.
     *
     * @param start
     *     The beginning index, inclusive.
     * @param end
     *     The ending index, exclusive.
     * @return The {@link Text} in the given range.
     */
    @Contract("_, _ -> this")
    public Text subsection(int start, int end)
    {
        if (start == 0 && end == stringBuilder.length())
            return this;

        if (end <= start)
            throw new RuntimeException(String.format("The end (%d) of a substring cannot be before it (%d)!",
                                                     end, start));

        if (start < 0 || end > stringBuilder.length())
            throw new RuntimeException(String.format("Range [%d %d] out of bounds for range: [0 %d]!",
                                                     start, end, stringBuilder.length()));

        final String string = stringBuilder.substring(start, end);
        final Text newText = new Text(colorScheme);
        newText.append(string);

        for (final StyledSection section : styledSections)
        {
            if (section.startIndex() >= end)
                break;

            if (section.end() < start)
                continue;

            int length = section.length();

            int startIdx = section.startIndex() - start;
            if (startIdx < 0)
            {
                length += startIdx;
                startIdx = 0;
            }

            if (section.end() > end)
                length -= (section.end() - end);

            if (length <= 0)
                continue;

            newText.styledSections.add(new StyledSection(startIdx, length, section.component()));
        }

        return newText;
    }

    /**
     * Appends some unstyled text to the current text.
     *
     * @param text
     *     The unstyled text to add.
     * @return The current {@link Text} instance.
     */
    @Contract("_ -> this")
    public Text append(String text)
    {
        stringBuilder.append(text);
        styledSize += text.length();
        return this;
    }

    /**
     * Appends some unstyled character to the current text.
     *
     * @param ch
     *     The unstyled character to add.
     * @return The current {@link Text} instance.
     */
    @Contract("_ -> this")
    public Text append(char ch)
    {
        stringBuilder.append(ch);
        ++styledSize;
        return this;
    }

    private void addStyledSection(@Nullable TextComponent component, int textLength)
    {
        if (component == null || component.isEmpty())
            return;
        styledSections.add(new StyledSection(stringBuilder.length(), textLength, component));
        styledSize += component.on().length() + component.off().length();
    }

    /**
     * Appends some styled text to the current text.
     *
     * @param text
     *     The text to add.
     * @param type
     *     The {@link TextType} of the text to add. The {@link #colorScheme} will be used to look up the style
     *     associated with the type. See {@link ColorScheme#getStyle(TextType)}.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _ -> this")
    public Text append(String text, @Nullable TextType type)
    {
        if (type != null)
            addStyledSection(textComponentFactory.updateComponent(colorScheme.getStyle(type)), text.length());
        return append(text);
    }

    /**
     * Appends some styled text to the current text.
     *
     * @param text
     *     The text to add.
     * @param component
     *     The {@link TextComponent} to use for the text that is being added.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _ -> this")
    public Text append(String text, @Nullable TextComponent component)
    {
        addStyledSection(component, text.length());
        return append(text);
    }

    /**
     * Prepends another {@link Text} object to this object, so the other text is placed before the current one.
     * <p>
     * The other {@link Text} instance is not modified.
     *
     * @param other
     *     The {@link Text} to insert before the current {@link Text}.
     * @return The current {@link Text} instance.
     */
    @Contract("_ -> this")
    public Text prepend(Text other)
    {
        styledSections = appendSections(other.getLength(), other.styledSections, styledSections,
                                        (section, offset) -> new StyledSection(section.startIndex + offset,
                                                                               section.length, section.component));
        this.styledSize += other.styledSize;
        stringBuilder.insert(0, other.stringBuilder);
        return this;
    }

    /**
     * Appends the (copied) values of a list to the values of another list into a list.
     *
     * @param offset
     *     The offset of the second set of values.
     * @param first
     *     The first set of values. All values in this list will maintain their index in the new list.
     * @param last
     *     The last set of values. These values will be placed after the first set of values.
     * @param copier
     *     The function that creates a new list entry from the current entry and the offset value (this value is 0 for
     *     the first list).
     * @param <T>
     *     The type of the entries in the list.
     * @return The new list with all the values copied from the first and the last list (in that order).
     */
    private static <T> List<T> appendSections(
        final int offset,
        final List<T> first,
        final List<T> last,
        final BiFunction<T, Integer, T> copier)
    {
        final List<T> ret = new ArrayList<>(first.size() + last.size());
        first.forEach(entry -> ret.add(copier.apply(entry, 0)));
        last.forEach(entry -> ret.add(copier.apply(entry, offset)));
        return ret;
    }

    /**
     * Appends another {@link Text} object to this one.
     * <p>
     * The other {@link Text} object will not be modified.
     *
     * @param other
     *     The other {@link Text} instance to append to the current one.
     * @return The current {@link Text} instance.
     */
    @Contract("_ -> this")
    public Text append(Text other)
    {
        if (other.stringBuilder.length() == 0)
            return this;

        styledSections = appendSections(getLength(), styledSections, other.styledSections,
                                        (section, offset) -> new StyledSection(section.startIndex + offset,
                                                                               section.length, section.component));
        stringBuilder.append(other.stringBuilder);
        styledSize += other.styledSize;
        return this;
    }

    /**
     * Renders this text using the provided text renderer.
     * <p>
     * All sections of this Text object are processed by the renderer.
     *
     * @param renderer
     *     The renderer to use.
     * @param <T>
     *     The output type of the renderer.
     * @return The output of the renderer after processing this Text object.
     */
    public <T> T render(ITextRenderer<T> renderer)
    {
        int lastIdx = 0;
        for (final StyledSection section : styledSections)
        {
            // Process any unstyled text between styled sections.
            if (section.startIndex > lastIdx)
                renderer.process(stringBuilder.substring(lastIdx, section.startIndex));

            renderer.process(stringBuilder.substring(section.startIndex(), section.end()), section.component);
            lastIdx = section.end();
        }

        // Add any trailing text that doesn't have any styles.
        if (lastIdx < stringBuilder.length())
            renderer.process(stringBuilder.substring(lastIdx, stringBuilder.length()));

        return renderer.getRendered();
    }

    @Override
    public String toString()
    {
        if (stringBuilder.length() == 0)
            return "";

        return render(new ITextRenderer.StringRenderer(styledSize));
    }

    /**
     * Gets the plain String without any styles.
     *
     * @return The plain String without any styles.
     */
    public String toPlainString()
    {
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof Text other))
            return false;
        return this.getLength() == other.getLength() &&
            this.getStyledLength() == other.getStyledLength() &&
            Objects.equals(this.colorScheme, other.colorScheme) &&
            Objects.equals(this.styledSections, other.styledSections) &&
            Objects.equals(this.toPlainString(), other.toPlainString());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int hashCode = 1;
        hashCode = hashCode * prime + this.colorScheme.hashCode();
        hashCode = hashCode * prime + this.styledSections.hashCode();
        hashCode = hashCode * prime + this.toPlainString().hashCode();

        return hashCode;
    }

    /**
     * Represents a section in a text that is associated with a certain style.
     *
     * @author Pim
     */
    private record StyledSection(int startIndex, int length, TextComponent component)
    {
        // Copy constructor
        public StyledSection(final StyledSection other)
        {
            this(other.startIndex, other.length, other.component);
        }

        int end()
        {
            return startIndex + length;
        }
    }
}