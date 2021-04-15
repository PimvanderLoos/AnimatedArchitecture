package nl.pim16aap2.bigdoors.extensions;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
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
    private static final @NonNull DoorTypeLoader INSTANCE = new DoorTypeLoader();
    private @NonNull DoorTypeClassLoader doorTypeClassLoader = new DoorTypeClassLoader(getClass().getClassLoader());

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
            BigDoors.get().getPLogger()
                    .logThrowable(e, "Failed to close door type classloader! Extensions will NOT be reloaded!");
        }
    }

    @NonNull
    public static DoorTypeLoader get()
    {
        return INSTANCE;
    }

    private @NonNull Optional<DoorTypeInitializer.TypeInfo> getDoorTypeInfo(final @NonNull File file)
    {
        BigDoors.get().getPLogger().logMessage(Level.FINE, "Attempting to load DoorType from jar: " + file.toString());
        if (!file.toString().endsWith(".jar"))
        {
            BigDoors.get().getPLogger()
                    .logThrowable(new IllegalArgumentException("\"" + file.toString() + "\" is not a valid jar file!"));
            return Optional.empty();
        }

        final String typeName;
        final @NonNull String className;
        @Nullable String dependencies;
        final int version;
        try (final @NonNull FileInputStream fileInputStream = new FileInputStream(file);
             final @NonNull JarInputStream jarStream = new JarInputStream(fileInputStream))
        {
            final @NonNull Manifest manifest = jarStream.getManifest();
            className = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (className == null)
            {
                BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
                                                                                          "\" does not specify its main class!"));
                return Optional.empty();
            }

            final @Nullable Attributes typeNameSection = manifest.getEntries().get("TypeName");
            typeName = typeNameSection != null ? typeNameSection.getValue("TypeName") : null;
            if (typeName == null)
            {
                BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
                                                                                          "\" does not specify its type name!"));
                return Optional.empty();
            }

            final @Nullable Attributes versionSection = manifest.getEntries().get("Version");
            final @NonNull OptionalInt versionOpt = Util.parseInt(versionSection == null ?
                                                                  null : versionSection.getValue("Version"));
            if (versionOpt.isEmpty())
            {
                BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
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
            BigDoors.get().getPLogger().logThrowable(e);
            return Optional.empty();
        }

        return Optional.of(new DoorTypeInitializer.TypeInfo(typeName, version, className, file, dependencies));
    }

    /**
     * Attempts to load and register all jars in the default directory:
     * <p>
     * {@link IBigDoorsPlatform#getDataDirectory()} + {@link Constants#BIGDOORS_EXTENSIONS_FOLDER}.
     * <p>
     * See also {@link #loadDoorTypesFromDirectory(String)}.
     */
    public @NonNull List<DoorType> loadDoorTypesFromDirectory()
    {
        return loadDoorTypesFromDirectory(
            BigDoors.get().getPlatform().getDataDirectory() + Constants.BIGDOORS_EXTENSIONS_FOLDER);
    }

    /**
     * Attempts to load and register all jars in a given directory.
     *
     * @param directory The directory.
     * @return The list of {@link DoorType}s that were loaded successfully.
     */
    public @NonNull List<DoorType> loadDoorTypesFromDirectory(final @NonNull String directory)
    {
        final @NonNull List<DoorTypeInitializer.TypeInfo> typeInfos = new ArrayList<>();

        try (final @NonNull Stream<Path> walk = Files.walk(Paths.get(directory), 1, FileVisitOption.FOLLOW_LINKS))
        {
            final @NonNull Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getDoorTypeInfo(path.toFile()).ifPresent(typeInfos::add));
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
        }

        List<DoorType> types = new DoorTypeInitializer(typeInfos, doorTypeClassLoader).loadDoorTypes();
        BigDoors.get().getDoorTypeManager().registerDoorTypes(types);
        return types;
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
        BigDoors.get().getDoorTypeManager().shutdown();
        deregisterDoorTypes();
    }
}

