package nl.pim16aap2.animatedarchitecture.core.extensions;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains information about a structure type.
 * <p>
 * These data are extracted from the manifest of the jar file that contains the structure type and can be used to
 * instantiate the structure type.
 */
@Flogger
@ToString
final class StructureTypeInfo implements IKeyed
{
    /**
     * Matches the name of a dependency in the format "{@code string:string(int;int)}".
     * <p>
     * The name is expected to be a sequence of lower case characters followed by the version range in parentheses at
     * the end of the string.
     */
    private static final Pattern NAME_MATCH = getNameMatchPattern();

    /**
     * Matches the version range of a dependency in the format "{@code string(int;int)}".
     * <p>
     * The version range is expected to be 2 integers separated by a semicolon and enclosed in parentheses without any
     * characters in the String after the closing parenthesis.
     */
    private static final Pattern VERSION_MATCH = Pattern.compile("(?<=\\()[0-9]+;[0-9]+(?=\\)$)");

    /**
     * The key of the structure type in its namespace.
     */
    @Getter
    private final NamespacedKey namespacedKey;

    /**
     * The version of the structure type.
     */
    @Getter
    private final int version;

    /**
     * The path to the main class of the structure type.
     * <p>
     * E.g. {@code com.example.Main}.
     */
    @Getter
    private final String mainClass;

    /**
     * The path to the jar file being described by this {@link StructureTypeInfo}.
     */
    @Getter
    private final Path jarFile;

    /**
     * A String that contains the supported API version(s) of the structure type.
     */
    @Getter
    private final String supportedApiVersions;

    /**
     * The dependencies of the structure type.
     * <p>
     * Each dependency is defined by a name, a minimum version, and a maximum version. The version of a structure type
     * must be between the minimum and maximum version of the dependency for the dependency to be satisfied.
     */
    @Getter
    private final List<Dependency> dependencies;

    /**
     * Creates a new instance of {@link StructureTypeInfo}.
     *
     * @param namespacedKey
     *     The namespaced key of the structure type.
     * @param version
     *     The version of the structure type.
     * @param mainClass
     *     The main class of the structure type.
     * @param jarFile
     *     The jar file that contains the structure type.
     * @param dependencies
     *     The dependencies of the structure type. This is a string that contains a space-separated list of dependencies
     *     in the format "{@code <typeName>(minVersion;maxVersion)}". For example: "{@code portcullis(1;5) door(1;1)}".
     *     <p>
     *     Both the minimum and the maximum versions of the dependency are inclusive.
     */
    public StructureTypeInfo(
        NamespacedKey namespacedKey,
        int version,
        String mainClass,
        Path jarFile,
        String supportedApiVersions,
        @Nullable String dependencies)
    {
        this.namespacedKey = namespacedKey;

        this.version = version;
        this.mainClass = mainClass;
        this.jarFile = jarFile;
        this.supportedApiVersions = supportedApiVersions;
        this.dependencies = parseDependencies(dependencies, namespacedKey.getFullKey());
    }

    /**
     * Verifies that the loaded structure type is the same as the one described by this {@link StructureTypeInfo}.
     *
     * @param structureType
     *     The loaded structure type.
     * @throws IllegalArgumentException
     *     If the loaded structure type does not match the structure type described by this {@link StructureTypeInfo}.
     */
    public void verifyLoadedType(StructureType structureType)
    {
        if (!structureType.getNamespacedKey().equals(namespacedKey))
            throw new IllegalArgumentException(
                String.format(
                    "Key mismatch for loaded structure type! Expected '%s', got '%s'.",
                    getFullKey(),
                    structureType.getFullKey()
                )
            );

        if (structureType.getVersion() != version)
            throw new IllegalArgumentException(
                String.format(
                    "Version mismatch for structure type '%s'! Expected %d, got %d",
                    getFullKey(),
                    getVersion(),
                    structureType.getVersion()
                )
            );
    }

    /**
     * Parses a String containing dependencies into a list of {@link Dependency} objects.
     * <p>
     * The dependencies are expected to be a space-separated list of dependencies in the format
     * "{@code <typeName>(minVersion;maxVersion)}". For example: "{@code portcullis(1;5) door(1;1)}".
     *
     * @param dependencies
     *     The String
     * @param fullKey
     *     The full key of the structure type. This is used for logging purposes.
     *     <p>
     *     This is the result of calling {@link NamespacedKey#getFullKey()} on the {@link #namespacedKey}.
     * @return The list of dependencies parsed from the provided string.
     */
    @VisibleForTesting
    static List<Dependency> parseDependencies(@Nullable String dependencies, String fullKey)
    {
        if (dependencies == null || dependencies.isEmpty() || "null".equals(dependencies))
            return Collections.emptyList();

        final String[] split = dependencies.toLowerCase(Locale.ENGLISH).split(" ");
        final List<Dependency> ret = new ArrayList<>(split.length);

        for (int idx = 0; idx < split.length; ++idx)
        {
            final String depStr = split[idx];

            try
            {
                ret.add(idx, parseDependency(depStr));
            }
            catch (Exception exception)
            {
                throw new IllegalArgumentException(
                    "Failed to parse dependency '" + depStr +
                        "' at index " + idx +
                        " from dependency string '" + dependencies +
                        "' for structure type '" + fullKey + "'.",
                    exception
                );
            }
        }
        return Collections.unmodifiableList(ret);
    }

    @VisibleForTesting
    static Dependency parseDependency(@Nullable String fullKey)
    {
        if (fullKey == null || fullKey.isBlank())
            throw new IllegalArgumentException("Dependency must not be null or blank.");

        // Get all the alphanumeric characters at the start of the string.
        final Matcher nameMatcher = NAME_MATCH.matcher(fullKey);
        if (!nameMatcher.find())
            throw new IllegalArgumentException("Failed to find the dependency name in: '" + fullKey + "'");

        final String dependencyName = nameMatcher.group();
        final var dependencyKey = NamespacedKey.of(dependencyName);

        final Matcher versionMatcher = VERSION_MATCH.matcher(fullKey);
        if (!versionMatcher.find())
            throw new IllegalArgumentException("Failed to find the version in: '" + fullKey + "'");

        final String[] versionSplit = versionMatcher.group().split(";");
        if (versionSplit.length != 2)
            throw new IllegalArgumentException("Failed to split the version in: '" + fullKey + "'");

        final OptionalInt minVersionOpt = MathUtil.parseInt(versionSplit[0]);
        if (minVersionOpt.isEmpty())
            throw new IllegalArgumentException("Failed to parse min version from '" + versionSplit[0] + "'");

        final OptionalInt maxVersionOpt = MathUtil.parseInt(versionSplit[1]);
        if (maxVersionOpt.isEmpty())
            throw new IllegalArgumentException("Failed to parse max version from '" + versionSplit[1] + "'");

        return new Dependency(dependencyKey, minVersionOpt.getAsInt(), maxVersionOpt.getAsInt());
    }

    /**
     * Matches the name of a structure type in the format "{@code namespace:name(int;int)}".
     * <p>
     * Both the namespace and the name are expected to adhere to the pattern defined by {@link NamespacedKey#PATTERN}.
     *
     * @return The compiled pattern.
     */
    private static Pattern getNameMatchPattern()
    {
        final String base = NamespacedKey.PATTERN.pattern().replace("^", "").replace("$", "");
        return Pattern.compile("^" + base + ":" + base + "(?=\\([0-9]+;[0-9]+\\)$)");
    }

    /**
     * Represents a dependency of a structure type.
     * <p>
     * A dependency is defined by a name, a minimum version, and a maximum version. The version of a structure type must
     * be between the minimum and maximum version of the dependency for the dependency to be satisfied.
     *
     * @param namespacedKey
     *     The key of the dependency. This is the namespaced key of the structure type that this dependency refers to.
     * @param minVersion
     *     The minimum version of the dependency. Inclusive.
     *     <p>
     *     Must be smaller than {@code maxVersion}.
     * @param maxVersion
     *     The maximum version of the dependency. Inclusive.
     *     <p>
     *     Must be larger than {@code minVersion}.
     */
    record Dependency(NamespacedKey namespacedKey, int minVersion, int maxVersion) implements IKeyed
    {
        // Ensure that the minVersion is smaller than the maxVersion.
        Dependency
        {
            if (minVersion > maxVersion)
                throw new IllegalArgumentException("minVersion must be smaller than maxVersion.");
        }

        /**
         * Creates a new instance of {@link Dependency}.
         *
         * @param key
         *     The key of the dependency. This will be parsed into a {@link NamespacedKey}. See
         *     {@link NamespacedKey#of(String)}. The key is expected to be in the format "{@code namespace:name}".
         * @param minVersion
         *     The minimum version of the dependency. Inclusive.
         *     <p>
         *     Must be smaller than {@code maxVersion}.
         * @param maxVersion
         *     The maximum version of the dependency. Inclusive.
         *     <p>
         *     Must be larger than {@code minVersion}.
         */
        Dependency(String key, int minVersion, int maxVersion)
        {
            this(NamespacedKey.of(key), minVersion, maxVersion);
        }

        @Override
        public String toString()
        {
            return namespacedKey.getFullKey() + "(" + minVersion + ";" + maxVersion + ")";
        }

        /**
         * Checks if a {@link StructureTypeInfo} satisfies the requirements of this dependency (i.e. its version is
         * between the min/max version of this dependency).
         *
         * @param structureTypeInfo
         *     The structure type info to compare against.
         * @return True if this dependency is satisfied by the provided {@link StructureTypeInfo}.
         */
        boolean satisfiedBy(StructureTypeInfo structureTypeInfo)
        {
            return structureTypeInfo.getNamespacedKey().equals(namespacedKey) &&
                MathUtil.between(structureTypeInfo.getVersion(), minVersion, maxVersion);
        }

        @Override
        public NamespacedKey getNamespacedKey()
        {
            return namespacedKey;
        }
    }
}
