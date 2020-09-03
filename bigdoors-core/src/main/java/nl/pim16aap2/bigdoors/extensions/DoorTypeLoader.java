package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class DoorTypeLoader
{
    @NotNull
    private static final DoorTypeLoader INSTANCE = new DoorTypeLoader();

    private DoorTypeLoader()
    {
    }

    @NotNull
    public static DoorTypeLoader get()
    {
        return INSTANCE;
    }

    private @NotNull Optional<DoorType> getDoorType(final @NotNull File file)
    {
        PLogger.get().logMessage(Level.SEVERE, "Attempting to load DoorType from jar: " + file.toString());
        if (!file.toString().endsWith(".jar"))
        {
            PLogger.get()
                   .logThrowable(new IllegalArgumentException("\"" + file.toString() + "\" is not a valid jar file!"));
            return Optional.empty();
        }

        final @NotNull String className;
        try (final @NotNull FileInputStream fileInputStream = new FileInputStream(file);
             final @NotNull JarInputStream jarStream = new JarInputStream(fileInputStream))
        {
            className = jarStream.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (className == null)
                throw new IllegalArgumentException("File: \"" + file.toString() +
                                                       "\" does not specify its main class!");
        }
        catch (IOException | IllegalArgumentException e)
        {
            PLogger.get().logThrowable(e);
            return Optional.empty();
        }

        final @NotNull Class<?> typeClass;
        try
        {
            final @NotNull ClassLoader classLoader =
                URLClassLoader.newInstance(new URL[]{file.toURI().toURL()},
                                           DoorTypeLoader.class.getClassLoader());
            typeClass = Class.forName(className, true, classLoader);
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
        PLogger.get().info("Loaded BigDoors extension: " + Util.capitalizeFirstLetter(doorType.getSimpleName()));
        return Optional.of(doorType);
    }

//    private @NotNull LoadResult attemptLoad(final @NotNull DoorType type)
//    {
//        for (Pair<String, Pair<Integer, Integer>> dependency : type.getDependencies())
//        {
//            final @NotNull Optional<DoorType> dependencyDoorType = DoorTypeManager.get().getDoorType(dependency.first);
//            if (!dependencyDoorType.isPresent())
//                return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
//                                      "Type \"" + type.getSimpleName() + "\" depends on type: \"" + dependency.first +
//                                          "\" which isn't installed!");
//
//            if (dependencyDoorType.get().getTypeVersion() < dependency.second.first ||
//                dependencyDoorType.get().getTypeVersion() > dependency.second.second)
//                return new LoadResult(LoadResultType.DEPENDENCY_UNSUPPORTED_VERSION,
//                                      "Version " + type.getTypeVersion() + " of type: \"" + type.getSimpleName() +
//                                          "\" requires " + dependency.second.first + ">= version <= " +
//                                          dependency.second.second + " of type: \"" +
//                                          dependencyDoorType.get().getSimpleName() + "\", but version " +
//                                          dependencyDoorType.get().getTypeVersion() + " was found!");
//        }
//        return new LoadResult(LoadResultType.SUCCESS, "");
//    }

    /**
     * Attempts to load a jar file.
     *
     * @param file The jar file.
     */
    public @NotNull Optional<DoorType> loadDoorType(final @NotNull File file)
    {
//        final @NotNull Optional<DoorType> doorTypeOpt = getDoorType(file);
//        final @NotNull LoadResult loadResult = doorTypeOpt.map(this::attemptLoad)
//                                                          .orElse(new LoadResult(LoadResultType.INVALID_DOOR_TYPE,
//                                                                                 "File: \"" + file.toString() +
//                                                                                     "\" does not contain a valid door type!"));
//        if (loadResult.getLoadResultType() == LoadResultType.SUCCESS)
//            return doorTypeOpt;
//        PLogger.get().severe(loadResult.getMessage());
//        return Optional.empty();
        return getDoorType(file);
    }

    /**
     * Attempts to load a jar file that contains and describes an {@link DoorType}.
     *
     * @param file The jar file.
     */
    public @NotNull Optional<DoorType> loadDoorType(final @NotNull String file)
    {
        return getDoorType(new File(file));
    }

    /**
     * Attempts to load all jars in a given directory.
     *
     * @param directory The directory.
     */
    public @NotNull List<DoorType> loadDoorTypesFromDirectory(final @NotNull String directory)
    {
        final @NotNull List<DoorType> doorTypes = new ArrayList<>();

        System.out.println("Checking directory: " + Paths.get(directory).toAbsolutePath().toString());

        try (final @NotNull Stream<Path> walk = Files.walk(Paths.get(directory), 1, FileVisitOption.FOLLOW_LINKS))
        {
            final @NotNull Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getDoorType(path.toFile()).ifPresent(doorTypes::add));
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e);
        }

        return doorTypes;
    }
}

