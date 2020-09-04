package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.PLogger;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
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

    private @NotNull Optional<DoorTypeInitializer.TypeInfo> getDoorTypeInfo(final @NotNull File file)
    {
        PLogger.get().logMessage(Level.INFO, "Attempting to load DoorType from jar: " + file.toString());
        if (!file.toString().endsWith(".jar"))
        {
            PLogger.get()
                   .logThrowable(new IllegalArgumentException("\"" + file.toString() + "\" is not a valid jar file!"));
            return Optional.empty();
        }

        final @NotNull String typeName;
        final @NotNull String className;
        final @Nullable String dependencies;
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
            if (!versionOpt.isPresent())
            {
                PLogger.get().logThrowable(new IllegalArgumentException("File: \"" + file.toString() +
                                                                            "\" does not specify its version!"));
                return Optional.empty();
            }
            version = versionOpt.getAsInt();

            final @Nullable Attributes dependencySection = manifest.getEntries().get("Dependencies");
            dependencies = dependencySection != null ? dependencySection.getValue("Dependencies") : null;
        }
        catch (IOException | IllegalArgumentException e)
        {
            PLogger.get().logThrowable(e);
            return Optional.empty();
        }

        return Optional.of(new DoorTypeInitializer.TypeInfo(typeName, version, className, file, dependencies));
    }

    /**
     * Attempts to load a jar file.
     *
     * @param file The jar file.
     */
    public @NotNull Optional<DoorType> loadDoorType(final @NotNull File file)
    {
        final @NotNull Optional<DoorTypeInitializer.TypeInfo> typeInfo = getDoorTypeInfo(file);
        if (!typeInfo.isPresent())
            return Optional.empty();


        final @NotNull List<DoorType> loadDoorTypes = DoorTypeInitializer
            .loadDoorTypes(Collections.singletonList(typeInfo.get()));

        return loadDoorTypes.size() == 1 ? Optional.of(loadDoorTypes.get(0)) : Optional.empty();
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
        final @NotNull List<DoorTypeInitializer.TypeInfo> typeInfos = new ArrayList<>();

        System.out.println("Checking directory: " + Paths.get(directory).toAbsolutePath().toString());

        try (final @NotNull Stream<Path> walk = Files.walk(Paths.get(directory), 1, FileVisitOption.FOLLOW_LINKS))
        {
            final @NotNull Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getDoorTypeInfo(path.toFile()).ifPresent(typeInfos::add));
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e);
        }

        return DoorTypeInitializer.loadDoorTypes(typeInfos);
    }
}

