package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

public final class ExtensionLoader
{
    @NotNull
    private static final ExtensionLoader INSTANCE = new ExtensionLoader();

    private ExtensionLoader()
    {
    }

    @NotNull
    public static ExtensionLoader get()
    {
        return INSTANCE;
    }


    /**
     * Attempts to load a jar file.
     *
     * @param file The jar file.
     */
    public @NotNull Optional<DoorType> loadDoorType(final @NotNull File file)
    {
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
                                           ExtensionLoader.class.getClassLoader());
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

    /**
     * Attempts to load a jar file that contains and describes an {@link DoorType}.
     *
     * @param file The jar file.
     */
    public @NotNull Optional<DoorType> loadDoorType(final @NotNull String file)
    {
        return loadDoorType(new File(file));
    }

    /**
     * Attempts to load all jars in a given directory.
     *
     * @param directory The directory.
     */
    public @NotNull List<DoorType> loadDoorTypesFromDirectory(final @NotNull String directory)
    {
        final @NotNull List<DoorType> doorTypes = new ArrayList<>();

        try (final @NotNull Stream<Path> walk = Files.walk(Paths.get(directory)))
        {
            final @NotNull Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> loadDoorType(path.toFile()).ifPresent(doorTypes::add));
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e);
        }

        return doorTypes;
    }
}

