package nl.pim16aap2.bigdoors.core.extensions;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IConfig;
import nl.pim16aap2.bigdoors.core.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.managers.StructureTypeManager;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.Constants;
import nl.pim16aap2.bigdoors.core.util.Util;
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
public final class StructureTypeLoader extends Restartable
{
    private final ClassLoader classLoader = getClass().getClassLoader();
    private @Nullable StructureTypeClassLoader structureTypeClassLoader;

    private final StructureTypeManager structureTypeManager;
    private final IConfig config;
    private final Path extensionsDirectory;
    private boolean successfulInit;

    @Inject
    public StructureTypeLoader(
        RestartableHolder holder, StructureTypeManager structureTypeManager, IConfig config,
        @Named("pluginBaseDirectory") Path dataDirectory)
    {
        super(holder);
        this.structureTypeManager = structureTypeManager;
        this.config = config;
        extensionsDirectory = dataDirectory.resolve(Constants.BIGDOORS_EXTENSIONS_FOLDER_NAME);
    }

    private void unloadStructureTypes()
    {
        if (structureTypeClassLoader == null)
            return;

        try
        {
            structureTypeClassLoader.close();
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e)
               .log("Failed to close structure type classloader! Extensions will NOT be loaded!");
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

    private Optional<StructureTypeInfo> getStructureTypeInfo(Path file)
    {
        log.atFine().log("Attempting to load StructureType from jar: %s", file);
        if (!file.toString().endsWith(".jar"))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("'%s' is not a valid jar file!", file);
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
                log.atSevere().withStackTrace(StackSize.FULL).log("File: '%s' does not specify its main class!", file);
                return Optional.empty();
            }

            final @Nullable Attributes typeNameSection = manifest.getEntries().get("TypeName");
            typeName = typeNameSection == null ? null : typeNameSection.getValue("TypeName");
            if (typeName == null)
            {
                log.atSevere().withStackTrace(StackSize.FULL).log("File: '%s' does not specify its type name!", file);
                return Optional.empty();
            }

            final @Nullable Attributes versionSection = manifest.getEntries().get("Version");
            final OptionalInt versionOpt = Util.parseInt(versionSection == null ?
                                                         null : versionSection.getValue("Version"));
            if (versionOpt.isEmpty())
            {
                log.atSevere().withStackTrace(StackSize.FULL).log("File: '%s' does not specify its version!", file);
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

        return Optional.of(new StructureTypeInfo(typeName, version, className, file, dependencies));
    }

    /**
     * Attempts to load and register all jars in the default directory, which is the base plugin directory +
     * {@link Constants#BIGDOORS_EXTENSIONS_FOLDER_NAME}.
     * <p>
     * See also {@link #loadStructureTypesFromDirectory(Path)}.
     */
    @SuppressWarnings("UnusedReturnValue")
    public List<StructureType> loadStructureTypesFromDirectory()
    {
        return loadStructureTypesFromDirectory(extensionsDirectory);
    }

    /**
     * Attempts to load and register all jars in a given directory.
     *
     * @param directory
     *     The directory.
     * @return The list of {@link StructureType}s that were loaded successfully.
     */
    public List<StructureType> loadStructureTypesFromDirectory(Path directory)
    {
        if (structureTypeClassLoader == null)
        {
            log.atSevere()
               .log("Trying to load structure types from directory %s, but the classloader does not exist!", directory);
            return Collections.emptyList();
        }

        final List<StructureTypeInfo> typeInfoList = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS))
        {
            final Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getStructureTypeInfo(path).ifPresent(typeInfoList::add));
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log();
        }

        final List<StructureType> types =
            new StructureTypeInitializer(typeInfoList, structureTypeClassLoader,
                                         config.debug()).loadStructureTypes();
        structureTypeManager.registerStructureTypes(types);
        return types;
    }

    @Override
    public void initialize()
    {
        if (!successfulInit && !(successfulInit = ensureDirectoryExists()))
            return;
        structureTypeClassLoader = new StructureTypeClassLoader(classLoader);
        loadStructureTypesFromDirectory();
    }

    @Override
    public void shutDown()
    {
        unloadStructureTypes();
    }
}
