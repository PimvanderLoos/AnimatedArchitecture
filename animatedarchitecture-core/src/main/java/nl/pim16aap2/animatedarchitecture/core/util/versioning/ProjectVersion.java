package nl.pim16aap2.animatedarchitecture.core.util.versioning;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Representation of a version of the project.
 * <p>
 * To obtain the version of the project that is currently running, use
 * {@link IAnimatedArchitecturePlatform#getProjectVersion()}.
 * <p>
 * This class can be used to compare versions. For example, to check if the current version is at least version 1.0:
 * <pre>{@code
 * ProjectVersion currentVersion = platform.getProjectVersion();
 * if (!currentVersion.isAtLeast("0.6-SNAPSHOT")) {
 *     // Do something
 *     return;
 * }}</pre>
 * <p>
 * For more information about the accepted version format, see {@link #parse(String)}.
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
     * Matches the insignificant zeroes of a version.
     * <p>
     * For example, "1.0.0" will match ".0.0", "1.0" will match ".0", and "1" and "1.0.1" will not match anything.
     * <p>
     * This pattern is used to remove the insignificant ".0" part from version Strings, leaving us only with the
     * important parts.
     */
    private static final Pattern INSIGNIFICANT_ZEROES_PATTERN = Pattern.compile("(\\.0+)*$");

    /**
     * The suffix of input snapshot versions.
     */
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * The version that is returned as error state. This is always equal to version "0-SNAPSHOT"; the lowest possible
     * version.
     */
    public static final ProjectVersion ERROR_VERSION = new ProjectVersion("0", true);

    /**
     * Creates a new ProjectVersion.
     *
     * @param version
     *     The (cleaned) version of the project.
     *     <p>
     *     If the actual version is "1.0-SNAPSHOT", the version is "1.0".
     * @param snapshot
     *     True if this is a snapshot version.
     * @throws IllegalStateException
     *     When the version pattern does not find exactly 1 match in the input String.
     * @throws IllegalStateException
     *     When the String that matches the pattern is not equal to the input String.
     */
    public ProjectVersion
    {
        verifyVersionPattern(version);
    }

    /**
     * Parses the ProjectVersion from the input String.
     * <p>
     * If the input String is invalid, {@link #ERROR_VERSION} is returned.
     *
     * @param input
     *     The input string. Must be of the form "version[.sub-version[.etc]][-SNAPSHOT]", where everything between
     *     brackets is optional.
     *     <p>
     *     For example, "1", "1.0", "1.0.1", "1-SNAPSHOT", "1.0-SNAPSHOT", "1.0.1-SNAPSHOT", etc.
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
     * Checks if the current version is newer than the other version.
     * <p>
     * If both the version and the snapshot are equal, the current version is not considered newer.
     * <p>
     * If only the versions are equal, the snapshot version is considered older than the non-snapshot version.
     *
     * @param other
     *     The other version to compare to.
     * @return True if the current version is newer than the other version.
     */
    public boolean isNewerThan(ProjectVersion other)
    {
        if (this.equals(other))
            return false;
        if (version.equals(other.version))
            return other.snapshot;
        return isNewer(other.version, this.version);
    }

    /**
     * Checks if the current version is newer than the other version.
     * <p>
     * If both the version and the snapshot are equal, the current version is not considered newer and the result will
     * be 'false'.
     * <p>
     * If only the versions are equal, the snapshot version is considered older than the non-snapshot version.
     *
     * @param other
     *     The String representation of the other version to compare to.
     *     <p>
     *     See {@link #parse(String)}.
     * @return True if the current version is newer than the other version.
     */
    public boolean isNewerThan(String other)
    {
        return isNewerThan(ProjectVersion.parse(other));
    }

    /**
     * Checks if the current version is greater than or equal to the other version.
     *
     * @param other
     *     The other version to compare to.
     * @return True if the current version is greater than or equal to the other version.
     */
    public boolean isAtLeast(ProjectVersion other)
    {
        if (this.equals(other))
            return true;
        return isNewerThan(other);
    }

    /**
     * Checks if the current version is greater than or equal to the other version.
     *
     * @param other
     *     The String representation of the other version to compare to.
     *     <p>
     *     See {@link #parse(String)}.
     * @return True if the current version is greater than or equal to the other version.
     */
    public boolean isAtLeast(String other)
    {
        return isAtLeast(ProjectVersion.parse(other));
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
     * Parses an integer from a String.
     * <p>
     * If the length of the input String is less than the section length, zeroes are appended to the input if allowed.
     *
     * @param input
     *     The input String to convert to an integer.
     * @param sectionLength
     *     The length of the section the input String was taken from. This is the number of characters between the dots
     *     of a version string.
     * @param append
     *     True to allow appending zeroes to the input String to pad its length to the section length.
     * @return The integer as parsed from the configured parameters.
     */
    @VisibleForTesting
    static int versionSectionToInt(String input, int sectionLength, boolean append)
    {
        try
        {
            final String base = append ? (input + "0".repeat(sectionLength - input.length())) : input;
            return Integer.parseInt(base);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to parse integer from input '%s' with section length %d", input, sectionLength);
            return 0;
        }
    }

    /**
     * Trims insignificant zeroes from a version String.
     * <p>
     * This method removes all 'insignificant' zeroes from a version String. This means that all zeroes that are not at
     * the start of the String are removed.
     * <p>
     * For example, "1.0.0" will be trimmed to "1", but "1.0.1" and "0.1" will be returned as-is.
     *
     * @param input
     *     The input String to trim.
     * @return The trimmed version String.
     */
    @VisibleForTesting
    static String trimInsignificantZeroes(String input)
    {
        return INSIGNIFICANT_ZEROES_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Checks if one version is greater than another version.
     * <p>
     * All version Strings are expected to be of the format "version[.sub-version[.etc]]", with everything between
     * brackets being optional. Note that this method assumes that the input Strings may not contain a snapshot suffix.
     * <p>
     * This method splits a version on the dots and compares each group of characters against each other.
     *
     * @param base
     *     The base version to test again.
     * @param test
     *     The test version to compare to the base version.
     * @return True if the test version is newer than the base version.
     */
    @VisibleForTesting
    static boolean isNewer(String base, String test)
    {
        final String[] baseParts = trimInsignificantZeroes(base).split("\\.");
        final String[] testParts = trimInsignificantZeroes(test).split("\\.");

        final int partsLen = Math.min(baseParts.length, testParts.length);
        for (int idx = 0; idx < partsLen; ++idx)
        {
            final String basePart = baseParts[idx];
            final String testPart = testParts[idx];
            final int sectionLength = Math.max(basePart.length(), testPart.length());

            // Allow appending zeroes when the other version part has a leading zero.
            // For example, when comparing the second section of "0.1" and "0.01", we actually need to compare 10 to 1,
            // respectively, not 1 to 1 as you would get with a regular integer cast.
            // And for the second section of "0.9" and "0.11", we need to compare "9" to "11" (not 90 to 11).
            final int baseVer = versionSectionToInt(basePart, sectionLength, testPart.startsWith("0"));
            final int testVer = versionSectionToInt(testPart, sectionLength, basePart.startsWith("0"));
            if (baseVer != testVer)
                return testVer > baseVer;
        }
        return testParts.length > baseParts.length;
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
