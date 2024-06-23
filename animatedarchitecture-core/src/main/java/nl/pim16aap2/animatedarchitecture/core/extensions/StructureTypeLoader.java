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
import org.jetbrains.annotations.VisibleForTesting;
import org.semver4j.Semver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Responsible for loading extensions (structure types) from the extensions directory.
 */
@Singleton
@Flogger
public final class StructureTypeLoader extends Restartable
{
    /**
     * The current extension API version.
     * <p>
     * This version is used to check if an extension is compatible with the current API version.
     */
    public static final Semver CURRENT_EXTENSION_API_VERSION = Semver.of(1, 0, 0);

    /**
     * The title of the section in the manifest that contains the structure type metadata.
     */
    public static final String STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE = "StructureTypeMetadata";

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
     * @return The {@link StructureTypeInfo} of the file, if it could be constructed.
     */
    private Optional<StructureTypeInfo> getStructureTypeInfo(Path file)
    {
        log.atFine().log("Attempting to load StructureType from jar: %s", file);
        if (!file.toString().endsWith(".jar"))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("'%s' is not a valid jar file!", file);
            return Optional.empty();
        }

        @Nullable Manifest manifest = null;
        try (
            InputStream fileInputStream = Files.newInputStream(file);
            JarInputStream jarStream = new JarInputStream(fileInputStream))
        {
            manifest = jarStream.getManifest();
            return getStructureTypeInfo(
                STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE,
                file,
                manifest
            );
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to load structure type info from file: '%s'.\nManifest:\n%s",
                file,
                manifestToString(manifest)
            );
            return Optional.empty();
        }
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
     * Performs a preload check on a given {@link StructureTypeInfo}.
     * <p>
     * This method will check if the structure type is already loaded, if the API version of the structure type is
     * supported by the current API version and if the structure type is supported by the current API version.
     *
     * @param currentApiVersion
     *     The current API version to check against.
     * @param alreadyLoadedTypes
     *     The set of already loaded structure types.
     * @param typeInfo
     *     The {@link StructureTypeInfo} to check.
     * @return The result of the preload check.
     */
    static PreloadCheckResult performPreloadCheck(
        Semver currentApiVersion,
        Set<String> alreadyLoadedTypes,
        StructureTypeInfo typeInfo)
    {
        if (alreadyLoadedTypes.contains(typeInfo.getTypeName()))
            return PreloadCheckResult.ALREADY_LOADED;

        if (!isSupported(currentApiVersion, typeInfo.getSupportedApiVersions()))
        {
            log.atSevere().log(
                "The API version of the extension '%s' is not supported by the current API version: %s",
                typeInfo.getTypeName(),
                currentApiVersion
            );
            return PreloadCheckResult.API_VERSION_NOT_SUPPORTED;
        }

        return PreloadCheckResult.PASS;
    }

    /**
     * Logs the result of a preload check.
     *
     * @param preloadCheck
     *     The result of the preload check.
     * @param structureTypeInfo
     *     The {@link StructureTypeInfo} that was checked.
     */
    private void logPreloadCheckResult(PreloadCheckResult preloadCheck, StructureTypeInfo structureTypeInfo)
    {
        switch (preloadCheck)
        {
            case PASS:
                log.atInfo().log("Loading structure type: %s", structureTypeInfo.getTypeName());
                break;

            case ALREADY_LOADED:
                log.atInfo().log("Structure type '%s' is already loaded, skipping.", structureTypeInfo.getTypeName());
                break;

            case API_VERSION_NOT_SUPPORTED:
                log.atSevere().log(
                    "Current API version '%s' out of the supported range '%s' for structure type: '%s'",
                    CURRENT_EXTENSION_API_VERSION,
                    structureTypeInfo.getSupportedApiVersions(),
                    structureTypeInfo.getTypeName()
                );
                break;

            default:
                log.atSevere().log(
                    "Unknown preload check result '%s' for structure type '%s'.",
                    preloadCheck,
                    structureTypeInfo.getTypeName()
                );
        }
    }

    /**
     * Handles the results of a preload check.
     * <p>
     * This method will log the results of the preload check and take appropriate action.
     *
     * @param preloadCheckListMap
     *     The map of preload check results to structure type infos.
     */
    private void logPreloadCheckResults(Map<PreloadCheckResult, List<StructureTypeInfo>> preloadCheckListMap)
    {
        preloadCheckListMap.forEach((result, structureTypeInfos) ->
            structureTypeInfos.forEach(info -> logPreloadCheckResult(result, info)));
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
        final Set<String> alreadyLoadedTypes = getAlreadyLoadedTypes();

        final PathMatcher pathMatcher = directory.getFileSystem().getPathMatcher("glob:**.jar");

        final Map<PreloadCheckResult, List<StructureTypeInfo>> preloadCheckListMap;

        try (Stream<Path> files = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS))
        {
            preloadCheckListMap = files
                .filter(Files::isRegularFile)
                .filter(pathMatcher::matches)
                .map(this::getStructureTypeInfo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(info ->
                    performPreloadCheck(CURRENT_EXTENSION_API_VERSION, alreadyLoadedTypes, info))
                );
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log();
            return List.of();
        }

        logPreloadCheckResults(preloadCheckListMap);

        final List<StructureTypeInfo> acceptedTypes = preloadCheckListMap.get(PreloadCheckResult.PASS);
        if (acceptedTypes == null)
        {
            log.atWarning().log("No structure types to load!");
            return List.of();
        }

        final var loadedStructureTypes =
            new StructureTypeInitializer(acceptedTypes, structureTypeClassLoader, config.debug())
                .loadStructureTypes();

        structureTypeManager.register(loadedStructureTypes);
        return loadedStructureTypes;
    }

    /**
     * Writes the data used for loading a structure type from a manifest to a string.
     *
     * @param manifest
     *     The manifest to convert to a string.
     *     <p>
     *     If this is null, the string "null" will be returned.
     * @return The manifest as a string.
     */
    private String manifestToString(@Nullable Manifest manifest)
    {
        if (manifest == null)
            return "null";

        final StringBuilder sb = new StringBuilder();

        sb.append("  Main-Class: ")
            .append(manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS))
            .append('\n');

        final @Nullable var attributes = manifest.getEntries().get(STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE);
        if (attributes == null)
            return sb.append("  ").append(STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE).append(": null").toString();

        sb.append("  ").append(STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE).append(":\n");

        manifest.getAttributes(STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE)
            .forEach((key, value) -> sb.append("    ").append(key).append(": ").append(value).append('\n'));

        // Remove trailing newline
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * Checks if a given version range is supported by the current API version.
     *
     * @param apiVersion
     *     The current API version.
     * @param versionRange
     *     The version range to check.
     * @return True if the version range is supported by the current API version.
     */
    static boolean isSupported(Semver apiVersion, @Nullable String versionRange)
    {
        if (versionRange == null || versionRange.isBlank())
            return false;

        return apiVersion.satisfies(versionRange);
    }

    /**
     * Extracts the {@link StructureTypeInfo} from the manifest of a jar file.
     *
     * @param entryTitle
     *     The title of the section in the manifest that contains the structure type metadata.
     * @param file
     *     The path to the jar file.
     * @param manifest
     *     The manifest of the jar file.
     * @return The {@link StructureTypeInfo} if a valid one was found.
     *
     * @throws NullPointerException
     *     If the manifest does not contain the required section.
     * @throws IllegalArgumentException
     *     If the manifest does not contain the required section.
     */
    static Optional<StructureTypeInfo> getStructureTypeInfo(
        String entryTitle,
        Path file,
        Manifest manifest)
        throws NullPointerException
    {
        final @Nullable var attributes = manifest.getEntries().get(entryTitle);
        if (attributes == null)
            throw new IllegalArgumentException(
                "The manifest of file: '" + file + "' does not contain the section '" + entryTitle + "'!");

        final String className =
            Util.requireNonNull(manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS), "Main-Class");

        return Optional.of(new StructureTypeInfo(
            Util.requireNonNull(attributes.getValue("TypeName"), "TypeName"),
            Util.parseInt(attributes.getValue("Version"))
                .orElseThrow(() -> new NoSuchElementException("Version not found")),
            className,
            file,
            Util.requireNonNull(attributes.getValue("SupportedApiVersions"), "SupportedApiVersions"),
            attributes.getValue("TypeDependencies"))
        );
    }

    @Override
    public void initialize()
    {
        if (!successfulInit && !(successfulInit = ensureDirectoryExists()))
            return;

        loadStructureTypesFromDirectory();
    }

    /**
     * Describes the result of a preload check.
     */
    @VisibleForTesting
    enum PreloadCheckResult
    {
        /**
         * The preload check passed and the process can continue.
         */
        PASS,

        /**
         * The structure type is already loaded and should not be loaded again.
         */
        ALREADY_LOADED,

        /**
         * The structure type has an API version that is not supported by the current API version.
         */
        API_VERSION_NOT_SUPPORTED,
    }
}
