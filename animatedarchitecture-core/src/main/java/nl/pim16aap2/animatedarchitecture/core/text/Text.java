package nl.pim16aap2.animatedarchitecture.core.text;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a piece of text with styled sections.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class Text implements CharSequence
{
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
     * The factory for the {@link TextComponent} instance used by this Text.
     */
    @Getter
    private final ITextComponentFactory textComponentFactory;

    /**
     * The factory for {@link TextArgument}s.
     */
    @Getter
    private final TextArgumentFactory textArgumentFactory;

    public Text(ITextComponentFactory textComponentFactory)
    {
        this.textComponentFactory = textComponentFactory;
        this.textArgumentFactory = new TextArgumentFactory(textComponentFactory);
    }

    // CopyConstructor
    public Text(Text other)
    {
        this.stringBuilder.append(other.stringBuilder);
        other.styledSections.forEach(section -> this.styledSections.add(new StyledSection(section)));
        this.textComponentFactory = other.textComponentFactory;
        this.textArgumentFactory = other.textArgumentFactory;
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
        final Text newText = new Text(textComponentFactory);
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

    @Contract("_ -> this")
    private Text append0(String text)
    {
        stringBuilder.append(text);
        return this;
    }

    @Contract("_, _ -> this")
    private Text append0(String text, @Nullable TextComponent component)
    {
        if (component != null && (!component.isEmpty()))
            styledSections.add(new StyledSection(stringBuilder.length(), text.length(), component));
        return append0(text);
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
        return append0(text);
    }

    /**
     * Appends some unstyled text to the current text.
     *
     * @param text
     *     The unstyled text to add.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _ -> this")
    public Text append(String text, TextArgument... arguments)
    {
        return append(text, (TextComponent) null, arguments);
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
        return this;
    }

    @SafeVarargs
    private TextArgument[] retrieveArguments(Function<TextArgumentFactory, TextArgument>... argumentRetrievers)
    {
        final TextArgument[] ret = new TextArgument[argumentRetrievers.length];
        for (int idx = 0; idx < ret.length; ++idx)
            ret[idx] = argumentRetrievers[idx].apply(this.textArgumentFactory);
        return ret;
    }

    /**
     * Appends some styled text to the current text.
     *
     * @param text
     *     The text to add.
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _ -> this")
    public Text append(String text, @Nullable TextType type)
    {
        return append(text, textComponentFactory.newComponent(type), new TextArgument[0]);
    }

    /**
     * Appends some styled text to the current text.
     *
     * @param text
     *     The text to add.
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @param arguments
     *     The text arguments to use for variable substitution into the input text.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _, _ -> this")
    public Text append(String text, @Nullable TextType type, TextArgument... arguments)
    {
        return append(text, textComponentFactory.newComponent(type), arguments);
    }

    /**
     * Appends some styled text to the current text.
     *
     * @param text
     *     The text to add.
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @param argumentRetrievers
     *     Retrievers for text arguments to use for variable substitution into the input text.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _, _ -> this")
    @SafeVarargs
    public final Text append(
        String text, @Nullable TextType type, Function<TextArgumentFactory, TextArgument>... argumentRetrievers)
    {
        return append(text, type, retrieveArguments(argumentRetrievers));
    }

    /**
     * Appends some styled text to the current text with some optional argument.
     * <p>
     * For example "Hello, {0}".
     * <p>
     * See {@link MessageFormat}.
     *
     * @param text
     *     The text to add.
     * @param component
     *     The {@link TextComponent} to use for the text that is being added.
     * @param arguments
     *     The arguments to use to format the text.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _, _ -> this")
    public Text append(String text, @Nullable TextComponent component, TextArgument... arguments)
    {
        if (arguments.length == 0)
            return append0(text, component);

        final var processor = new MessageFormatProcessor(text, arguments);
        addStyledSections(component, processor.getSections(), arguments);
        return append0(processor.getFormattedString());
    }

    /**
     * Appends some styled text to the current text with some optional argument.
     * <p>
     * For example "Hello, {0}".
     * <p>
     * See {@link MessageFormat}.
     *
     * @param text
     *     The text to add.
     * @param component
     *     The {@link TextComponent} to use for the text that is being added.
     * @param argumentRetrievers
     *     Retrievers for text arguments to use for variable substitution into the input text.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _, _ -> this")
    @SafeVarargs
    public final Text append(
        String text,
        @Nullable TextComponent component,
        Function<TextArgumentFactory, TextArgument>... argumentRetrievers)
    {
        return append(text, component, retrieveArguments(argumentRetrievers));
    }

    private void addStyledSections(
        @Nullable TextComponent base,
        List<MessageFormatProcessor.MessageFormatSection> messageFormatSections,
        TextArgument... arguments)
    {
        final int currentLength = stringBuilder.length();
        for (final var section : messageFormatSections)
        {
            final @Nullable TextComponent component;
            if (section.argumentIdx() == -1)
                component = base;
            else
                component = arguments[section.argumentIdx()].component();

            if (component == null)
                continue;

            this.styledSections.add(
                new StyledSection(currentLength + section.start(), section.end() - section.start(), component));
        }
    }

    /**
     * Creates a new TextComponent for texts of a specific type.
     * <p>
     * This method is a shortcut for {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @return The new text component if one was created or null if no specific decoration should be applied to the
     * text.
     */
    @Nullable TextComponent newTextComponent(@Nullable TextType type)
    {
        return textComponentFactory.newComponent(type);
    }

    /**
     * Creates a new {@link TextArgument} with a given input String and text type.
     * <p>
     * This method is a shortcut for creating a new text argument using the input String and
     * {@link ITextComponentFactory#newComponent(TextType)}.
     *
     * @param argument
     *     The argument to use for the TextArgument.
     * @param type
     *     The text type to use for the {@link TextComponent}.
     * @return The new text argument.
     */
    public TextArgument newTextArgument(Object argument, TextType type)
    {
        return new TextArgument(argument, newTextComponent(type));
    }

    /**
     * Creates a new TextComponent for clickable texts.
     * <p>
     * This method is a shortcut for {@link ITextComponentFactory#newClickableTextComponent(TextType, String, String)}.
     *
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return The new text component if one was created or null if no specific decoration should be applied to the
     * text.
     */
    @Nullable TextComponent newClickableTextComponent(
        @Nullable TextType type, String command, @Nullable String info)
    {
        return textComponentFactory.newClickableTextComponent(type, command, info);
    }

    /**
     * Creates a new text argument for clickable texts.
     * <p>
     * This method is a shortcut for creating a new argument using the input text and
     * {@link ITextComponentFactory#newClickableTextComponent(TextType, String, String)}.
     *
     * @param argument
     *     The argument to use for the TextArgument.
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return The new text component if one was created or null if no specific decoration should be applied to the
     * text.
     */
    public TextArgument newClickableTextArgument(
        Object argument, @Nullable TextType type, String command, @Nullable String info)
    {
        return new TextArgument(argument, newClickableTextComponent(type, command, info));
    }

    /**
     * Attempts to add clickable text.
     * <p>
     * The clickable text will execute a command when clicked.
     * <p>
     * The result depends on the capabilities of both the registered {@link ITextComponentFactory} and the
     * {@link ITextRenderer}.
     *
     * @param text
     *     The text to add.
     * @param type
     *     The {@link TextType} of the text to add. This is used by any potential decorators to add styling and such to
     *     the text.
     * @param command
     *     The command to execute when this text is clicked.
     * @param info
     *     The optional information String explaining what clicking the text will do.
     * @return The current {@link Text} instance.
     */
    @Contract("_, _, _, _ -> this")
    public Text appendClickableText(String text, @Nullable TextType type, String command, @Nullable String info)
    {
        return append0(text, newClickableTextComponent(type, command, info));
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
    public int length()
    {
        return getLength();
    }

    @Override
    public char charAt(int index)
    {
        return stringBuilder.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return this.subsection(start, end);
    }

    @Override
    public String toString()
    {
        if (stringBuilder.length() == 0)
            return "";

        return render(new ITextRenderer.StringRenderer(this.getLength()));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof Text other))
            return false;

        return this.getLength() == other.getLength() &&
            Objects.equals(this.styledSections, other.styledSections) &&
            Objects.equals(this.textComponentFactory, other.textComponentFactory) &&
            Objects.equals(this.toString(), other.toString());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int hashCode = 1;
        hashCode = hashCode * prime + this.styledSections.hashCode();
        hashCode = hashCode * prime + this.textComponentFactory.hashCode();
        hashCode = hashCode * prime + this.toString().hashCode();

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
