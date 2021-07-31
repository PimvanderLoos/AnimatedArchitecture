package nl.pim16aap2.bigdoors.localization;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static nl.pim16aap2.bigdoors.localization.LocalizationUtil.*;

/**
 * Represents a class that can generate a localization file from multiple sources.
 * <p>
 * All sources are merged and written to a new set of files in the specified output directory with the specified base
 * name.
 *
 * @author Pim
 */
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
     * See {@link #addResources(Path, String)}.
     *
     * @param localizer The {@link Localizer} to shut down before (re)generating the localization file(s). This ensures
     *                  the file isn't locked when running on *shudders* Windows.
     *                  <p>
     *                  After (re)generation, the localizer will be re-initialized.
     * @return The current {@link LocalizationGenerator} instance.
     */
    public @NotNull LocalizationGenerator addResources(@NotNull Localizer localizer, @NotNull Path directory,
                                                       @NotNull String baseName)
    {
        synchronized (lck)
        {
            localizer.shutdown();
            addResources(directory, baseName);
            localizer.restart();
            return this;
        }
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
    public @NotNull LocalizationGenerator addResources(@NotNull Path directory, @NotNull String baseName)
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
     * Appends new keys from a locale file into the existing locale file.
     * <p>
     * The existing locale file is derived from {@link LocaleFile#path()} of the input locale file, {@link
     * #outputDirectory}, and {@link #outputBaseName}.
     * <p>
     * If the output file does not exist yet, a new file will be created.
     *
     * @param localeFile The locale file whose files to copy into the existing file.
     * @throws IOException When an I/O error occurred.
     */
    @GuardedBy("lck")
    void mergeWithExistingLocaleFile(@NotNull LocaleFile localeFile)
        throws IOException
    {
        mergeWithExistingLocaleFile(Files.newInputStream(localeFile.path()), localeFile.locale());
    }

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
