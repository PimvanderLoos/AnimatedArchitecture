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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

class DoorTypeInitializer
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

    private DoorTypeInitializer(final @NotNull List<TypeInfo> typeInfoList)
    {
        registrationQueue = new HashMap<String, Pair<TypeInfo, Integer>>(typeInfoList.size())
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
                final @NotNull LoadResult loadResult = verifyDependencies(pair.first);
                if (loadResult.loadResultType != LoadResultType.DEPENDENCIES_AVAILABLE &&
                    (!loadResult.message.isEmpty()))
                    PLogger.get().warn(loadResult.message);
            });
    }

    private @NotNull Optional<DoorType> loadDoorType(final @NotNull TypeInfo typeInfo)
    {
        PLogger.get().logMessage(Level.FINE, "Trying to load type: " + typeInfo.getTypeName());
        final @NotNull Class<?> typeClass;
        try
        {
            final @NotNull ClassLoader classLoader =
                URLClassLoader.newInstance(new URL[]{typeInfo.getJarFile().toURI().toURL()},
                                           DoorTypeLoader.class.getClassLoader());
            typeClass = Class.forName(typeInfo.getMainClass(), true, classLoader);
        }
        catch (MalformedURLException | ClassNotFoundException e)
        {
            PLogger.get().logThrowable(e);
            return Optional.empty();
        }

        final @NotNull DoorType doorType;
        try
        {
            final @NotNull Method getter = typeClass.getDeclaredMethod("get");
            doorType = (DoorType) getter.invoke(null);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            PLogger.get().logThrowable(e);
            return Optional.empty();
        }

        PLogger.get().logMessage(Level.FINE,
                                 "Loaded BigDoors extension: " + Util.capitalizeFirstLetter(doorType.getSimpleName()));
        return Optional.of(doorType);
    }

    private @NotNull List<DoorType> loadDoorTypes()
    {
        final @NotNull List<TypeInfo> sortedInfo = getSortedDoorTypeInfo();
        final @NotNull List<DoorType> ret = new ArrayList<>(sortedInfo.size());
        sortedInfo.forEach(doorInfo -> loadDoorType(doorInfo).ifPresent(ret::add));
        return ret;
    }

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

    private @NotNull LoadResult verifyDependencies(final @NotNull TypeInfo doorTypeInfo)
    {
        final @NotNull String currentName = doorTypeInfo.getTypeName();
        final @Nullable Pair<TypeInfo, Integer> currentStatus = registrationQueue.get(doorTypeInfo.typeName);
        if (currentStatus == null)
            return new LoadResult(LoadResultType.INVALID_DOOR_TYPE,
                                  "Type " + doorTypeInfo.getTypeName() + " was not mapped!");

        if (currentStatus.second != null)
            return currentStatus.second == -1 ?
                   new LoadResult(LoadResultType.DEPENDENCIES_AVAILABLE, "") :
                   new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE, "");
        int newWeight = 0;

        for (final @NotNull Optional<Dependency> dependencyOpt : doorTypeInfo.getDependencies())
        {
            if (!dependencyOpt.isPresent())
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
                final @NotNull LoadResult dependencyLoadResult = verifyDependencies(queuedDoorType);
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

    /**
     * Tries to load all the provided {@link DoorType}s as defined by their {@link TypeInfo}.
     *
     * @param typeInfo The list of {@link TypeInfo}s defining {@link DoorType}s that will be loaded.
     * @return All the {@link DoorType}s that were loaded successfully.
     */
    public static @NotNull List<DoorType> loadDoorTypes(final @NotNull List<TypeInfo> typeInfo)
    {
        return new DoorTypeInitializer(typeInfo).loadDoorTypes();
    }

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
                if (!dependency.isPresent())
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
            if (!minVersionOpt.isPresent())
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
            if (!maxVersionOpt.isPresent())
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
