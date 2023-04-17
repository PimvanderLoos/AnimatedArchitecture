package nl.pim16aap2.animatedarchitecture.core.extensions;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
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
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Flogger
public final class StructureTypeLoader extends Restartable
{
    private final StructureTypeClassLoader structureTypeClassLoader =
        new StructureTypeClassLoader(getClass().getClassLoader());

    private final StructureTypeManager structureTypeManager;
    private final IConfig config;
    private final Path extensionsDirectory;
    private boolean successfulInit;

    @Inject
    public StructureTypeLoader(
        RestartableHolder holder,
        StructureTypeManager structureTypeManager,
        IConfig config,
        @Named("pluginBaseDirectory") Path dataDirectory)
    {
        super(holder);
        this.structureTypeManager = structureTypeManager;
        this.config = config;
        extensionsDirectory = dataDirectory.resolve(Constants.ANIMATE_ARCHITECTURE_EXTENSIONS_FOLDER_NAME);
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

    /**
     * Attempts to create the structure type info for a file at a given path.
     *
     * @param file
     *     The path to a file for which to create the structure type info.
     * @param alreadyLoadedTypes
     *     A set of names of main classes of structure types that have already been loaded. See {@link Class#getName()}
     *     and {@link StructureTypeInfo#getMainClass()}.
     *     <p>
     *     If the main class of the file being loaded already exists in this set, an empty optional will be returned, as
     *     this class cannot be loaded.
     * @return The {@link StructureTypeInfo} of the file, if it could be constructed.
     */
    private Optional<StructureTypeInfo> getStructureTypeInfo(Path file, Set<String> alreadyLoadedTypes)
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

            if (alreadyLoadedTypes.contains(className))
            {
                log.atInfo().log("File: '%s' with main class '%s' cannot be loaded: Main class is already loaded",
                                 file, className);
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
            final OptionalInt versionOpt =
                Util.parseInt(versionSection == null ? null : versionSection.getValue("Version"));

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
     * Retrieves the main class of all {@link StructureType} registered with the {@link StructureTypeManager}.
     * <p>
     * See {@link StructureTypeManager#getRegisteredStructureTypes()}.
     * <p>
     * See {@link Class#getName()}.
     *
     * @return The main classes of all registered structure types represented as a set of Strings.
     */
    private Set<String> getAlreadyLoadedTypes()
    {
        return structureTypeManager
            .getRegisteredStructureTypes().stream()
            .map(StructureType::getClass)
            .map(Class::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Attempts to load and register all jars in the default directory, which is the base plugin directory +
     * {@link Constants#ANIMATE_ARCHITECTURE_EXTENSIONS_FOLDER_NAME}.
     * <p>
     * See also {@link #loadStructureTypesFromDirectory(Path)}.
     */
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
        final List<StructureTypeInfo> typeInfoList = new ArrayList<>();

        final Set<String> alreadyLoadedTypes = getAlreadyLoadedTypes();
        try (Stream<Path> walk = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS))
        {
            final Stream<Path> result = walk.filter(Files::isRegularFile);
            result.forEach(path -> getStructureTypeInfo(path, alreadyLoadedTypes).ifPresent(typeInfoList::add));
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log();
        }

        final List<StructureType> types =
            new StructureTypeInitializer(typeInfoList, structureTypeClassLoader, config.debug()).loadStructureTypes();

        structureTypeManager.register(types);
        return types;
    }

    @Override
    public void initialize()
    {
        if (!successfulInit && !(successfulInit = ensureDirectoryExists()))
            return;

        loadStructureTypesFromDirectory();
    }
}
