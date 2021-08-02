package nl.pim16aap2.bigdoors.localization;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static nl.pim16aap2.bigdoors.localization.LocalizationUtil.*;

/**
 * Represents a class that can generate a localization file from multiple sources.
 * <p>
 * All sources are merged and written to a new set of files in the specified output directory with the specified base
 * name.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class LocalizationGenerator
{
    private final @NotNull Object lck = new Object();

    private final @NotNull Path outputDirectory;
    private final @NotNull String outputBaseName;

    /**
     * @param outputDirectory The output directory to write all the combined localizations into.
     * @param outputBaseName  The base name of the properties files in the output directory.
     */
    public LocalizationGenerator(@NotNull Path outputDirectory, @NotNull String outputBaseName)
    {
        this.outputDirectory = outputDirectory;
        this.outputBaseName = outputBaseName;
    }

    /**
     * Executes a {@link Runnable} and ensures that 1) the execution is synchronized and 2) the provided {@link
     * Localizer} is restarted appropriately to ensure files aren't locked and updates are propagated.
     *
     * @param localizer The localizer to restart.
     * @param runnable  The runnable to execute.
     * @return The current {@link LocalizationGenerator} instance.
     */
    @Contract("_, _ -> this")
    private LocalizationGenerator runWithLocalizer(@NotNull Localizer localizer, @NotNull Runnable runnable)
    {
        synchronized (lck)
        {
            localizer.shutdown();
            runnable.run();
            localizer.restart();
            return this;
        }
    }

    /**
     * See {@link #addResources(Path, String)}.
     *
     * @param localizer The {@link Localizer} to shut down before (re)generating the localization file(s). This ensures
     *                  the file isn't locked when running on *shudders* Windows.
     *                  <p>
     *                  After (re)generation, the localizer will be re-initialized.
     * @return The current {@link LocalizationGenerator} instance.
     */
    @Contract("_, _, _ -> this")
    public LocalizationGenerator addResources(@NotNull Localizer localizer, @NotNull Path directory,
                                              @NotNull String baseName)
    {
        return runWithLocalizer(localizer, () -> addResources(directory, baseName));
    }

    /**
     * Adds a new set of resources to the current localization set.
     * <p>
     * Only files of the format "directory/basename[_locale].properties" are included. "[_locale]" is optional here.
     *
     * @param directory The directory of the properties files.
     * @param baseName  The base name of the properties files.
     * @return The current {@link LocalizationGenerator} instance.
     */
    @Contract("_, _ -> this")
    public LocalizationGenerator addResources(@NotNull Path directory, @NotNull String baseName)
    {
        synchronized (lck)
        {
            try
            {
                val localeFiles = getLocaleFilesInDirectory(directory, baseName);
                for (val localeFile : localeFiles)
                    mergeWithExistingLocaleFile(localeFile);
            }
            catch (Exception e)
            {
                BigDoors.get().getPLogger().logThrowable(e, "Failed to add resources from directory \"" +
                    directory + "\" with base name: \"" + baseName + "\"");
            }
            return this;
        }
    }

    /**
     * Adds a new set of resources to the current localization set from a specific zip file (this includes jar files, of
     * course).
     * <p>
     * Only files in the top-level directory of the zip file are considered and of those, only if the have the format
     * "baseName[_locale].properties". When no specific baseName is provided, the "baseName" part used as example in the
     * format definition can only contain letters, numbers, and hyphens.
     * <p>
     * The existing locale file is derived from {@link LocaleFile#path()} of the input locale file, {@link
     * #outputDirectory}, and {@link #outputBaseName}.
     * <p>
     * If the output file does not exist yet, a new file will be created.
     *
     * @param jarFile  The zip file to load the locale files from.
     * @param baseName The base name of the translation files. When this is null, this property will be ignored and
     *                 locale files will be appended purely based on their locale.
     *                 <p>
     *                 When a baseName is provided, only those files whose names contain only those exact characters or
     *                 those characters followed by an underscore and the locale are considered. The ".properties" file
     *                 extension requirement stays either way.
     * @return The current {@link LocalizationGenerator} instance.
     */
    @Contract("_, _ -> this")
    public LocalizationGenerator addResourcesFromZip(@NotNull Path jarFile, @Nullable String baseName)
    {
        synchronized (lck)
        {
            try (val zipFileSystem = FileSystems.newFileSystem(jarFile))
            {
                List<String> fileNames = Util.getLocaleFilesInJar(jarFile);
                if (baseName != null)
                    fileNames = fileNames.stream().filter(file -> file.startsWith(baseName))
                                         .collect(Collectors.toList());

                val localeFiles = getLocaleFiles(zipFileSystem, fileNames);
                for (val localeFile : localeFiles)
                    mergeWithExistingLocaleFile(Files.newInputStream(localeFile.path()), localeFile.locale());
            }
            catch (IOException e)
            {
                BigDoors.get().getPLogger().logThrowable(e, "Failed to read resource from file: " + jarFile);
            }
        }
        return this;
    }

    /**
     * See {@link #addResourcesFromZip(Path, String)}.
     *
     * @param localizer The localizer to restart to ensure the changes are visible and that the target files aren't
     *                  locked.
     */
    @Contract("_, _, _ -> this")
    public LocalizationGenerator addResourcesFromZip(@NotNull Localizer localizer, @NotNull Path jarFile,
                                                     @Nullable String baseName)
    {
        return runWithLocalizer(localizer, () -> addResourcesFromZip(jarFile, baseName));
    }

    /**
     * Adds locale files from multiple zip files.
     * <p>
     * This method does not support using base names.
     * <p>
     * See {@link #addResourcesFromZip(Path, String)}
     */
    @Contract("_, -> this")
    public LocalizationGenerator addResourcesFromZips(@NotNull List<Path> zipFiles)
    {
        for (val zipFile : zipFiles)
            addResourcesFromZip(zipFile, null);
        return this;
    }

    /**
     * See {@link #addResourcesFromZips(List)}.
     *
     * @param localizer The localizer to restart to ensure the changes are visible and that the target files aren't
     *                  locked.
     */
    @Contract("_, _ -> this")
    public LocalizationGenerator addResourcesFromZips(@NotNull Localizer localizer, @NotNull List<Path> zipFiles)
    {
        return runWithLocalizer(localizer, () -> addResourcesFromZips(zipFiles));
    }

    /**
     * Loads the locale files from the jar file a specific {@link Class} was loaded from.
     * <p>
     * See {@link #addResourcesFromZip(Path, String)}.
     */
    @Contract("_, _ -> this")
    public LocalizationGenerator addResources(@NotNull Class<?> clz, @Nullable String baseName)
    {
        return addResourcesFromZip(Util.getJarFile(clz), baseName);
    }

    /**
     * See {@link #addResources(Class, String)}.
     *
     * @param localizer The localizer to restart to ensure the changes are visible and that the target files aren't
     *                  locked.
     */
    @Contract("_, _, _ -> this")
    public LocalizationGenerator addResources(@NotNull Localizer localizer, @NotNull Class<?> clz,
                                              @Nullable String baseName)
    {
        return runWithLocalizer(localizer, () -> addResources(clz, baseName));
    }

    /**
     * Loads the locale files from the jars file specific {@link Class}es was loaded from.
     * <p>
     * This method does not support using base names.
     * <p>
     * See {@link #addResources(Class, String)}.
     */
    @Contract("_ -> this")
    public LocalizationGenerator addResources(@NotNull List<Class<?>> classes)
    {
        for (val clz : classes)
            addResources(clz, null);
        return this;
    }

    /**
     * See {@link #addResources(List)}.
     *
     * @param localizer The localizer to restart to ensure the changes are visible and that the target files aren't
     *                  locked.
     */
    @Contract("_, _ -> this")
    public LocalizationGenerator addResources(@NotNull Localizer localizer, @NotNull List<Class<?>> classes)
    {
        return runWithLocalizer(localizer, () -> addResources(classes));
    }

    /**
     * Appends new keys from a locale file into the existing locale file.
     * <p>
     * The existing locale file is derived from {@link LocaleFile#path()} of the input locale file, {@link
     * #outputDirectory}, and {@link #outputBaseName}.
     * <p>
     * If the output file does not exist yet, a new file will be created.
     *
     * @param inputStream The input stream to read the new lines to append to the existing locale file from.
     * @param locale      The locale of the file to read.
     * @throws IOException When an I/O error occurred.
     */
    @GuardedBy("lck")
    void mergeWithExistingLocaleFile(@NotNull InputStream inputStream, @NotNull String locale)
        throws IOException
    {
        val existingLocaleFile = getOutputLocaleFile(locale);
        ensureFileExists(existingLocaleFile);
        val existing = readFile(Files.newInputStream(existingLocaleFile));
        val newlines = readFile(inputStream);
        val appendable = getAppendable(existing, newlines);
        appendToFile(existingLocaleFile, appendable);
    }

    /**
     * See {@link #mergeWithExistingLocaleFile(InputStream, String)}.
     */
    @GuardedBy("lck")
    void mergeWithExistingLocaleFile(@NotNull LocaleFile localeFile)
        throws IOException
    {
        mergeWithExistingLocaleFile(Files.newInputStream(localeFile.path()), localeFile.locale());
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
    @NotNull Path getOutputLocaleFile(@NotNull String locale)
    {
        val fileName = String.format("%s%s.properties", outputBaseName, locale.length() == 0 ? "" : ("_" + locale));
        return outputDirectory.resolve(fileName);
    }
}
