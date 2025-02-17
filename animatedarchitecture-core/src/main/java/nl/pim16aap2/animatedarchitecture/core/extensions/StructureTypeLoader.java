package nl.pim16aap2.animatedarchitecture.core.extensions;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidNameSpacedKeyException;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.FileUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.semver4j.Semver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
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
     * The file names of the legacy extension jars.
     */
    private static final Set<String> LEGACY_EXTENSION_JARS = Set.of(
        "BigDoor.jar",
        "Drawbridge.jar",
        "Windmill.jar",
        "Clock.jar",
        "Flag.jar",
        "GarageDoor.jar",
        "Portcullis.jar",
        "RevolvingDoor.jar",
        "SlidingDoor.jar"
    );

    /**
     * The file names of the invalid extension jars which were released in 0.7.0.
     * <p>
     * These filenames contain the ':' character which is not allowed in filenames on Windows.
     */
    private static final Set<String> INVALID_EXTENSION_JARS = Set.of(
        "animatedarchitecture:bigdoor.jar",
        "animatedarchitecture:clock.jar",
        "animatedarchitecture:drawbridge.jar",
        "animatedarchitecture:flag.jar",
        "animatedarchitecture:garagedoor.jar",
        "animatedarchitecture:portcullis.jar",
        "animatedarchitecture:revolvingdoor.jar",
        "animatedarchitecture:slidingdoor.jar",
        "animatedarchitecture:windmill.jar"
    );

    /**
     * The current extension API version.
     * <p>
     * This version is used to check if an extension is compatible with the current API version.
     */
    public static final Semver CURRENT_EXTENSION_API_VERSION = Semver.of(2, 0, 0);

    /**
     * The title of the section in the manifest that contains the structure type metadata.
     */
    public static final String STRUCTURE_TYPE_METADATA_MANIFEST_SECTION_TITLE = "StructureTypeMetadata";

    /**
     * The pattern used for matching embedded extension jars.
     * <p>
     * Each extension jar is expected to be in the form of: /extensions/*.jar.
     */
    private static final Pattern EMBEDDED_JAR_PATTERN = Pattern.compile("^extensions/[\\w-]+\\.jar$");

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
    private ParsedStructureTypeInfo getStructureTypeInfo(Path file)
    {
        log.atFine().log("Attempting to load StructureType from jar: %s", file);
        if (!file.toString().endsWith(".jar"))
        {
            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.INVALID_JAR,
                "File is not a jar file: " + file
            );
        }

        if (INVALID_EXTENSION_JARS.contains(file.getFileName().toString()))
        {
            log.atSevere().log(
                "Found extension with invalid name '%s'. Going to delete it now...",
                file.getFileName()
            );

            try
            {
                Files.delete(file);
            }
            catch (IOException e)
            {
                log.atSevere().withCause(e).log("Failed to delete extension with invalid name '%s'", file);
            }

            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.GENERIC_ERROR,
                "Extension has invalid name '" + file + "'"
            );
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

            return ParsedStructureTypeInfo.genericError(file);
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
            .getRegisteredStructureTypes()
            .stream()
            .map(StructureType::getFullKey)
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
        if (alreadyLoadedTypes.contains(typeInfo.getFullKey()))
            return PreloadCheckResult.ALREADY_LOADED;

        if (!isSupported(currentApiVersion, typeInfo.getSupportedApiVersions()))
        {
            log.atSevere().log(
                "The API version of the extension '%s' is not supported by the current API version: %s",
                typeInfo.getFullKey(),
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
                log.atInfo().log("Loading structure type: %s", structureTypeInfo.getFullKey());
                break;

            case ALREADY_LOADED:
                log.atInfo().log("Structure type '%s' is already loaded, skipping.", structureTypeInfo.getFullKey());
                break;

            case API_VERSION_NOT_SUPPORTED:
                log.atSevere().log(
                    "Current API version '%s' out of the supported range '%s' for structure type: '%s'",
                    CURRENT_EXTENSION_API_VERSION,
                    structureTypeInfo.getSupportedApiVersions(),
                    structureTypeInfo.getFullKey()
                );
                break;

            default:
                log.atSevere().log(
                    "Unknown preload check result '%s' for structure type '%s'.",
                    preloadCheck,
                    structureTypeInfo.getFullKey()
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
     * Extracts the structure types from the embedded jars in a given jar file.
     *
     * @param jarFile
     *     The jar file to extract the embedded structure types from.
     */
    public void extractEmbeddedStructureTypes(Path jarFile)
    {
        try
        {
            final List<String> extensionJars = FileUtil.getFilesInJar(jarFile, EMBEDDED_JAR_PATTERN);
            extractEmbeddedStructureTypes(jarFile, extensionJars);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to read resource from file: %s", jarFile);
        }
    }

    /**
     * Extracts the structure types from the embedded jars in a given jar file.
     *
     * @param jarFile
     *     The jar file to extract the embedded structure types from.
     * @param extensions
     *     The list of extensions to extract. Each entry is the path to the extension in the jar file.
     */
    void extractEmbeddedStructureTypes(Path jarFile, List<String> extensions)
    {
        try (FileSystem zipFileSystem = FileUtil.createNewFileSystem(jarFile))
        {
            extensions
                .stream()
                .map(zipFileSystem::getPath)
                .map(this::getStructureTypeInfo)
                .forEach(info -> extractEmbeddedStructureType(info, zipFileSystem, jarFile));
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to read resource from file: %s", jarFile);
        }
    }

    /**
     * Extracts an embedded structure type.
     *
     * @param parsedInfo
     *     The parsed structure type info.
     * @param zipFileSystem
     *     The file system of the jar file.
     * @param jarFile
     *     The jar file that contains the embedded structure type.
     */
    private void extractEmbeddedStructureType(
        ParsedStructureTypeInfo parsedInfo,
        FileSystem zipFileSystem,
        Path jarFile)
    {
        if (!parsedInfo.isSuccessful())
        {
            log.atSevere().log(
                "Failed to extract embedded structure type from jar: '%s'. Reason: %s",
                jarFile,
                parsedInfo.context()
            );
            return;
        }

        extractEmbeddedStructureType(Objects.requireNonNull(parsedInfo.structureTypeInfo()), zipFileSystem, jarFile);
    }

    /**
     * Extracts an embedded structure type.
     *
     * @param structureTypeInfo
     *     The structure type info to extract.
     * @param zipFileSystem
     *     The file system of the jar file.
     * @param jarFile
     *     The jar file that contains the embedded structure type.
     */
    private void extractEmbeddedStructureType(
        StructureTypeInfo structureTypeInfo,
        FileSystem zipFileSystem,
        Path jarFile
    )
    {
        final Path structureTypePath =
            zipFileSystem.getPath(structureTypeInfo.getJarFile().toAbsolutePath().toString());

        final Path targetPath =
            extensionsDirectory.resolve(structureTypeInfo.getFullKey().replaceAll(":", "_") + ".jar");

        try (InputStream is = Files.newInputStream(structureTypePath))
        {
            Files.copy(is, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log(
                "Failed to extract embedded structure type '%s' from jar: '%s' to: '%s'",
                structureTypeInfo.getFullKey(),
                jarFile,
                targetPath
            );
        }
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
                // Sort inverted to get e.g. my-structure-v2 before my-structure-v1
                .sorted(Comparator.reverseOrder())
                .map(this::getStructureTypeInfo)
                .map(this::handleParsedStructureTypeInfo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(info ->
                    performPreloadCheck(CURRENT_EXTENSION_API_VERSION, alreadyLoadedTypes, info))
                );
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to load structure types from directory: %s", directory);
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
     * Handles the result of parsing a {@link StructureTypeInfo} from a manifest.
     * <p>
     * If the parse was successful, the structure type is returned.
     * <p>
     * If the parse was not successful, we log the error, perform any other necessary actions, and return an empty
     * optional.
     *
     * @param parsedStructureTypeInfo
     *     The parsed structure type info.
     * @return The structure type if the parse was successful.
     */
    private Optional<StructureTypeInfo> handleParsedStructureTypeInfo(ParsedStructureTypeInfo parsedStructureTypeInfo)
    {
        if (parsedStructureTypeInfo.isSuccessful())
            return Optional.of(Objects.requireNonNull(parsedStructureTypeInfo.structureTypeInfo()));

        log.atSevere().log(
            "Failed to load structure type from file: '%s'. Reason: %s",
            parsedStructureTypeInfo.file(),
            parsedStructureTypeInfo.context()
        );

        // These states are too invalid to assume it's even a structure jar at all.
        // So we don't handle them any further.
        final var result = parsedStructureTypeInfo.parseResult();
        if (result == StructureTypeInfoParseResult.INVALID_JAR ||
            result == StructureTypeInfoParseResult.NO_ATTRIBUTES)
            return Optional.empty();

        handleInvalidStructureFile(parsedStructureTypeInfo.file());

        return Optional.empty();
    }

    /**
     * Handles an invalid structure file.
     * <p>
     * If the file is a legacy extension jar, it will be deleted.
     *
     * @param file
     *     The file to handle.
     */
    private void handleInvalidStructureFile(Path file)
    {
        if (!LEGACY_EXTENSION_JARS.contains(file.getFileName().toString()))
            return;

        log.atWarning().log(
            "Found legacy extension jar '%s'. " +
                "Going to delete it now... The updated version has been installed automatically.",
            file.getFileName()
        );

        try
        {
            Files.delete(file);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to delete legacy extension jar: %s", file);
        }
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
    static ParsedStructureTypeInfo getStructureTypeInfo(
        String entryTitle,
        Path file,
        Manifest manifest)
        throws NullPointerException
    {
        final @Nullable var attributes = manifest.getEntries().get(entryTitle);

        if (attributes == null)
            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.NO_ATTRIBUTES,
                String.format(
                    "The manifest of file '%s' does not contain the section '%s!",
                    file,
                    entryTitle
                ));

        final @Nullable String className = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        if (className == null)
            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.MISSING_ATTRIBUTE,
                String.format(
                    "The manifest of file '%s' does not contain the main class!",
                    file
                ));

        final OptionalInt version = MathUtil.parseInt(attributes.getValue("Version"));
        if (version.isEmpty())
            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.MISSING_ATTRIBUTE,
                String.format(
                    "The manifest of file '%s' does not contain the version!",
                    file
                ));

        final @Nullable NamespacedKey namespacedKey;
        try
        {
            namespacedKey = new NamespacedKey(
                Objects.requireNonNullElse(attributes.getValue("Namespace"), ""),
                Objects.requireNonNullElse(attributes.getValue("TypeName"), "")
            );
        }
        catch (InvalidNameSpacedKeyException e)
        {
            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.INVALID_NAMESPACED_KEY,
                "The manifest of file '" + file + "' contains an invalid namespaced key: " + e.getMessage()
            );
        }

        final @Nullable String supportedApiVersions = attributes.getValue("SupportedApiVersions");
        if (supportedApiVersions == null)
            return ParsedStructureTypeInfo.failed(
                file,
                StructureTypeInfoParseResult.MISSING_ATTRIBUTE,
                String.format(
                    "The manifest of file '%s' does not contain the supported API versions!",
                    file
                ));

        return ParsedStructureTypeInfo.success(
            new StructureTypeInfo(
                namespacedKey,
                version.getAsInt(),
                className,
                file,
                supportedApiVersions,
                attributes.getValue("TypeDependencies")
            ));
    }

    @Override
    public void initialize()
    {
        if (!successfulInit && !(successfulInit = ensureDirectoryExists()))
            return;

        extractEmbeddedStructureTypes(FileUtil.getJarFile(this.getClass()).toAbsolutePath());
        loadStructureTypesFromDirectory();
    }

    /**
     * Represents the result of parsing a {@link StructureTypeInfo} from a manifest.
     *
     * @param file
     *     The path to the file that was parsed.
     * @param structureTypeInfo
     *     The parsed structure type info. This is always null if the parse result is not
     *     {@link StructureTypeInfoParseResult#SUCCESS}.
     *     <p>
     *     When it is successful, it is never null.
     * @param parseResult
     *     The result of the parse.
     * @param context
     *     Additional context for the parse result.
     */
    record ParsedStructureTypeInfo(
        Path file,
        @Nullable StructureTypeInfo structureTypeInfo,
        StructureTypeInfoParseResult parseResult,
        @Nullable String context
    )
    {
        /**
         * Creates a new {@link ParsedStructureTypeInfo} with a failed parse result.
         *
         * @param file
         *     The path to the file that was parsed.
         * @param parseResult
         *     The reason for the failed parse.
         * @param context
         *     Additional context for the parse result.
         * @return The failed {@link ParsedStructureTypeInfo}.
         */
        static ParsedStructureTypeInfo failed(
            Path file,
            StructureTypeInfoParseResult parseResult,
            @Nullable String context)
        {
            return new ParsedStructureTypeInfo(file, null, parseResult, context);
        }

        /**
         * Creates a new {@link ParsedStructureTypeInfo} with a generic error.
         *
         * @param file
         *     The path to the file that was parsed.
         * @return The generic error {@link ParsedStructureTypeInfo}.
         */
        static ParsedStructureTypeInfo genericError(Path file)
        {
            return new ParsedStructureTypeInfo(
                file,
                null,
                StructureTypeInfoParseResult.GENERIC_ERROR,
                "A generic error occurred."
            );
        }

        /**
         * Creates a new {@link ParsedStructureTypeInfo} with a successful parse result.
         *
         * @param structureTypeInfo
         *     The parsed structure type info.
         * @return The successful {@link ParsedStructureTypeInfo}.
         */
        static ParsedStructureTypeInfo success(StructureTypeInfo structureTypeInfo)
        {
            return new ParsedStructureTypeInfo(
                structureTypeInfo.getJarFile(),
                structureTypeInfo,
                StructureTypeInfoParseResult.SUCCESS,
                null
            );
        }

        /**
         * Checks if the parse was successful.
         *
         * @return True if the parse was successful.
         */
        boolean isSuccessful()
        {
            return parseResult == StructureTypeInfoParseResult.SUCCESS;
        }
    }

    /**
     * Represents the result of parsing a {@link StructureTypeInfo} from a manifest.
     */
    enum StructureTypeInfoParseResult
    {
        /**
         * The structure type info was successfully parsed.
         */
        SUCCESS,

        /**
         * An error occurred while parsing the structure type info.
         */
        GENERIC_ERROR,

        /**
         * The manifest of the jar file does not contain any attributes.
         */
        NO_ATTRIBUTES,

        /**
         * The manifest of the jar file does not contain the required attribute.
         */
        MISSING_ATTRIBUTE,

        /**
         * The manifest of the jar file contains an invalid namespaced key or the contains no key at all.
         */
        INVALID_NAMESPACED_KEY,

        /**
         * The file is not a valid jar file.
         */
        INVALID_JAR,
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
