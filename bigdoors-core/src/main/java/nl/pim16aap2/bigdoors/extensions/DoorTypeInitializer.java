package nl.pim16aap2.bigdoors.extensions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Pair;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull TypeInfo, @Nullable Integer>> registrationQueue;

    /**
     * Gets the list of all {@link TypeInfo}s sorted by their dependency weights. This means that {@link TypeInfo}s at
     * the lower indices need to be loaded before those at the higher indices to satisfy all dependencies.
     * <p>
     * {@link TypeInfo}s with unmet dependencies will not appear in this list.
     *
     * @return The sorted list of all {@link TypeInfo}s sorted by their dependency weights.
     */
    @Getter
    private final @NotNull List<@NotNull TypeInfo> sorted;

    private final @NotNull DoorTypeClassLoader doorTypeClassLoader;

    /**
     * Instantiates this {@link DoorTypeInitializer}. It will attempt to assign dependency weights to all entries
     * (stored in {@link #registrationQueue} and then sort those entries by their weight, starting at the lowest (stored
     * in {@link #sorted}).
     *
     * @param typeInfoList The list of {@link TypeInfo}s that should be loaded.
     */
    public DoorTypeInitializer(final @NotNull List<TypeInfo> typeInfoList,
                               final @NotNull DoorTypeClassLoader doorTypeClassLoader)
    {
        this.doorTypeClassLoader = doorTypeClassLoader;
        registrationQueue = new HashMap<>(typeInfoList.size())
        {
            @Override
            public @Nullable Pair<TypeInfo, Integer> put(final @NotNull String key,
                                                         final @NotNull Pair<@NotNull TypeInfo, @Nullable Integer> value)
            {
                return super.put(key.toLowerCase(), value);
            }
        };

        typeInfoList.forEach(info -> registrationQueue.put(info.getTypeName(), new Pair<>(info, null)));
        registrationQueue.forEach(
            (name, pair) ->
            {
                final @NotNull LoadResult loadResult = processDependencies(pair.first);
                if (loadResult.loadResultType != LoadResultType.DEPENDENCIES_AVAILABLE &&
                    (!loadResult.message.isEmpty()))
                    PLogger.get().warn(loadResult.message);
            });

        sorted = getSortedDoorTypeInfo();
        PLogger.get().logMessage(Level.FINER, this::sortedDependenciesToString);
    }

    /**
     * Constructs {@link #sorted} from {@link #registrationQueue}. The sorted list contains all {@link TypeInfo} with a
     * valid dependency weight and it is sorted by this weight value. Lower weights are assigned lower indices.
     *
     * @return The sorted list of {@link TypeInfo}s with valid weights.
     */
    private @NotNull List<TypeInfo> getSortedDoorTypeInfo()
    {
        final @NotNull List<TypeInfo> sorted = new ArrayList<>(registrationQueue.size());
        registrationQueue.forEach(
            (name, pair) ->
            {
                if (pair.second != null && pair.second >= 0)
                {
                    pair.first.setWeight(pair.second);
                    sorted.add(pair.first);
                }
            });
        sorted.sort(Comparator.comparing(TypeInfo::getWeight));
        return sorted;
    }

    /**
     * Formats the {@link #sorted} list into a pretty string.
     *
     * @return The formatted String representing {@link #sorted}.
     */
    private @NotNull String sortedDependenciesToString()
    {
        final @NotNull StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < sorted.size(); ++idx)
        {
            final @NotNull TypeInfo info = sorted.get(idx);
            final @NotNull StringBuilder depSB = new StringBuilder();
            info.getDependencies().forEach(dependencyOpt -> dependencyOpt
                .ifPresent(dependency -> depSB.append(dependency.dependencyName).append(" ")));

            sb.append(String.format("(%-2d) Weight: %-2d type: %-15s dependencies: %s",
                                    idx, info.weight, info.getTypeName(), depSB.toString())).append("\n");
        }
        return sb.toString();
    }

    /**
     * Attempts to load a jar.
     *
     * @param file The jar file.
     * @return True if the jar loaded successfully.
     */
    private boolean loadJar(final @NotNull File file)
    {
        try
        {
            doorTypeClassLoader.addURL(file.toURI().toURL());
        }
        catch (Throwable e)
        {
            PLogger.get().logThrowable(Level.FINE, e);
            return false;
        }
        return true;
    }

    /**
     * Attempts to load a {@link TypeInfo} from its {@link TypeInfo#getMainClass()}.
     *
     * @param typeInfo The {@link TypeInfo} to load.
     * @return The {@link DoorType} that resulted from loading the {@link TypeInfo}, if possible.
     */
    private @NotNull Optional<DoorType> loadDoorType(final @NotNull TypeInfo typeInfo)
    {
        PLogger.get().logMessage(Level.FINE, "Trying to load type: " + typeInfo.getTypeName());

        if (!loadJar(typeInfo.jarFile))
        {
            PLogger.get().logMessage(Level.WARNING,
                                     "Failed to load file: \"" + typeInfo.getJarFile().toString() +
                                         "\"! This type (\"" + typeInfo.getTypeName() +
                                         "\") will not be loaded! See the log for more details.");
            return Optional.empty();
        }

        final @NotNull DoorType doorType;
        try
        {
            final @NotNull Class<?> typeClass = doorTypeClassLoader.loadClass(typeInfo.mainClass);
            final @NotNull Method getter = typeClass.getDeclaredMethod("get");
            doorType = (DoorType) getter.invoke(null);
        }
        catch (Throwable e)
        {
            PLogger.get().logThrowable(e, "Failed to load extension: " + typeInfo.getTypeName());
            return Optional.empty();
        }

        PLogger.get().logMessage(Level.FINE,
                                 "Loaded BigDoors extension: " + Util.capitalizeFirstLetter(doorType.getSimpleName()));
        return Optional.of(doorType);
    }

    /**
     * Attempts to load all {@link DoorType} from {@link #sorted}.
     *
     * @return The {@link DoorType}s that resulted from loading the {@link TypeInfo}s.
     */
    public @NotNull List<DoorType> loadDoorTypes()
    {
        final @NotNull List<DoorType> ret = new ArrayList<>(getSorted().size());
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
     * @param doorTypeInfo The {@link TypeInfo} whose dependencies to check.
     * @return The {@link LoadResult} of the current {@link DoorType}.
     */
    private @NotNull LoadResult processDependencies(final @NotNull TypeInfo doorTypeInfo)
    {
        final @NotNull String currentName = doorTypeInfo.getTypeName();
        final @Nullable Pair<TypeInfo, Integer> currentStatus = registrationQueue.get(doorTypeInfo.typeName);
        if (currentStatus == null)
            return new LoadResult(LoadResultType.INVALID_DOOR_TYPE,
                                  "Type " + doorTypeInfo.getTypeName() + " was not mapped!");

        // If 'currentStatus.status' (the weight) of the current entry is not null, then it has already been calculated.
        if (currentStatus.second != null)
            return currentStatus.second == -1 ?
                   new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "") :
                   new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE, "");
        int newWeight = 0;

        for (final @NotNull Optional<Dependency> dependencyOpt : doorTypeInfo.getDependencies())
        {
            if (dependencyOpt.isEmpty())
            {
                registrationQueue.replace(currentName, new Pair<>(doorTypeInfo, -1));
                return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                      currentName + ": Failed to find dependency!");
            }
            final @NotNull Dependency dependency = dependencyOpt.get();
            final @NotNull String dependencyName = dependency.getDependencyName();

            // If the dependency has already been registered, it has already been loaded, obviously.
            if (DoorTypeManager.get().getDoorType(dependencyName).isPresent())
            {
                registrationQueue.replace(currentName, new Pair<>(doorTypeInfo, 0));
                return new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "");
            }

            // If it hasn't been registered yet, check if it exists in the registerQueue.
            // If the dependency will be installed in the future, then that's fine.
            if (!registrationQueue.containsKey(dependencyName))
            {
                registrationQueue.replace(currentName, new Pair<>(doorTypeInfo, -1));
                return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                      "Type \"" + currentName + "\" depends on type: \"" +
                                          dependencyName + "\" which isn't installed!");
            }

            // Before we just assume any old TypeInfo we find that will be registered in the future, we'll
            // have to make sure that that dependency's dependencies are also met.
            final @NotNull Pair<TypeInfo, Integer> queuedDoorTypeInfo = registrationQueue.get(dependencyName);
            @Nullable Integer dependencyWeight = queuedDoorTypeInfo.second;
            final @NotNull TypeInfo queuedDoorType = queuedDoorTypeInfo.first;

            // If the dependency's dependencies haven't been checked, recursively check if they are satisfied.
            if (dependencyWeight == null)
            {
                final @NotNull LoadResult dependencyLoadResult = processDependencies(queuedDoorType);
                if (dependencyLoadResult.getLoadResultType() == LoadResultType.DEPENDENCIES_AVAILABLE)
                    // Increment the weight by 1, to make sure that the current DoorType is loaded after this dependency.
                    dependencyWeight = registrationQueue.get(dependencyName).second;
                else
                {
                    registrationQueue.replace(currentName, new Pair<>(doorTypeInfo, -1));
                    return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                          "Type \"" + currentName + "\" depends on \"" + dependencyName +
                                              "\", but its dependencies could not be loaded!");
                }
            }

            newWeight = Math.max(newWeight, dependencyWeight) + 1;

            if (queuedDoorType.getVersion() < dependency.getMinVersion() ||
                queuedDoorType.getVersion() > dependency.getMaxVersion())
            {
                registrationQueue.replace(currentName, new Pair<>(doorTypeInfo, -1));
                return new LoadResult(LoadResultType.DEPENDENCY_UNSUPPORTED_VERSION,
                                      "Version " + doorTypeInfo.getVersion() + " of type: \"" + currentName +
                                          "\" requires " + dependency.getMinVersion() + ">= version <= " +
                                          dependency.getMaxVersion() + " of type: \"" + dependency.getDependencyName() +
                                          "\", but version " + queuedDoorType.getVersion() + " was found!");
            }
        }

        registrationQueue.replace(currentName, new Pair<>(doorTypeInfo, newWeight));
        return new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "");
    }

//    /**
//     * Tries to load all the provided {@link DoorType}s as defined by their {@link TypeInfo}.
//     *
//     * @param typeInfo The list of {@link TypeInfo}s defining {@link DoorType}s that will be loaded.
//     * @return All the {@link DoorType}s that were loaded successfully.
//     */
//    public static @NotNull List<DoorType> loadDoorTypes(final @NotNull List<TypeInfo> typeInfo)
//    {
//        return new DoorTypeInitializer(typeInfo).loadDoorTypes();
//    }

    public static final class TypeInfo
    {
        @Getter
        private final @NotNull String typeName;
        @Getter
        private final int version;
        @Getter
        private final @NotNull String mainClass;
        @Getter
        private final @NotNull File jarFile;
        @Getter
        private final @NotNull List<Optional<Dependency>> dependencies;
        @Getter(AccessLevel.PRIVATE)
        @Setter(AccessLevel.PRIVATE)
        private int weight;

        private static final Pattern NAME_MATCH = Pattern.compile("^[a-zA-Z]*");
        private static final Pattern MIN_VERSION_MATCH = Pattern.compile("[0-9]*;");
        private static final Pattern MAX_VERSION_MATCH = Pattern.compile(";[0-9]*");

        public TypeInfo(final @NotNull String typeName, final int version, final @NotNull String mainClass,
                        final @NotNull File jarFile, final @Nullable String dependencies)
        {
            this.typeName = typeName.toLowerCase();
            this.version = version;
            this.mainClass = mainClass;
            this.jarFile = jarFile;
            this.dependencies = parseDependencies(dependencies);
        }

        private @NotNull List<Optional<Dependency>> parseDependencies(final @Nullable String dependencies)
        {
            if (dependencies == null || dependencies.isEmpty())
                return Collections.emptyList();

            final @NotNull String[] split = dependencies.split(" ");
            final @NotNull List<Optional<Dependency>> ret = new ArrayList<>(split.length);

            for (int idx = 0; idx < split.length; ++idx)
            {
                final @NotNull Optional<Dependency> dependency = parseDependency(split[idx]);
                if (dependency.isEmpty())
                    PLogger.get().severe("Failed to parse dependency \"" + split[idx] + "\" for type: " + typeName);
                ret.add(idx, dependency);
            }
            return ret;
        }

        private @NotNull Optional<Dependency> parseDependency(final @NotNull String dependency)
        {
            final @NotNull Matcher nameMatcher = NAME_MATCH.matcher(dependency);
            if (!nameMatcher.find())
            {
                PLogger.get().logMessage(Level.FINE, "Failed to find the dependency name in: " + dependency);
                return Optional.empty();
            }
            final @NotNull String dependencyName = nameMatcher.group();

            final @NotNull Matcher minVersionMatcher = MIN_VERSION_MATCH.matcher(dependency);
            if (!minVersionMatcher.find())
            {
                PLogger.get().logMessage(Level.FINE, "Failed to find the min version in: " + dependency);
                return Optional.empty();
            }
            @NotNull String minVersionStr = minVersionMatcher.group();
            minVersionStr = minVersionStr.substring(0, minVersionStr.length() - 1);
            final @NotNull OptionalInt minVersionOpt = Util.parseInt(minVersionStr);
            if (minVersionOpt.isEmpty())
            {
                PLogger.get().logMessage(Level.FINE, "Failed to parse min version from: " + minVersionStr);
                return Optional.empty();
            }

            final @NotNull Matcher maxVersionMatcher = MAX_VERSION_MATCH.matcher(dependency);
            if (!maxVersionMatcher.find())
            {
                PLogger.get().logMessage(Level.FINE, "Failed to find the max version in: " + dependency);
                return Optional.empty();
            }

            @NotNull String maxVersionStr = maxVersionMatcher.group();
            maxVersionStr = maxVersionStr.substring(1);
            final @NotNull OptionalInt maxVersionOpt = Util.parseInt(maxVersionStr);
            if (maxVersionOpt.isEmpty())
            {
                PLogger.get().logMessage(Level.FINE, "Failed to parse max version from: " + maxVersionStr);
                return Optional.empty();
            }

            return Optional.of(new Dependency(dependencyName, minVersionOpt.getAsInt(), maxVersionOpt.getAsInt()));
        }
    }

    @Value
    public static class Dependency
    {
        String dependencyName;
        int minVersion;
        int maxVersion;

        @Override
        public @NotNull String toString()
        {
            return dependencyName + "(" + minVersion + "," + maxVersion + ")";
        }
    }

    @Value
    private static class LoadResult
    {
        LoadResultType loadResultType;
        String message;
    }

    private enum LoadResultType
    {
        DEPENDENCY_UNSUPPORTED_VERSION,
        DEPENDENCY_UNAVAILABLE,
        DEPENDENCIES_AVAILABLE,
        INVALID_DOOR_TYPE
    }
}
