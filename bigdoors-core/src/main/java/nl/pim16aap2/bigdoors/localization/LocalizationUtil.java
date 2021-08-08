package nl.pim16aap2.bigdoors.localization;

import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a utility class with methods that can be used for the localization system.
 *
 * @author Pim
 */
public class LocalizationUtil
{
    private LocalizationUtil()
    {
        // utility class
    }

    /**
     * Ensures that a file exists.
     * <p>
     * If the file does not already exists, it will be created.
     *
     * @param file The file whose existence to ensure.
     * @throws IOException When the file could not be created.
     */
    static void ensureFileExists(@NotNull Path file)
        throws IOException
    {
        if (Files.isRegularFile(file))
            return;
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }

    /**
     * Appends a list of strings to a file.
     * <p>
     * Every entry in the list will be printed on its own line.
     *
     * @param path   The path of the file.
     * @param append The list of Strings (lines) to append to the file.
     */
    static void appendToFile(@NotNull Path path, @NotNull List<String> append)
    {
        if (append.isEmpty())
            return;

        val sb = new StringBuilder();
        append.forEach(line -> sb.append(line).append("\n"));
        try
        {
            Files.write(path, sb.toString().getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to write localization file: " + path, e);
        }
    }

    /**
     * Retrieves the list of Strings from a list of lines that can be appended to the list of existing Strings.
     * <p>
     * A line can be appended when its key does not appear in the list of existing Strings.
     *
     * @param existing The base list of lines.
     * @param newLines The set of new lines to try to append to
     * @return A list with all lines from the existing set with any lines from the newLines set added to it where the
     * key did not exist in the existing list yet.
     */
    static @NotNull List<String> getAppendable(@NotNull List<String> existing, @NotNull List<String> newLines)
    {
        if (existing.isEmpty())
            return newLines;

        val keys = getKeySet(existing);
        val merged = new ArrayList<String>(newLines.size());

        for (val line : newLines)
        {
            val key = getKeyFromLine(line);
            if (key == null || keys.contains(key))
                continue;
            merged.add(line);
        }

        merged.trimToSize();
        return merged;
    }

    /**
     * Gets a set containing all the keys in a list of string.
     *
     * @param lines The lines containing "key=value" mappings.
     * @return A set with all the keys used in the lines.
     */
    static @NotNull Set<String> getKeySet(@NotNull List<String> lines)
    {
        final Set<String> ret = new HashSet<>(lines.size());
        for (val line : lines)
        {
            val key = getKeyFromLine(line);
            if (key != null)
                ret.add(key);
        }
        return ret;
    }

    /**
     * Retrieves the key from a String with the format "key=value".
     *
     * @param line A string containing a key/value pair.
     * @return The key as used in the line.
     */
    static @Nullable String getKeyFromLine(@NotNull String line)
    {
        val parts = line.split("=", 2);
        return parts.length == 2 ? parts[0] : null;
    }

    /**
     * Reads all the lines from an {@link InputStream};
     *
     * @return A list of Strings where every string represents a single line in the provided input stream.
     */
    static @NotNull List<String> readFile(@NotNull InputStream inputStream)
    {
        final ArrayList<String> tmp = new ArrayList<>();

        try (val bufferedReader = new BufferedReader(new InputStreamReader(inputStream)))
        {
            for (String line; (line = bufferedReader.readLine()) != null; )
            {
                if (line.length() == 0)
                    continue;
                if (line.charAt(0) == '#')
                    continue;
                tmp.add(line);
            }
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to read localization file!");
            return Collections.emptyList();
        }
        return tmp;
    }

    /**
     * Retrieves all the {@link LocaleFile}s in a directory.
     *
     * @param directory The directory to search in.
     * @param baseName  The base name of the locale files.
     * @return A list of {@link LocaleFile}s found in the directory.
     */
    static @NotNull List<LocaleFile> getLocaleFilesInDirectory(@NotNull Path directory, @NotNull String baseName)
    {
        try (val stream = Files.list(directory))
        {
            return getLocaleFiles(baseName,
                                  stream.filter(file -> !Files.isDirectory(file)).collect(Collectors.toList()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a list of {@link LocaleFile}s from a base name and a list of files.
     *
     * @param baseName The base name of the translation files.
     * @param files    The list of files to look at.
     * @return A list of {@link LocaleFile}s that includes all files that meet the correct naming format of
     * "directory/basename[_locale].properties".
     */
    static @NotNull List<LocaleFile> getLocaleFiles(@NotNull String baseName, @NotNull List<Path> files)
    {
        final @NotNull ArrayList<LocaleFile> ret = new ArrayList<>(files.size());
        for (val file : files)
        {
            @Nullable val locale = parseLocaleFile(baseName, file.getFileName().toString());
            if (locale != null)
                ret.add(new LocaleFile(file, locale));
        }
        ret.trimToSize();
        return ret;
    }

    /**
     * See {@link #getLocaleFiles(FileSystem, List)}.
     * <p>
     * All paths are created on the default filesystem: {@link FileSystems#getDefault()}.
     */
    static @NotNull List<LocaleFile> getLocaleFiles(@NotNull List<String> resources)
    {
        return getLocaleFiles(FileSystems.getDefault(), resources);
    }

    /**
     * Gets a list of {@link LocaleFile}s from a list of filenames.
     * <p>
     * The file names should not include any directories.
     *
     * @param fileSystem The {@link FileSystem} to create the paths on.
     * @param resources  The list of file names.
     * @return A list of {@link LocaleFile}s that includes all files that meet the correct naming format:
     * "whatever-filename[_locale].properties".
     */
    static @NotNull List<LocaleFile> getLocaleFiles(@NotNull FileSystem fileSystem, @NotNull List<String> resources)
    {
        final ArrayList<LocaleFile> ret = new ArrayList<>(resources.size());
        for (val resource : resources)
        {
            @Nullable val locale = parseLocaleFile(resource);
            if (locale != null)
                ret.add(new LocaleFile(fileSystem.getPath(resource), locale));
        }
        ret.trimToSize();
        return ret;
    }

    /**
     * Retrieves the locale from a locale file with the format "basename[_locale].properties".
     *
     * @param baseName The base name of the translation files.
     * @param fileName The name of the location file. This should only include the name of the file itself and not its
     *                 parent directories.
     * @return The locale as used in the filename.
     */
    static @Nullable String parseLocaleFile(@NotNull String baseName, @NotNull String fileName)
    {
        if (!fileName.startsWith(baseName) || !fileName.endsWith(".properties"))
            return null;

        String locale = fileName.replace(baseName, "").replace(".properties", "");
        if (locale.length() > 0)
        {
            if (locale.charAt(0) != '_')
                return null;
            locale = locale.substring(1);
        }
        return locale;
    }

    /**
     * Parses the filename of a locale file to extract its locale.
     *
     * @param fileName The name of a locale file.
     * @return The locale as extracted from the locale file or null if the file is not a properties file.
     * <p>
     * For example, in the case of a file "Translated_en_US.properties", this method would return "en_US".
     * <p>
     * If the file does not have a locale (e.g. "Translated.properties"), an empty String is returned.
     */
    static @Nullable String parseLocaleFile(@NotNull String fileName)
    {
        if (!fileName.endsWith(".properties"))
            return null;
        val parts = fileName.replace(".properties", "").split("_", 2);
        return parts.length == 1 ? "" : parts[1];
    }

    /**
     * Gets the currently-available list of {@link Locale}s as found in the directory.
     */
    static @NotNull List<Locale> getLocalesInDirectory(@NotNull Path directory, @NotNull String baseName)
    {
        return LocalizationUtil.getLocaleFilesInDirectory(directory, baseName).stream()
                               .map(localeFile -> getLocale(localeFile.locale())).toList();
    }

    /**
     * Gets a {@link Locale} from a String representing a locale. E.g. "en_US".
     * <p>
     * If the locale string is empty, the root locale is returned. See {@link Locale#ROOT}.
     *
     * @param localeStr A String representing a locale.
     * @return The decoded Locale.
     */
    public static @NotNull Locale getLocale(@NotNull String localeStr)
    {
        val parts = localeStr.split("_", 3);
        if (parts[0].isBlank())
            return Locale.ROOT;

        if (parts.length == 1)
            return new Locale(parts[0]);
        if (parts.length == 2)
            return new Locale(parts[0], parts[1]);
        return new Locale(parts[0], parts[1], parts[2]);
    }

    /**
     * Represents a translation file for a specific {@link Locale}.
     *
     * @param path   The path of the file.
     * @param locale The {@link Locale} this file represents.
     */
    record LocaleFile(@NotNull Path path, @NotNull String locale) {}
}
