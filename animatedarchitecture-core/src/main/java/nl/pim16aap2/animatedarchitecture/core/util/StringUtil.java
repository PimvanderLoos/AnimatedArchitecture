package nl.pim16aap2.animatedarchitecture.core.util;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import lombok.CustomLog;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.IntStream;

/**
 * Utility class for strings.
 */
@CustomLog
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

    private StringUtil()
    {
    }

    /**
     * Converts an {@link ExecutorService} to a string.
     * <p>
     * This method is used for logging/debugging purposes.
     *
     * @param executor
     *     The {@link ExecutorService} to convert.
     * @return A string representation of the {@link ExecutorService}.
     */
    public static String toString(ExecutorService executor)
    {
        return String.format("""
                ExecutorService:
                - Executor Type: %s
                - Is shutdown:   %s
                - Is terminated: %s
                """,
            executor.getClass().getName(),
            executor.isShutdown(),
            executor.isTerminated()
        );
    }

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
     * Checks if a {@link CharSequence} has a trailing newline.
     *
     * @param charSequence
     *     The {@link CharSequence} to check.
     * @return True if the {@link CharSequence} has a trailing newline.
     */
    static boolean hasTrailingNewline(CharSequence charSequence)
    {
        return !charSequence.isEmpty() && charSequence.charAt(charSequence.length() - 1) == '\n';
    }

    /**
     * Removes all trailing newlines from a {@link StringBuilder}.
     *
     * @param sb
     *     The {@link StringBuilder} to remove the trailing newlines from.
     *     <p>
     *     This object is modified in place.
     *     <p>
     *     If the {@link StringBuilder} is empty or does not end with a newline, nothing is done.
     * @return The {@link StringBuilder} for chaining.
     */
    @Contract("_ -> param1")
    public static StringBuilder removeTrailingNewlines(StringBuilder sb)
    {
        while (hasTrailingNewline(sb))
            sb.deleteCharAt(sb.length() - 1);
        return sb;
    }

    /**
     * Removes all trailing newlines from a {@link String}.
     *
     * @param string
     *     The {@link String} to remove the trailing newlines from.
     *     <p>
     *     If the {@link String} is empty or does not end with a newline, the same object is returned.
     * @return The {@link String} without trailing newlines.
     */
    public static String removeTrailingNewlines(CharSequence string)
    {
        String ret = string.toString();
        while (hasTrailingNewline(ret))
            ret = ret.substring(0, ret.length() - 1);
        return ret;
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
     * Formats a collection of objects into a string.
     * <p>
     * The objects are formatted using the provided mapper function. The resulting strings are joined with a delimiter
     * and indented by the provided amount.
     * <p>
     * For example, given a collection of Strings ["A", "B", "C"], a mapper that returns the input string, and an indent
     * of 0, the resulting string would be:
     * <pre>{@code
     * - A
     * - B
     * - C
     * }</pre>
     * <p>
     * Note that there is no trailing delimiter or new line.
     *
     * @param collection
     *     The collection of objects to format.
     * @param mapper
     *     The function to map the objects to strings.
     * @param indent
     *     The amount of spaces to indent the lines. This is the number of spaces before each '-'.
     * @param printSize
     *     Whether to print the size of the collection in parentheses before the formatted strings.
     * @param <T>
     *     The type of the objects in the collection.
     * @return A formatted string.
     */
    public static <T> String formatCollection(
        Collection<T> collection,
        Function<T, String> mapper,
        int indent,
        boolean printSize)
    {
        final String delimiter = "\n" + " ".repeat(indent) + "- ";
        String prefix = "";
        if (printSize)
            prefix = collection.isEmpty() ? "" : "(" + collection.size() + ") ";
        return prefix + collection.stream().map(mapper).collect(stringCollector(delimiter));
    }

    /**
     * Formats a collection of objects into a string.
     * <p>
     * This method is a shortcut for {@link #formatCollection(Collection, Function, int, boolean)} with an indent of 0
     * and printSize enabled.
     *
     * @param collection
     *     The collection of objects to format.
     * @param mapper
     *     The function to map the objects to strings.
     * @param <T>
     *     The type of the objects in the collection.
     * @return A formatted string.
     */
    public static <T> String formatCollection(
        Collection<T> collection,
        Function<T, String> mapper,
        int indent)
    {
        final String delimiter = "\n" + " ".repeat(indent) + "- ";
        final String prefix = collection.isEmpty() ? "" : "(" + collection.size() + ")";
        return prefix + collection.stream().map(mapper).collect(stringCollector(delimiter));
    }

    /**
     * Formats a collection of objects into a string.
     * <p>
     * This method is a shortcut for {@link #formatCollection(Collection, Function, int, boolean)} with an indent of 0
     * and printSize enabled.
     *
     * @param collection
     *     The collection of objects to format.
     * @param mapper
     *     The function to map the objects to strings.
     * @param <T>
     *     The type of the objects in the collection.
     * @return A formatted string.
     */
    public static <T> String formatCollection(Collection<T> collection, Function<T, String> mapper)
    {
        return formatCollection(collection, mapper, 0);
    }

    /**
     * Formats a collection of objects into a string.
     * <p>
     * This method is a shortcut for {@link #formatCollection(Collection, Function, int, boolean)} with a mapper that
     * calls {@link Object#toString()} and printSize enabled.
     *
     * @param collection
     *     The collection of objects to format.
     * @return A formatted string.
     */
    public static String formatCollection(Collection<?> collection, int indent)
    {
        return formatCollection(collection, Object::toString, indent);
    }

    /**
     * Formats a collection of objects into a string.
     * <p>
     * This method is a shortcut for {@link #formatCollection(Collection, Function, int, boolean)} with an indent of 0
     * and the mapper that calls {@link Object#toString()} and printSize enabled.
     *
     * @param collection
     *     The collection of objects to format.
     * @return A formatted string.
     */
    public static String formatCollection(Collection<?> collection)
    {
        return formatCollection(collection, Object::toString, 0);
    }

    /**
     * Creates a collector that collects a stream of strings into a formatted string.
     * <p>
     * For example, given a delimiter of "\n  - " and an empty String of "{}", and a stream with values "1", "2", and
     * "3", the resulting string would be:
     * <pre>{@code
     * - 1
     * - 2
     * - 3
     * }</pre>
     * <p>
     * Given the same delimiter and empty string, but an empty stream, the resulting string would be:
     * <pre>{@code {}}</pre>
     * <p>
     * Note that there is no trailing delimiter or new line.
     * <p>
     * Example usage:
     * <pre>{@code
     * List<String> list = List.of("1", "2", "3");
     * String formatted = list.stream().collect(StringUtil.stringCollector("\n  - ", "{}"));
     * }</pre>
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

    /**
     * Creates a collector that collects a stream of strings into a formatted string.
     * <p>
     * This method is a shortcut for {@link #stringCollector(String, String)} with the empty string "{}".
     *
     * @param delimiter
     *     The delimiter to prepend to each string in the stream.
     * @return A collector that collects the stream into a formatted string.
     */
    public static Collector<String, ?, String> stringCollector(String delimiter)
    {
        return stringCollector(delimiter, "{}");
    }

    /**
     * Creates a collector that collects a stream of strings into a formatted string.
     * <p>
     * This method is a shortcut for {@link #stringCollector(String, String)} with the empty string "{}" and the
     * delimiter "\n- ".
     *
     * @return A collector that collects the stream into a formatted string.
     */
    public static Collector<String, ?, String> stringCollector()
    {
        return stringCollector("\n- ", "{}");
    }

    /**
     * Gets all indices of a given character in a string.
     * <p>
     * For example, given the string "Hello, World!" and the character 'o', this method would return the list [4, 8].
     *
     * @param string
     *     The string to search in.
     * @return A list of all indices of the given character in the string.
     */
    public static IntImmutableList getVariableIndices(String string, char variable)
    {
        return IntImmutableList.toList(IntStream.iterate(
            string.indexOf(variable),
            index -> index >= 0,
            index -> string.indexOf(variable, index + 1)));
    }
}
