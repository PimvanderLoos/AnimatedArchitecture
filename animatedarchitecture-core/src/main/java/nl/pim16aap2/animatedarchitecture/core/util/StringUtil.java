package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

/**
 * Utility class for strings.
 */
@Flogger
@UtilityClass
public final class StringUtil
{
    /**
     * Characters to use in (secure) random strings.
     */
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Used to generate simple random strings.
     */
    private static final Random RANDOM = new Random();
    /**
     * A valid structure name.
     * <p>
     * All letters "a-zA-Z" are allowed as well as "-" and '_'. Numbers are allowed as well, but only if there are
     * non-number characters in the name as well. For example, '0_MyDoor-0' is allowed, but '0' is not.
     */
    private static final Pattern VALID_STRUCTURE_NAME = Pattern.compile("^\\w*[a-zA-Z_-]+\\w*$");


    /**
     * Generate an (insecure) random alphanumeric string of a given length.
     *
     * @param length
     *     Length of the resulting string
     * @return An insecure random alphanumeric string.
     */
    public static String randomString(int length)
    {
        final StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    /**
     * Removes the trailing new line from a {@link StringBuilder}.
     *
     * @param sb
     *     The {@link StringBuilder} to remove the trailing new line from.
     *     <p>
     *     This object is modified in place.
     *     <p>
     *     If the {@link StringBuilder} is empty or does not end with a new line, nothing is done.
     * @return The {@link StringBuilder} for chaining.
     */
    @Contract("_ -> param1")
    public static StringBuilder removeTrailingNewLine(StringBuilder sb)
    {
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n')
            sb.deleteCharAt(sb.length() - 1);
        return sb;
    }

    /**
     * Capitalizes the first letter. The rest of the String is left intact.
     *
     * @param string
     *     The string for which to capitalize the first letter.
     * @return The same string that it received as input, but with a capizalized first letter.
     */
    public static String capitalizeFirstLetter(String string)
    {
        return string.isEmpty() ? string : string.substring(0, 1).toUpperCase(Locale.ENGLISH) + string.substring(1);
    }

    /**
     * Obtains the numbers of question marks in a String.
     *
     * @param statement
     *     The String.
     * @return The number of question marks in the String.
     */
    public static int countPatternOccurrences(Pattern pattern, String statement)
    {
        int found = 0;
        final Matcher matcher = pattern.matcher(statement);
        while (matcher.find())
            ++found;
        return found;
    }

    /**
     * Check if a given string is a valid structure name. The following input is not allowed:
     *
     * <ul>
     *     <li>Numerical names (numerical values are reserved for UIDs).</li>
     *     <li>Empty input.</li>
     *     <li>Input containing spaces.</li>
     * </ul>
     *
     * @param name
     *     The name to test for validity,
     * @return True if the name is allowed.
     */
    public static boolean isValidStructureName(@Nullable String name)
    {
        if (name == null || name.isBlank())
            return false;

        return VALID_STRUCTURE_NAME.matcher(name).matches();
    }

    /**
     * Creates a collector that collects a stream of strings into a formatted string.
     * <p>
     * For example, given a delimiter of "\n  - " and an empty String of "[]", and a stream "A" with values "1", "2",
     * and "3", the resulting string would be:
     * <pre>{@code
     * - 1
     * - 2
     * - 3
     * }</pre>
     * <p>
     * Given the same delimiter and empty string, but an empty stream, the resulting string would be:
     * <pre>{@code []}</pre>
     * <p>
     * Note that there is no trailing delimiter or new line.
     *
     * @param delimiter
     *     The delimiter to prepend to each string in the stream.
     * @param emptyString
     *     The string representation when the stream is empty.
     * @return A collector that collects the stream into a formatted string.
     */
    public static Collector<String, ?, String> stringCollector(String delimiter, String emptyString)
    {
        return Collector.of(
            StringBuilder::new,
            (sb, str) -> sb.append(delimiter).append(str),
            (sb1, sb2) ->
            {
                if (!sb1.isEmpty() && !sb2.isEmpty())
                    sb1.append(delimiter);
                return sb1.append(sb2);
            },
            sb -> sb.isEmpty() ? emptyString : sb.toString()
        );
    }
}
