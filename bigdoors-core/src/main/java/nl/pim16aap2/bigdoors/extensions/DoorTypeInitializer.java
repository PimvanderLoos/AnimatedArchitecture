package nl.pim16aap2.bigdoors.extensions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an initializer for a group of {@link DoorType}s.
 * <p>
 * This class will ensure that the dependencies between {@link DoorType}s are met and then load their jars into the
 * desired {@link DoorTypeClassLoader}.,
 *
 * @author Pim
 */
@Flogger //
final class DoorTypeInitializer
{
    /**
     * Maps all {@link DoorType}s that are going to be registered in this run.
     * <p>
     * The map contains the {@link DoorType#getSimpleName()} of all the {@link DoorType}s that will be registered as
     * well as a pair of the {@link TypeInfo} of the {@link DoorType} and its weight.
     * <p>
     * The weight here can be null if it hasn't been computed yet. Once computed, lower values must be instantiated
     * before higher values. So if a type "BigDoor" doesn't depend on anything, it will have a weight of 0. If a type
     * "RevolvingDoor" depends on "BigDoor", it will have weight > 0.
     */
    private final Map<String, TypeInfoAndWeight> registrationQueue = new HashMap<>()
    {
        @Override
        public @Nullable TypeInfoAndWeight put(String key, TypeInfoAndWeight value)
        {
            return super.put(key.toLowerCase(Locale.ENGLISH), value);
        }
    };

    /**
     * Gets the list of all {@link TypeInfo}s sorted by their dependency weights. This means that {@link TypeInfo}s at
     * the lower indices need to be loaded before those at the higher indices to satisfy all dependencies.
     * <p>
     * {@link TypeInfo}s with unmet dependencies will not appear in this list.
     *
     * @return The sorted list of all {@link TypeInfo}s sorted by their dependency weights.
     */
    @Getter
    private final List<TypeInfo> sorted;

    private final DoorTypeClassLoader doorTypeClassLoader;
    private final DoorTypeManager doorTypeManager;

    /**
     * Instantiates this {@link DoorTypeInitializer}. It will attempt to assign dependency weights to all entries
     * (stored in {@link #registrationQueue}) and then sort those entries by their weight, starting at the lowest
     * (stored in {@link #sorted}).
     *
     * @param typeInfoList
     *     The list of {@link TypeInfo}s that should be loaded.
     */
    DoorTypeInitializer(List<TypeInfo> typeInfoList, DoorTypeClassLoader doorTypeClassLoader,
                        DoorTypeManager doorTypeManager)
    {
        this.doorTypeClassLoader = doorTypeClassLoader;
        this.doorTypeManager = doorTypeManager;

        typeInfoList.forEach(info -> registrationQueue.put(info.getTypeName(), new TypeInfoAndWeight(info, null)));
        registrationQueue.forEach(
            (name, pair) ->
            {
                final LoadResult loadResult = processDependencies(pair.typeInfo);
                if (loadResult.loadResultType != LoadResultType.DEPENDENCIES_AVAILABLE &&
                    (!loadResult.message.isEmpty()))
                    log.at(Level.WARNING).log("%s", loadResult.message);
            });

        sorted = getSortedDoorTypeInfo();
        log.at(Level.FINER).log("List of sorted dependencies:\n%s",
                                new SortedDependenciesPrinter().sortedDependenciesToString());
    }

    /**
     * Constructs {@link #sorted} from {@link #registrationQueue}. The sorted list contains all {@link TypeInfo} with a
     * valid dependency weight, and it is sorted by this weight value. Lower weights are assigned lower indices.
     *
     * @return The sorted list of {@link TypeInfo}s with valid weights.
     */
    private List<TypeInfo> getSortedDoorTypeInfo()
    {
        final List<TypeInfo> newSorted = new ArrayList<>(registrationQueue.size());
        registrationQueue.forEach(
            (name, pair) ->
            {
                if (pair.weight != null && pair.weight >= 0)
                {
                    pair.typeInfo.setWeight(pair.weight);
                    newSorted.add(pair.typeInfo);
                }
            });
        newSorted.sort(Comparator.comparing(TypeInfo::getWeight));
        return newSorted;
    }

    /**
     * Simple class used to print the {@link #sorted} dependencies.
     * <p>
     * It's wrapped in this class to allow for delayed logging of the sorted dependencies (the delayed logging used the
     * toString method).
     */
    private class SortedDependenciesPrinter
    {
        /**
         * Formats the {@link #sorted} list into a pretty string.
         *
         * @return The formatted String representing {@link #sorted}.
         */
        public String sortedDependenciesToString()
        {
            final StringBuilder sb = new StringBuilder();
            for (int idx = 0; idx < sorted.size(); ++idx)
            {
                final TypeInfo info = sorted.get(idx);
                final StringBuilder depSB = new StringBuilder();
                info.getDependencies().forEach(dependencyOpt -> dependencyOpt
                    .ifPresent(dependency -> depSB.append(dependency.dependencyName).append(' ')));

                sb.append(String.format("(%-2d) Weight: %-2d type: %-15s dependencies: %s",
                                        idx, info.weight, info.getTypeName(), depSB)).append('\n');
            }
            return sb.toString();
        }

        @Override
        public String toString()
        {
            return sortedDependenciesToString();
        }
    }

    /**
     * Attempts to load a jar.
     *
     * @param file
     *     The jar file.
     * @return True if the jar loaded successfully.
     */
    private boolean loadJar(Path file)
    {
        try
        {
            doorTypeClassLoader.addURL(file.toUri().toURL());
        }
        catch (Exception e)
        {
            log.at(Level.FINE).withCause(e).log();
            return false;
        }
        return true;
    }

    /**
     * Attempts to load a {@link TypeInfo} from its {@link TypeInfo#getMainClass()}.
     *
     * @param typeInfo
     *     The {@link TypeInfo} to load.
     * @return The {@link DoorType} that resulted from loading the {@link TypeInfo}, if possible.
     */
    private Optional<DoorType> loadDoorType(TypeInfo typeInfo)
    {
        log.at(Level.FINE).log("Trying to load type: %s", typeInfo.getTypeName());

        if (!loadJar(typeInfo.jarFile))
        {
            log.at(Level.WARNING)
               .log("Failed to load file: '%s'! This type ('%s') will not be loaded! See the log for more details.",
                    typeInfo.getJarFile(), typeInfo.getTypeName());
            return Optional.empty();
        }

        final DoorType doorType;
        try
        {
            final Class<?> typeClass = doorTypeClassLoader.loadClass(typeInfo.mainClass);
            final Method getter = typeClass.getDeclaredMethod("get");
            doorType = (DoorType) getter.invoke(null);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to load extension: %s", typeInfo.getTypeName());
            return Optional.empty();
        }

        log.at(Level.FINE).log("Loaded BigDoors extension: %s", Util.capitalizeFirstLetter(doorType.getSimpleName()));
        return Optional.of(doorType);
    }

    /**
     * Attempts to load all {@link DoorType} from {@link #sorted}.
     *
     * @return The {@link DoorType}s that resulted from loading the {@link TypeInfo}s.
     */
    public List<DoorType> loadDoorTypes()
    {
        final List<DoorType> ret = new ArrayList<>(getSorted().size());
        getSorted().forEach(doorInfo -> loadDoorType(doorInfo).ifPresent(ret::add));
        return ret;
    }

    /**
     * Processes the dependencies of a {@link TypeInfo}. It will recursively check all entries in {@link
     * #registrationQueue} if needed.
     * <p>
     * Once checked, it will assign the {@link TypeInfo} it checked with a dependency weight, where a higher weight
     * means it must be loaded after any other {@link TypeInfo}s with a lower weight.
     *
     * @param doorTypeInfo
     *     The {@link TypeInfo} whose dependencies to check.
     * @return The {@link LoadResult} of the current {@link DoorType}.
     */
    private LoadResult processDependencies(TypeInfo doorTypeInfo)
    {
        final String currentName = doorTypeInfo.getTypeName();
        final @Nullable TypeInfoAndWeight currentStatus = registrationQueue.get(doorTypeInfo.typeName);
        if (currentStatus == null)
            return new LoadResult(LoadResultType.INVALID_DOOR_TYPE,
                                  "Type " + doorTypeInfo.getTypeName() + " was not mapped!");

        // If the weight of the current entry is not null, then it has already been calculated.
        if (currentStatus.weight != null)
            return currentStatus.weight == -1 ?
                   new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "") :
                   new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE, "");
        int newWeight = 0;

        for (final Optional<Dependency> dependencyOpt : doorTypeInfo.getDependencies())
        {
            if (dependencyOpt.isEmpty())
            {
                registrationQueue.replace(currentName, new TypeInfoAndWeight(doorTypeInfo, -1));
                return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                      currentName + ": Failed to find dependency!");
            }
            final Dependency dependency = dependencyOpt.get();
            final String dependencyName = dependency.dependencyName();

            // If the dependency has already been registered, it has already been loaded, obviously.
            if (doorTypeManager.getDoorType(dependencyName).isPresent())
            {
                registrationQueue.replace(currentName, new TypeInfoAndWeight(doorTypeInfo, 0));
                return new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "");
            }

            // If it hasn't been registered yet, check if it exists in the registerQueue.
            // If the dependency will be installed in the future, then that's fine.
            if (!registrationQueue.containsKey(dependencyName))
            {
                registrationQueue.replace(currentName, new TypeInfoAndWeight(doorTypeInfo, -1));
                return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                      "Type \"" + currentName + "\" depends on type: \"" +
                                          dependencyName + "\" which isn't installed!");
            }

            // Before we just assume any old TypeInfo we find that will be registered in the future, we'll
            // have to make sure that that dependency's dependencies are also met.
            final TypeInfoAndWeight queuedDoorTypeInfo = registrationQueue.get(dependencyName);
            @Nullable Integer dependencyWeight = queuedDoorTypeInfo.weight;
            final TypeInfo queuedDoorType = queuedDoorTypeInfo.typeInfo;

            // If the dependency's dependencies haven't been checked, recursively check if they are satisfied.
            if (dependencyWeight == null)
            {
                final LoadResult dependencyLoadResult = processDependencies(queuedDoorType);
                if (dependencyLoadResult.loadResultType() == LoadResultType.DEPENDENCIES_AVAILABLE)
                    // Increment the weight by 1, to make sure that the current DoorType is loaded after this dependency
                    dependencyWeight = registrationQueue.get(dependencyName).weight;
                else
                {
                    registrationQueue.replace(currentName, new TypeInfoAndWeight(doorTypeInfo, -1));
                    return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                          "Type \"" + currentName + "\" depends on \"" + dependencyName +
                                              "\", but its dependencies could not be loaded!");
                }
            }

            newWeight = Math.max(newWeight, dependencyWeight == null ? 0 : dependencyWeight) + 1;

            if (queuedDoorType.getVersion() < dependency.minVersion() ||
                queuedDoorType.getVersion() > dependency.maxVersion())
            {
                registrationQueue.replace(currentName, new TypeInfoAndWeight(doorTypeInfo, -1));
                return new LoadResult(LoadResultType.DEPENDENCY_UNSUPPORTED_VERSION,
                                      "Version " + doorTypeInfo.getVersion() + " of type: \"" + currentName +
                                          "\" requires " + dependency.minVersion() + ">= version <= " +
                                          dependency.maxVersion() + " of type: \"" + dependency.dependencyName() +
                                          "\", but version " + queuedDoorType.getVersion() + " was found!");
            }
        }

        registrationQueue.replace(currentName, new TypeInfoAndWeight(doorTypeInfo, newWeight));
        return new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "");
    }

    public static final class TypeInfo
    {
        @Getter
        private final String typeName;
        @Getter
        private final int version;
        @Getter
        private final String mainClass;
        @Getter
        private final Path jarFile;
        @Getter
        private final List<Optional<Dependency>> dependencies;
        @Getter(AccessLevel.PRIVATE)
        @Setter(AccessLevel.PRIVATE)
        private int weight;

        private static final Pattern NAME_MATCH = Pattern.compile("^[a-zA-Z]*");
        private static final Pattern MIN_VERSION_MATCH = Pattern.compile("[0-9]*;");
        private static final Pattern MAX_VERSION_MATCH = Pattern.compile(";[0-9]*");

        public TypeInfo(String typeName, int version, String mainClass, Path jarFile, @Nullable String dependencies)
        {
            this.typeName = typeName.toLowerCase(Locale.ENGLISH);
            this.version = version;
            this.mainClass = mainClass;
            this.jarFile = jarFile;
            this.dependencies = parseDependencies(dependencies);
        }

        private List<Optional<Dependency>> parseDependencies(@Nullable String dependencies)
        {
            if (dependencies == null || dependencies.isEmpty())
                return Collections.emptyList();

            final String[] split = dependencies.split(" ");
            final List<Optional<Dependency>> ret = new ArrayList<>(split.length);

            for (int idx = 0; idx < split.length; ++idx)
            {
                final Optional<Dependency> dependency = parseDependency(split[idx]);
                if (dependency.isEmpty())
                    log.at(Level.SEVERE).log("Failed to parse dependency '%s' for type: %s", split[idx], typeName);
                ret.add(idx, dependency);
            }
            return ret;
        }

        private Optional<Dependency> parseDependency(String dependency)
        {
            final Matcher nameMatcher = NAME_MATCH.matcher(dependency);
            if (!nameMatcher.find())
            {
                log.at(Level.FINE).log("Failed to find the dependency name in: %s", dependency);
                return Optional.empty();
            }
            final String dependencyName = nameMatcher.group();

            final Matcher minVersionMatcher = MIN_VERSION_MATCH.matcher(dependency);
            if (!minVersionMatcher.find())
            {
                log.at(Level.FINE).log("Failed to find the min version in: %s", dependency);
                return Optional.empty();
            }
            String minVersionStr = minVersionMatcher.group();
            minVersionStr = minVersionStr.substring(0, minVersionStr.length() - 1);
            final OptionalInt minVersionOpt = Util.parseInt(minVersionStr);
            if (minVersionOpt.isEmpty())
            {
                log.at(Level.FINE).log("Failed to parse min version from: %s", minVersionStr);
                return Optional.empty();
            }

            final Matcher maxVersionMatcher = MAX_VERSION_MATCH.matcher(dependency);
            if (!maxVersionMatcher.find())
            {
                log.at(Level.FINE).log("Failed to find the max version in: %s", dependency);
                return Optional.empty();
            }

            String maxVersionStr = maxVersionMatcher.group();
            maxVersionStr = maxVersionStr.substring(1);
            final OptionalInt maxVersionOpt = Util.parseInt(maxVersionStr);
            if (maxVersionOpt.isEmpty())
            {
                log.at(Level.FINE).log("Failed to parse max version from: %s", maxVersionStr);
                return Optional.empty();
            }

            return Optional.of(new Dependency(dependencyName, minVersionOpt.getAsInt(), maxVersionOpt.getAsInt()));
        }
    }

    public record Dependency(String dependencyName, int minVersion, int maxVersion)
    {
        @Override
        public String toString()
        {
            return dependencyName + "(" + minVersion + "," + maxVersion + ")";
        }
    }

    private record LoadResult(LoadResultType loadResultType, String message)
    {
    }

    private enum LoadResultType
    {
        DEPENDENCY_UNSUPPORTED_VERSION,
        DEPENDENCY_UNAVAILABLE,
        DEPENDENCIES_AVAILABLE,
        INVALID_DOOR_TYPE
    }

    private record TypeInfoAndWeight(TypeInfo typeInfo, @Nullable Integer weight)
    {}
}
