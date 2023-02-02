package nl.pim16aap2.bigdoors.extensions;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Flogger
@ToString//
final class StructureTypeInfo
{
    @ToString.Exclude
    private static final Pattern NAME_MATCH = Pattern.compile("^[a-zA-Z]*");
    @ToString.Exclude
    private static final Pattern MIN_VERSION_MATCH = Pattern.compile("[0-9]*;");
    @ToString.Exclude
    private static final Pattern MAX_VERSION_MATCH = Pattern.compile(";[0-9]*");

    @Getter
    private final String typeName;
    @Getter
    private final int version;
    @Getter
    private final String mainClass;
    @Getter
    private final Path jarFile;
    @Getter
    private final List<Dependency> dependencies;

    public StructureTypeInfo(
        String typeName, int version, String mainClass, Path jarFile, @Nullable String dependencies)
    {
        this.typeName = typeName.toLowerCase(Locale.ENGLISH);
        this.version = version;
        this.mainClass = mainClass;
        this.jarFile = jarFile;
        this.dependencies = Collections.unmodifiableList(parseDependencies(dependencies));
    }

    private List<Dependency> parseDependencies(@Nullable String dependencies)
    {
        if (dependencies == null || dependencies.isEmpty())
            return Collections.emptyList();

        final String[] split = dependencies.split(" ");
        final List<Dependency> ret = new ArrayList<>(split.length);

        for (int idx = 0; idx < split.length; ++idx)
        {
            final int arrPos = idx;
            parseDependency(split[idx]).ifPresentOrElse(
                dep -> ret.add(arrPos, dep),
                () -> log.atSevere().log("Failed to parse dependency '%s' for type: %s", split[arrPos], typeName));
        }
        return ret;
    }

    private Optional<Dependency> parseDependency(String dependency)
    {
        final Matcher nameMatcher = NAME_MATCH.matcher(dependency);
        if (!nameMatcher.find())
        {
            log.atFine().log("Failed to find the dependency name in: %s", dependency);
            return Optional.empty();
        }
        final String dependencyName = nameMatcher.group();

        final Matcher minVersionMatcher = MIN_VERSION_MATCH.matcher(dependency);
        if (!minVersionMatcher.find())
        {
            log.atFine().log("Failed to find the min version in: %s", dependency);
            return Optional.empty();
        }
        String minVersionStr = minVersionMatcher.group();
        minVersionStr = minVersionStr.substring(0, minVersionStr.length() - 1);
        final OptionalInt minVersionOpt = Util.parseInt(minVersionStr);
        if (minVersionOpt.isEmpty())
        {
            log.atFine().log("Failed to parse min version from: %s", minVersionStr);
            return Optional.empty();
        }

        final Matcher maxVersionMatcher = MAX_VERSION_MATCH.matcher(dependency);
        if (!maxVersionMatcher.find())
        {
            log.atFine().log("Failed to find the max version in: %s", dependency);
            return Optional.empty();
        }

        String maxVersionStr = maxVersionMatcher.group();
        maxVersionStr = maxVersionStr.substring(1);
        final OptionalInt maxVersionOpt = Util.parseInt(maxVersionStr);
        if (maxVersionOpt.isEmpty())
        {
            log.atFine().log("Failed to parse max version from: %s", maxVersionStr);
            return Optional.empty();
        }

        return Optional.of(new Dependency(dependencyName, minVersionOpt.getAsInt(), maxVersionOpt.getAsInt()));
    }

    public record Dependency(String dependencyName, int minVersion, int maxVersion)
    {
        @Override
        public String toString()
        {
            return dependencyName + "(" + minVersion + "," + maxVersion + ")";
        }

        /**
         * Checks if a {@link StructureTypeInfo} satisfies the requirements of this dependency (i.e. its version is
         * between the min/max version of this dependency).
         *
         * @param structureTypeInfo
         *     The structure type info to compare against.
         * @return True if this dependency is satisfied by the provided {@link StructureTypeInfo}.
         */
        public boolean satisfiedBy(StructureTypeInfo structureTypeInfo)
        {
            return structureTypeInfo.getTypeName().equals(dependencyName) &&
                Util.between(structureTypeInfo.getVersion(), minVersion, maxVersion);
        }
    }
}
