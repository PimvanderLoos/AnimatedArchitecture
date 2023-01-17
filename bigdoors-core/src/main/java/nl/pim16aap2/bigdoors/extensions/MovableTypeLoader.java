package nl.pim16aap2.bigdoors.extensions;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

@Singleton
@Flogger
public final class MovableTypeLoader extends Restartable
{
    private final ClassLoader classLoader = getClass().getClassLoader();
    private @Nullable MovableTypeClassLoader movableTypeClassLoader;

    private final MovableTypeManager movableTypeManager;
    private final IConfigLoader configLoader;
    private final Path extensionsDirectory;
    private boolean successfulInit;

    @Inject
    public MovableTypeLoader(
        RestartableHolder holder, MovableTypeManager movableTypeManager, IConfigLoader configLoader,
        @Named("pluginBaseDirectory") Path dataDirectory)
    {
        super(holder);
        this.movableTypeManager = movableTypeManager;
        this.configLoader = configLoader;
        extensionsDirectory = dataDirectory.resolve(Constants.BIGDOORS_EXTENSIONS_FOLDER_NAME);
    }

    private void unloadMovableTypes()
    {
        if (movableTypeClassLoader == null)
            return;

        try
        {
            movableTypeClassLoader.close();
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e)
               .log("Failed to close movable type classloader! Extensions will NOT be loaded!");
        }
    }

    /**
     * Ensure that the {@link #extensionsDirectory} exists.
     *
     * @return True if the {@link #extensionsDirectory} exists or could be created.
     */
    private boolean ensureDirectoryExists()
    {
        try
        {
            if (!Files.isDirectory(extensionsDirectory))
                Files.createDirectories(extensionsDirectory);
            return true;
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to create directory: %s", extensionsDirectory);
        }
        return false;
    }

    private Optional<MovableTypeInfo> getMovableTypeInfo(Path file)
    {
        log.atFine().log("Attempting to load MovableType from jar: %s", file);
        if (!file.toString().endsWith(".jar"))
        {
            log.atSevere().withCause(new IllegalArgumentException("\"" + file + "\" is not a valid jar file!")).log();
            return Optional.empty();
        }

        final @Nullable String typeName;
        final String className;
        @Nullable String dependencies;
        final int version;

        try (InputStream fileInputStream = Files.newInputStream(file);
             JarInputStream jarStream = new JarInputStream(fileInputStream))
        {
            final Manifest manifest = jarStream.getManifest();
            className = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (className == null)
            {
                log.atSevere().withCause(new IllegalArgumentException(
                    "File: \"" + file + "\" does not specify its main class!")).log();
                return Optional.empty();
            }

            final @Nullable Attributes typeNameSection = manifest.getEntries().get("TypeName");
            typeName = typeNameSection == null ? null : typeNameSection.getValue("TypeName");
            if (typeName == null)
            {
                log.atSevere().withCause(new IllegalArgumentException(
                    "File: \"" + file + "\" does not specify its type name!")).log();
                return Optional.empty();
            }

            final @Nullable Attributes versionSection = manifest.getEntries().get("Version");
            final OptionalInt versionOpt = Util.parseInt(versionSection == null ?
                                                         null : versionSection.getValue("Version"));
            if (versionOpt.isEmpty())
            {
                log.atSevere().withCause(new IllegalArgumentException(
                    "File: \"" + file + "\" does not specify its version!")).log();
                return Optional.empty();
            }
            version = versionOpt.getAsInt();

            final @Nullable Attributes dependencySection = manifest.getEntries().get("TypeDependencies");
            dependencies = dependencySection == null ? null : dependencySection.getValue("TypeDependencies");
            // When no dependencies are provided, we don't get a null reference, but a "null" string instead.
            dependencies = "null".equals(dependencies) ? null : dependencies;
        }
        catch (IOException | IllegalArgumentException e)
        {
            log.atSevere().withCause(e).log();
            return Optional.empty();
        }

        return Optional.of(new MovableTypeInfo(typeName, version, className, file, dependencies));
    }

    /**
     * Attempts to load and register all jars in the default directory, which is the base plugin directory +
     * {@link Constants#BIGDOORS_EXTENSIONS_FOLDER_NAME}.
     * <p>
     * See also {@link #loadMovableTypesFromDirectory(Path)}.
     */
    @SuppressWarnings("UnusedReturnValue")
    public List<MovableType> loadMovableTypesFromDirectory()
    {
        return loadMovableTypesFromDirectory(extensionsDirectory);
    }

    /**
     * Attempts to load and register all jars in a given directory.
     *
     * @param directory
     *     The directory.
     * @return The list of {@link MovableType}s that were loaded successfully.
     */
    public List<MovableType> loadMovableTypesFromDirectory(Path directory)
    {
        if (movableTypeClassLoader == null)
        {
            log.atSevere()
               .log("Trying to load movable types from directory %s, but the movable type classloader does not exist!",
                    directory);
            return Collections.emptyList();
        }

        final List<MovableTypeInfo> typeInfoList = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS))
        {
            final Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getMovableTypeInfo(path).ifPresent(typeInfoList::add));
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log();
        }

        final List<MovableType> types =
            new MovableTypeInitializer(typeInfoList, movableTypeClassLoader, configLoader.debug()).loadMovableTypes();
        movableTypeManager.registerMovableTypes(types);
        return types;
    }

    @Override
    public void initialize()
    {
        if (!successfulInit && !(successfulInit = ensureDirectoryExists()))
            return;
        movableTypeClassLoader = new MovableTypeClassLoader(classLoader);
        loadMovableTypesFromDirectory();
    }

    @Override
    public void shutDown()
    {
        unloadMovableTypes();
    }
}
