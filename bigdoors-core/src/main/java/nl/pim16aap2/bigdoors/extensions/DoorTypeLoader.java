package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class DoorTypeLoader extends Restartable
{
    private static final @NotNull DoorTypeLoader INSTANCE = new DoorTypeLoader();
    private @NotNull DoorTypeClassLoader doorTypeClassLoader = new DoorTypeClassLoader(getClass().getClassLoader());

    private DoorTypeLoader()
    {
        super(BigDoors.get());
        init();
    }

    private void init()
    {
        doorTypeClassLoader = new DoorTypeClassLoader(doorTypeClassLoader);
    }

    private void deregisterDoorTypes()
    {
        try
        {
            doorTypeClassLoader.close();
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e, "Failed to close door type classloader! Extensions will NOT be reloaded!");
        }
    }

    @NotNull
    public static DoorTypeLoader get()
    {
        return INSTANCE;
    }

    private @NotNull Optional<DoorTypeInitializer.TypeInfo> getDoorTypeInfo(final @NotNull File file)
    {
        PLogger.get().logMessage(Level.INFO, "Attempting to load DoorType from jar: " + file.toString());
        if (!file.toString().endsWith(".jar"))
        {
            PLogger.get()
                   .logThrowable(new IllegalArgumentException("\"" + file.toString() + "\" is not a valid jar file!"));
            return Optional.empty();
        }

        final String typeName;
        final @NotNull String className;
        @Nullable String dependencies;
        final int version;
        try (final @NotNull FileInputStream fileInputStream = new FileInputStream(file);
             final @NotNull JarInputStream jarStream = new JarInputStream(fileInputStream))
        {
            final @NotNull Manifest manifest = jarStream.getManifest();
            className = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (className == null)
            {
                PLogger.get().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
                                                                            "\" does not specify its main class!"));
                return Optional.empty();
            }

            final @Nullable Attributes typeNameSection = manifest.getEntries().get("TypeName");
            typeName = typeNameSection != null ? typeNameSection.getValue("TypeName") : null;
            if (typeName == null)
            {
                PLogger.get().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
                                                                            "\" does not specify its type name!"));
                return Optional.empty();
            }

            final @Nullable Attributes versionSection = manifest.getEntries().get("Version");
            final @NotNull OptionalInt versionOpt = Util.parseInt(versionSection == null ?
                                                                  null : versionSection.getValue("Version"));
            if (versionOpt.isEmpty())
            {
                PLogger.get().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
                                                                            "\" does not specify its version!"));
                return Optional.empty();
            }
            version = versionOpt.getAsInt();

            final @Nullable Attributes dependencySection = manifest.getEntries().get("TypeDependencies");
            dependencies = dependencySection != null ? dependencySection.getValue("TypeDependencies") : null;
            // When no dependencies are provided, we don't get a null reference, but a "null" string instead.
            dependencies = "null".equals(dependencies) ? null : dependencies;
        }
        catch (IOException | IllegalArgumentException e)
        {
            PLogger.get().logThrowable(e);
            return Optional.empty();
        }

        return Optional.of(new DoorTypeInitializer.TypeInfo(typeName, version, className, file, dependencies));
    }

    public void loadDoorTypesFromDirectory()
    {
        final @NotNull List<DoorType> doorTypes =
            loadDoorTypesFromDirectory(BigDoors.get().getPlatform().getDataDirectory() +
                                           Constants.BIGDOORS_EXTENSIONS_FOLDER);
        DoorTypeManager.get().registerDoorTypes(doorTypes);
    }

    /**
     * Attempts to load all jars in a given directory.
     *
     * @param directory The directory.
     */
    private @NotNull List<DoorType> loadDoorTypesFromDirectory(final @NotNull String directory)
    {
        final @NotNull List<DoorTypeInitializer.TypeInfo> typeInfos = new ArrayList<>();

        try (final @NotNull Stream<Path> walk = Files.walk(Paths.get(directory), 1, FileVisitOption.FOLLOW_LINKS))
        {
            final @NotNull Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getDoorTypeInfo(path.toFile()).ifPresent(typeInfos::add));
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e);
        }

        return new DoorTypeInitializer(typeInfos, doorTypeClassLoader).loadDoorTypes();
    }

    @Override
    public void restart()
    {
        init();
        loadDoorTypesFromDirectory();
    }

    @Override
    public void shutdown()
    {
        DoorTypeManager.get().shutdown();
        deregisterDoorTypes();
    }
}

