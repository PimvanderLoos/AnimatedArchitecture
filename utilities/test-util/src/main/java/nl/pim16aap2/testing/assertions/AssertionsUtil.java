package nl.pim16aap2.testing.assertions;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.mockito.ArgumentMatcher;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A utility class for general assertions.
 */
public final class AssertionsUtil
{
    private AssertionsUtil()
    {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    static String throwableToString(Throwable throwable)
    {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    /**
     * Asserts that two strings are equal using a specific {@link StringMatchType}.
     *
     * @param expected
     *     The expected string.
     * @param actual
     *     The actual string.
     * @param matchType
     *     The type of match to perform.
     */
    public static void assertStringEquals(String expected, String actual, StringMatchType matchType)
    {
        matchType.assertMatches(expected, actual);
    }

    /**
     * The different ways to match a string against another string.
     */
    public enum StringMatchType
    {
        /**
         * The strings must be exactly the same.
         */
        EXACT
            {
                @Override
                boolean matches(@Nullable String base, @Nullable String argument)
                {
                    if (base == null || argument == null)
                        return base == null && argument == null;
                    return base.equals(argument);
                }

                @Override
                void assertMatches(@Nullable String expected, @Nullable String actual)
                {
                    if (!matches(actual, expected))
                        fail(String.format("""
                                Expected String did not match actual String:
                                Expected string: "%s"
                                Actual string:   "%s"
                                """,
                            expected,
                            actual)
                        );
                }
            },

        /**
         * The base string must contain the argument string.
         */
        CONTAINS
            {
                @Override
                boolean matches(@Nullable String base, @Nullable String argument)
                {
                    if (base == null || argument == null)
                        return base == null && argument == null;
                    return base.contains(argument);
                }

                @Override
                void assertMatches(@Nullable String expected, @Nullable String actual)
                {
                    if (!matches(actual, expected))
                        fail(String.format("""
                                Expected String did not match actual String:
                                Expected string to contain: "%s"
                                Actual string:               "%s"
                                """,
                            expected,
                            actual)
                        );
                }
            },

        /**
         * The base string must start with the argument string.
         */
        STARTS_WITH
            {
                @Override
                boolean matches(@Nullable String base, @Nullable String argument)
                {
                    if (base == null || argument == null)
                        return base == null && argument == null;
                    return base.startsWith(argument);
                }

                @Override
                void assertMatches(@Nullable String expected, @Nullable String actual)
                {
                    if (!matches(actual, expected))
                        fail(String.format("""
                                Expected String did not match actual String:
                                Expected string to start with: "%s"
                                Actual string:                 "%s"
                                """,
                            expected,
                            actual)
                        );
                }
            },

        /**
         * The base string must end with the argument string.
         */
        ENDS_WITH
            {
                @Override
                boolean matches(@Nullable String base, @Nullable String argument)
                {
                    if (base == null || argument == null)
                        return base == null && argument == null;
                    return base.endsWith(argument);
                }

                @Override
                void assertMatches(@Nullable String expected, @Nullable String actual)
                {
                    if (!matches(actual, expected))
                        fail(String.format("""
                                Expected String did not match actual String:
                                Expected string to end with: "%s"
                                Actual string:               "%s"
                                """,
                            expected,
                            actual)
                        );
                }
            };

        /**
         * Checks if the base string matches the argument string.
         *
         * @param base
         *     The base string.
         * @param argument
         *     The argument string.
         * @return Whether the base string matches the argument string.
         */
        abstract boolean matches(@Nullable String base, @Nullable String argument);

        /**
         * Asserts that the expected string matches the actual string.
         *
         * @param expected
         *     The expected string.
         * @param actual
         *     The actual string.
         */
        abstract void assertMatches(@Nullable String expected, @Nullable String actual);
    }

    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static final class StringArgumentMatcher implements ArgumentMatcher<String>
    {
        private final String base;
        private final StringMatchType matchType;

        @Override
        public boolean matches(String argument)
        {
            return matchType.matches(base, argument);
        }
    }
}
