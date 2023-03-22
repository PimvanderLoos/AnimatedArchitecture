package nl.pim16aap2.animatedarchitecture.core.util.versioning;

import lombok.extern.flogger.Flogger;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Representation of the project version that is currently running.
 *
 * @param version
 *     The (cleaned) version of the project.
 *     <p>
 *     If the actual version is "1.0-SNAPSHOT", the version is "1.0".
 * @param snapshot
 *     True if this is a snapshot version.
 */
@Flogger
public record ProjectVersion(String version, boolean snapshot)
{
    /**
     * Only digits and dots are allowed.
     * <p>
     * Dots are only allowed between digits (so no leading and trailing dots).
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)*$");

    /**
     * The suffix of input snapshot versions.
     */
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * The version that is returned as error state.
     */
    public static final ProjectVersion ERROR_VERSION = new ProjectVersion("0", true);

    public ProjectVersion
    {
        verifyVersionPattern(version);
    }

    /**
     * Parses the ProjectVersion from the input String.
     *
     * @param input
     *     The input string. Must be of the form "version[.subversion[.etc]][-SNAPSHOT]", where everything between
     *     brackets is optional.
     * @return The parsed project version.
     */
    public static ProjectVersion parse(String input)
    {
        try
        {
            final boolean snapshot = input.endsWith(SNAPSHOT_SUFFIX);
            final String cleaned = input.replace(SNAPSHOT_SUFFIX, "");
            return new ProjectVersion(cleaned, snapshot);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to parse project version from input: '%s'", input);
        }
        return ERROR_VERSION;
    }

    /**
     * Ensures that an input string fits {@link #VERSION_PATTERN}.
     *
     * @param input
     *     The input string to process.
     * @throws IllegalStateException
     *     When the version pattern does not find exactly 1 match in the input String.
     * @throws IllegalStateException
     *     When the String that matches the pattern is not equal to the input String.
     */
    private static void verifyVersionPattern(String input)
    {
        final List<MatchResult> matches = VERSION_PATTERN.matcher(input).results().toList();
        if (matches.size() != 1)
            throw new IllegalArgumentException("Found " + matches.size() + " matches for input '" + input + "'!");

        final String match = matches.get(0).group();
        if (!input.equals(match))
            throw new IllegalArgumentException(
                "Input '" + input + "' does not fit requirements! Found match: '" + match + "'!");
    }

    /**
     * Formats this project version as the original version String.
     *
     * @return The representation of this object represented as a version String.
     */
    public String toVersionString()
    {
        return this.version + (this.snapshot ? SNAPSHOT_SUFFIX : "");
    }
}
