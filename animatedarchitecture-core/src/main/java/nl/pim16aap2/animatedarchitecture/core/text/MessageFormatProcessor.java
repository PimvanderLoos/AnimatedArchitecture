package nl.pim16aap2.animatedarchitecture.core.text;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tool that can be used to process Strings containing variables.
 * <p>
 * This class is backed by {@link MessageFormat} for all variable substitution.
 * <p>
 * What makes this class different from the MessageFormat class is that it keeps a list of {@link MessageFormatSection}s
 * that can be used to determine the character ranges of the parts of the output message.
 * <p>
 * For example, given an input String "Hello there, {0}!" with an argument "John Smith", we end up with the following
 * output:
 * <ul>
 *     <li>The formatted String: "Hello there, John Smith!"</li>
 *     <li>The three sections:
 *         <ol>
 *             <li>Start:  0, End: 13, ArgumentIdx: -1. Selection: "Hello there, "</li>
 *             <li>Start: 13, End: 23, ArgumentIdx:  0, Selection: "John Smith"</li>
 *             <li>Start: 23, End: 24, ArgumentIdx: -1, Selection: "!"</li>
 *         </ol>
 *     </li>
 * </ul>
 */
final class MessageFormatProcessor
{
    private static final Locale DEFAULT_LOCALE = Locale.ROOT;

    private final StringBuilder sb;
    private final List<MessageFormatSection> sections = new ArrayList<>();

    public MessageFormatProcessor(String input, Locale locale, @Nullable Object... arguments)
    {
        final var iter = new MessageFormat(input, locale).formatToCharacterIterator(arguments);
        sb = new StringBuilder(iter.getEndIndex());
        processMessageFormatIterator(iter);
    }

    public MessageFormatProcessor(String input, @Nullable Object... arguments)
    {
        this(input, DEFAULT_LOCALE, arguments);
    }

    public MessageFormatProcessor(String input, TextArgument... arguments)
    {
        this(input, Arrays.stream(arguments).map(TextArgument::argument).toArray());
    }

    /**
     * Retrieves the formatted String created using {@link MessageFormat#format(String, Object...)}.
     *
     * @return The formatted String.
     */
    public String getFormattedString()
    {
        return this.sb.toString();
    }

    /**
     * Retrieves the list of sections in the formatted String. The start and end index of each section refers to the
     * positions of characters in the formatted String; not the input String!
     *
     * @return The list of sections in the formatted String.
     */
    public List<MessageFormatSection> getSections()
    {
        return Collections.unmodifiableList(this.sections);
    }

    private static int getFieldId(AttributedCharacterIterator iter)
    {
        for (final var attr : iter.getAttributes().entrySet())
            if (attr.getKey() instanceof MessageFormat.Field)
                return (Integer) attr.getValue();
        return -1;
    }

    private void processMessageFormatIterator(AttributedCharacterIterator iter)
    {
        @Nullable Integer argumentIdx = null;

        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())
        {
            sb.append(c);
            final int currentArgumentIdx = getFieldId(iter);
            if (argumentIdx == null || argumentIdx != currentArgumentIdx)
            {
                sections.add(new MessageFormatSection(iter.getRunStart(), iter.getRunLimit(), currentArgumentIdx));
                argumentIdx = currentArgumentIdx;
            }
        }
    }

    /**
     * Represents a section in a fully-formatted {@link MessageFormat}.
     */
    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static final class MessageFormatSection
    {
        /**
         * The start of the section in the fully formatted result String.
         */
        private final int start;

        /**
         * The end of the section in the fully formatted result String.
         */
        private final int end;

        /**
         * The index of the argument this section was created from in the input arguments.
         * <p>
         * This is -1 for all sections that were not created from an input argument.
         */
        private final int argumentIdx;

        @VisibleForTesting MessageFormatSection(int start, int end, int argumentIdx)
        {
            if (end < start)
                throw new IllegalArgumentException("Range end " + end + " cannot be lower than range start: " + start);
            this.start = start;
            this.end = end;
            this.argumentIdx = argumentIdx;
        }
    }
}
