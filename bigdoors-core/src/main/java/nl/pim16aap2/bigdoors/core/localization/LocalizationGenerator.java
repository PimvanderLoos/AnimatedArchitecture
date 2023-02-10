package nl.pim16aap2.bigdoors.core.localization;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a class that can generate a localization file from multiple sources.
 * <p>
 * All sources are merged and written to a new set of files in the specified output directory with the specified base
 * name.
 *
 * @author Pim
 */
@Flogger //
final class LocalizationGenerator implements ILocalizationGenerator
{
    /**
     * The output .bundle (zip) that holds all the localization files.
     */
    @Getter
    private final Path outputFile;
    private final String outputBaseName;

    /**
     * @param outputDirectory
     *     The output directory to write all the combined localizations into.
     * @param outputBaseName
     *     The base name of the properties files in the output directory.
     */
    LocalizationGenerator(Path outputDirectory, String outputBaseName)
    {
        this.outputBaseName = outputBaseName;
        outputFile = outputDirectory.resolve(this.outputBaseName + ".bundle");
        LocalizationUtil.ensureZipFileExists(outputFile);
    }

    @Override
    public void addResources(Path path, @Nullable String baseName)
    {
        if (Files.isDirectory(path))
            addResourcesFromDirectory(path, baseName);
        else
            addResourcesFromZip(path, baseName);
    }

    @Override
    public void addResources(List<Path> paths)
    {
        paths.forEach(path -> addResources(path, null));
    }

    void addResourcesFromDirectory(Path directory, @Nullable String baseName)
    {
        try (FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            final List<LocaleFile> localeFiles = LocalizationUtil.getLocaleFilesInDirectory(directory, baseName);
            for (final LocaleFile localeFile : localeFiles)
                mergeWithExistingLocaleFile(outputFileSystem, localeFile);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to add resources from directory '%s' with base name: '%s",
                                            directory, baseName);
        }
    }

    void addResourcesFromZip(Path jarFile, @Nullable String baseName)
    {
        try (FileSystem zipFileSystem = LocalizationUtil.createNewFileSystem(jarFile);
             FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            List<String> fileNames = Util.getLocaleFilesInJar(jarFile);
            if (baseName != null)
                fileNames = fileNames.stream().filter(file -> file.startsWith(baseName)).toList();

            final List<LocaleFile> localeFiles = LocalizationUtil.getLocaleFiles(zipFileSystem, fileNames);
            for (final LocaleFile localeFile : localeFiles)
                try (InputStream localeFileInputStream = Files.newInputStream(localeFile.path()))
                {
                    mergeWithExistingLocaleFile(outputFileSystem, localeFileInputStream, localeFile.locale());
                }
        }
        catch (IOException | URISyntaxException | ProviderNotFoundException e)
        {
            log.atSevere().withCause(e).log("Failed to read resource from file: %s", jarFile);
        }
    }

    /**
     * Finds the localization keys used in the root locale file in the output file.
     *
     * @return The set of localization keys used in the root locale file.
     */
    Set<String> getOutputRootKeys()
    {
        try (FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            final Path existingLocaleFile =
                outputFileSystem.getPath(LocalizationUtil.getOutputLocaleFileName(outputBaseName, ""));
            LocalizationUtil.ensureFileExists(existingLocaleFile);
            return LocalizationUtil.getKeySet(Files.newInputStream(existingLocaleFile));
        }
        catch (IOException | URISyntaxException | ProviderNotFoundException e)
        {
            log.atSevere().withCause(e).log("Failed to get keys from base locale file.");
            return Collections.emptySet();
        }
    }

    /**
     * Loads the locale files from the jar file a specific {@link Class} was loaded from.
     * <p>
     * See {@link #addResourcesFromZip(Path, String)}.
     */
    @Override
    public void addResourcesFromClass(Class<?> clz, @Nullable String baseName)
    {
        addResourcesFromZip(Util.getJarFile(clz), baseName);
    }

    /**
     * Loads the locale files from the jars file specific {@link Class}es was loaded from.
     * <p>
     * This method does not support using base names.
     * <p>
     * See {@link #addResourcesFromClass(Class, String)}.
     */
    @Override
    public void addResourcesFromClass(List<Class<?>> classes)
    {
        for (final Class<?> clz : classes)
            addResourcesFromClass(clz, null);
    }

    /**
     * Applies a set of patches to the existing output locale file.
     *
     * @param localeSuffix
     *     The suffix of the locale.
     * @param patches
     *     The patches to apply to the locale.
     */
    void applyPatches(String localeSuffix, Map<String, String> patches)
    {
        try (FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            final Path existingLocaleFile =
                outputFileSystem.getPath(LocalizationUtil.getOutputLocaleFileName(outputBaseName, localeSuffix));
            LocalizationUtil.ensureFileExists(existingLocaleFile);

            final List<String> lines = LocalizationUtil.readFile(Files.newInputStream(existingLocaleFile));
            mergeWithPatches(lines, patches);

            final StringBuilder sb = new StringBuilder();
            lines.forEach(line -> sb.append(line).append('\n'));
            Files.writeString(existingLocaleFile, sb.toString());
        }
        catch (IOException | URISyntaxException | ProviderNotFoundException e)
        {
            log.atSevere().withCause(e).log("Failed to open output file!");
        }
    }

    /**
     * Merges a list of lines with a set of patches.
     * <p>
     * Any existing lines for which a patch exist will be replaced.
     * <p>
     * All patches for which no existing line exists will be appended at the end.
     *
     * @param lines
     *     The list of lines to merge with the patches. This list is modified in-place.
     * @param patches
     *     The patches to merge into the existing lines.
     */
    static void mergeWithPatches(List<String> lines, Map<String, String> patches)
    {
        final Set<String> usedPatches = new HashSet<>((int) Math.ceil(1.25 * patches.size()));
        for (int idx = 0; idx < lines.size(); ++idx)
        {
            final @Nullable LocalizationEntry entry = LocalizationUtil.getEntryFromLine(lines.get(idx));
            if (entry == null)
                continue;
            final @Nullable String newLine = patches.get(entry.key());
            if (newLine == null)
                continue;
            lines.set(idx, newLine);
            usedPatches.add(entry.key());
        }

        if (usedPatches.size() == patches.size())
            return;

        for (final Map.Entry<String, String> patch : patches.entrySet())
        {
            if (usedPatches.contains(patch.getKey()))
                continue;
            lines.add(patch.getValue());
        }
    }

    /**
     * Creates a new {@link FileSystem} for {@link #outputFile}.
     * <p>
     * See {@link LocalizationUtil#createNewFileSystem(Path)}.
     */
    private FileSystem getOutputFileFileSystem()
        throws IOException, URISyntaxException, ProviderNotFoundException
    {
        LocalizationUtil.ensureZipFileExists(outputFile);
        return LocalizationUtil.createNewFileSystem(outputFile);
    }

    /**
     * Appends new keys from a locale file into the existing locale file.
     * <p>
     * The existing locale file is derived from {@link LocaleFile#path()} of the input locale file, the provided output
     * directory, and {@link #outputBaseName}.
     * <p>
     * If the output file does not exist yet, a new file will be created.
     *
     * @param outputFileSystem
     *     The filesystem of the output file.
     * @param inputStream
     *     The input stream to read the new lines to append to the existing locale file from.
     * @param locale
     *     The locale of the file to read.
     * @throws IOException
     *     When an I/O error occurred.
     */
    void mergeWithExistingLocaleFile(FileSystem outputFileSystem, InputStream inputStream, String locale)
        throws IOException
    {
        final Path existingLocaleFile =
            outputFileSystem.getPath(LocalizationUtil.getOutputLocaleFileName(outputBaseName, locale));
        LocalizationUtil.ensureFileExists(existingLocaleFile);
        LocalizationUtil.ensureFileExists(outputFile);
        final List<String> existing = LocalizationUtil.readFile(Files.newInputStream(existingLocaleFile));
        final List<String> newlines = LocalizationUtil.readFile(inputStream);
        final List<String> appendable = LocalizationUtil.getAppendable(existing, newlines);
        LocalizationUtil.appendToFile(existingLocaleFile, appendable);
    }

    /**
     * See {@link #mergeWithExistingLocaleFile(FileSystem, InputStream, String)}.
     */
    void mergeWithExistingLocaleFile(FileSystem outputFileSystem, LocaleFile localeFile)
        throws IOException
    {
        mergeWithExistingLocaleFile(outputFileSystem, Files.newInputStream(localeFile.path()), localeFile.locale());
    }
}
