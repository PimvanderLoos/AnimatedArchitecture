package nl.pim16aap2.bigdoors.localization;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nl.pim16aap2.bigdoors.localization.LocalizationUtil.*;

/**
 * Represents a class that can generate a localization file from multiple sources.
 * <p>
 * All sources are merged and written to a new set of files in the specified output directory with the specified base
 * name.
 *
 * @author Pim
 */
final class LocalizationGenerator implements ILocalizationGenerator
{
    private final Path outputDirectory;

    /**
     * The output .bundle (zip) that holds all the localization files.
     */
    private final Path outputFile;
    private final String outputBaseName;

    /**
     * @param outputDirectory The output directory to write all the combined localizations into.
     * @param outputBaseName  The base name of the properties files in the output directory.
     */
    LocalizationGenerator(@NotNull Path outputDirectory, @NotNull String outputBaseName)
    {
        this.outputDirectory = outputDirectory;
        this.outputBaseName = outputBaseName;
        outputFile = this.outputDirectory.resolve(this.outputBaseName + ".bundle");
    }

    @Override
    public void addResources(@NotNull Path path, @Nullable String baseName)
    {
        if (Files.isDirectory(path))
            addResourcesFromDirectory(path, baseName);
        else
            addResourcesFromZip(path, baseName);
    }

    @Override
    public void addResources(@NotNull List<Path> paths)
    {
        paths.forEach(path -> addResources(path, null));
    }

    void addResourcesFromDirectory(@NotNull Path directory, @Nullable String baseName)
    {
        try (final FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            final List<LocaleFile> localeFiles = getLocaleFilesInDirectory(directory, baseName);
            for (final LocaleFile localeFile : localeFiles)
                mergeWithExistingLocaleFile(outputFileSystem, localeFile);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to add resources from directory \"" +
                directory + "\" with base name: \"" + baseName + "\"");
        }
    }

    void addResourcesFromZip(@NotNull Path jarFile, @Nullable String baseName)
    {
        try (final FileSystem zipFileSystem = FileSystems.newFileSystem(jarFile);
             final FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            List<String> fileNames = Util.getLocaleFilesInJar(jarFile);
            if (baseName != null)
                fileNames = fileNames.stream().filter(file -> file.startsWith(baseName)).toList();

            final List<LocaleFile> localeFiles = getLocaleFiles(zipFileSystem, fileNames);
            for (final LocaleFile localeFile : localeFiles)
                try (final InputStream localeFileInputStream = Files.newInputStream(localeFile.path()))
                {
                    mergeWithExistingLocaleFile(outputFileSystem, localeFileInputStream, localeFile.locale());
                }
        }
        catch (IOException | URISyntaxException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to read resource from file: " + jarFile);
        }
    }

    /**
     * Finds the localization keys used in the root locale file in the output file.
     *
     * @return The set of localization keys used in the root locale file.
     */
    @NotNull Set<String> getOutputRootKeys()
    {
        try (final FileSystem outputFileSystem = getOutputFileFileSystem())
        {
            final Path existingLocaleFile = outputFileSystem.getPath(getOutputLocaleFileName(""));
            ensureFileExists(existingLocaleFile);
            return LocalizationUtil.getKeySet(Files.newInputStream(existingLocaleFile));
        }
        catch (IOException | URISyntaxException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to get keys from base locale file.");
            return Collections.emptySet();
        }
    }

    /**
     * Loads the locale files from the jar file a specific {@link Class} was loaded from.
     * <p>
     * See {@link #addResourcesFromZip(Path, String)}.
     */
    @Override
    public void addResourcesFromClass(@NotNull Class<?> clz, @Nullable String baseName)
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
    public void addResourcesFromClass(@NotNull List<Class<?>> classes)
    {
        for (final Class<?> clz : classes)
            addResourcesFromClass(clz, null);
    }

    /**
     * Applies a set of patches to the existing output locale file.
     *
     * @param localeSuffix The suffix of the locale.
     * @param patches      The patches to apply to the locale.
     */
    void applyPatches(@NotNull String localeSuffix, @NotNull Map<String, String> patches)
    {
        // FIXME: Implement method.
        throw new UnsupportedOperationException("UNIMPLEMENTED!");
    }

    /**
     * Creates a new {@link FileSystem} for {@link #outputFile}.
     * <p>
     * See {@link LocalizationUtil#createNewFileSystem(Path)}.
     */
    private @NotNull FileSystem getOutputFileFileSystem()
        throws IOException, URISyntaxException
    {
        ensureZipFileExists(outputFile);
        return createNewFileSystem(outputFile);
    }

    /**
     * Appends new keys from a locale file into the existing locale file.
     * <p>
     * The existing locale file is derived from {@link LocaleFile#path()} of the input locale file, {@link
     * #outputDirectory}, and {@link #outputBaseName}.
     * <p>
     * If the output file does not exist yet, a new file will be created.
     *
     * @param outputFileSystem The filesystem of the output file.
     * @param inputStream      The input stream to read the new lines to append to the existing locale file from.
     * @param locale           The locale of the file to read.
     * @throws IOException When an I/O error occurred.
     */
    void mergeWithExistingLocaleFile(@NotNull FileSystem outputFileSystem, @NotNull InputStream inputStream,
                                     @NotNull String locale)
        throws IOException
    {
        final Path existingLocaleFile = outputFileSystem.getPath(getOutputLocaleFileName(locale));
        ensureFileExists(existingLocaleFile);
        ensureFileExists(outputFile);
        final List<String> existing = readFile(Files.newInputStream(existingLocaleFile));
        final List<String> newlines = readFile(inputStream);
        final List<String> appendable = getAppendable(existing, newlines);
        appendToFile(existingLocaleFile, appendable);
    }

    /**
     * See {@link #mergeWithExistingLocaleFile(FileSystem, InputStream, String)}.
     */
    void mergeWithExistingLocaleFile(@NotNull FileSystem outputFileSystem, @NotNull LocaleFile localeFile)
        throws IOException
    {
        mergeWithExistingLocaleFile(outputFileSystem, Files.newInputStream(localeFile.path()), localeFile.locale());
    }

    /**
     * Retrieves the path of the output locale file.
     * <p>
     * The path is derived from {@link LocaleFile#path()} of the input locale file, {@link #outputDirectory}, and {@link
     * #outputBaseName}.
     *
     * @param locale The locale used to derive the path of the output file.
     * @return The path of the output file.
     */
    @SuppressWarnings("unused")
    @NotNull Path getOutputLocaleFile(@NotNull String locale)
    {
        return outputDirectory.resolve(getOutputLocaleFileName(locale));
    }

    /**
     * Retrieves the filename of the output locale file for a specific locale.
     *
     * @param locale The locale for which to find the output filename.
     * @return The filename of the output locale file for the provided locale.
     */
    @NotNull String getOutputLocaleFileName(@NotNull String locale)
    {
        return String.format("%s%s.properties", outputBaseName, locale.length() == 0 ? "" : ("_" + locale));
    }
}
