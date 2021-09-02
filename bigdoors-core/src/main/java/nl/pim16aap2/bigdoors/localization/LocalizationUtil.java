package nl.pim16aap2.bigdoors.localization;

import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

/**
 * Represents a utility class with methods that can be used for the localization system.
 *
 * @author Pim
 */
public final class LocalizationUtil
{
    private LocalizationUtil()
    {
        // utility class
    }

    /**
     * Ensures that a file exists.
     * <p>
     * If the file does not already exist, it will be created.
     *
     * @param file
     *     The file whose existence to ensure.
     * @return The path to the file.
     *
     * @throws IOException
     *     When the file could not be created.
     */
    static Path ensureFileExists(Path file)
        throws IOException
    {
        if (Files.isRegularFile(file))
            return file;

        final Path parent = file.getParent();
        if (parent != null)
            Files.createDirectories(parent);
        return Files.createFile(file);
    }

    /**
     * Appends a list of strings to a file.
     * <p>
     * Every entry in the list will be printed on its own line.
     *
     * @param path
     *     The path of the file.
     * @param append
     *     The list of Strings (lines) to append to the file.
     */
    static void appendToFile(Path path, List<String> append)
    {
        if (append.isEmpty())
            return;

        final StringBuilder sb = new StringBuilder();
        append.forEach(line -> sb.append(line).append('\n'));
        try
        {
            Files.writeString(path, sb.toString(), StandardOpenOption.APPEND);
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
     * @param existing
     *     The base list of lines.
     * @param newLines
     *     The set of new lines to try to append to
     * @return A list with all lines from the existing set with any lines from the newLines set added to it where the
     * key did not exist in the existing list yet.
     */
    static List<String> getAppendable(List<String> existing, List<String> newLines)
    {
        if (existing.isEmpty())
            return newLines;

        final Set<@Nullable String> keys = getKeySet(existing);
        final ArrayList<String> merged = new ArrayList<>(newLines.size());

        for (final String line : newLines)
        {
            final @Nullable String key = getKeyFromLine(line);
            if (key == null || keys.contains(key))
                continue;
            merged.add(line);
        }

        merged.trimToSize();
        return merged;
    }

    /**
     * Gets a set containing all the keys in an input stream.
     *
     * @param inputStream
     *     The input stream to read lines of Strings from. These lines are expected to be of the format "key=value".
     * @return A set with all the keys used in the input stream.
     */
    static Set<String> getKeySet(InputStream inputStream)
    {
        final Set<String> ret = new LinkedHashSet<>();
        readFile(inputStream, line ->
        {
            final @Nullable String key = getKeyFromLine(line);
            if (key != null)
                ret.add(key);
        });
        return ret;
    }

    /**
     * Gets a set containing all the keys in a file.
     *
     * @param path
     *     The path to a file.
     * @return A set with all the keys used in the file.
     */
    static Set<String> getKeySet(Path path)
    {
        if (!Files.isRegularFile(path))
            return Collections.emptySet();
        try (InputStream inputStream = Files.newInputStream(path))
        {
            return getKeySet(inputStream);
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to get keys from file: " + path);
            return Collections.emptySet();
        }
    }

    /**
     * Gets a set containing all the keys in a list of strings.
     *
     * @param lines
     *     The lines containing "key=value" mappings.
     * @return A set with all the keys used in the lines.
     */
    static Set<String> getKeySet(List<String> lines)
    {
        final Set<String> ret = new LinkedHashSet<>(lines.size());
        for (final String line : lines)
        {
            final @Nullable String key = getKeyFromLine(line);
            if (key != null)
                ret.add(key);
        }
        return ret;
    }

    /**
     * Retrieves the key from a String with the format "key=value".
     *
     * @param line
     *     A string containing a key/value pair.
     * @return The key as used in the line.
     */
    static @Nullable String getKeyFromLine(String line)
    {
        final char[] chars = new char[line.length()];
        line.getChars(0, line.length(), chars, 0);

        for (int idx = 0; idx < line.length(); ++idx)
            if (chars[idx] == '=')
                return line.substring(0, idx);
        return null;
    }

    /**
     * Reads all lines from an {@link InputStream} and applies a {@link Consumer} for each line.
     *
     * @param inputStream
     *     The input stream to read the data from.
     * @param fun
     *     The function to apply for each line retrieved from the input stream.
     */
    static void readFile(InputStream inputStream, Consumer<String> fun)
    {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream)))
        {
            for (String line; (line = bufferedReader.readLine()) != null; )
            {
                if (line.length() == 0)
                    continue;
                if (line.charAt(0) == '#')
                    continue;
                fun.accept(line);
            }
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to read localization file!");
        }
    }

    /**
     * Reads all the lines from an {@link InputStream} and stores each line in a list.
     *
     * @param inputStream
     *     The input stream to read the data from.
     * @return A list of Strings where every string represents a single line in the provided input stream.
     */
    static List<String> readFile(InputStream inputStream)
    {
        final List<String> ret = new ArrayList<>();
        readFile(inputStream, ret::add);
        return ret;
    }

    /**
     * Reads all the lines from a file and stores each line in a list.
     *
     * @param path
     *     The path to the file to read the data from.
     * @param fun
     *     The function to apply for each line retrieved from the input stream.
     * @return A list of Strings where every string represents a single line in the provided file.
     */
    static void readFile(Path path, Consumer<String> fun)
    {
        if (!Files.isRegularFile(path))
            return;

        try (InputStream inputStream = Files.newInputStream(path))
        {
            readFile(inputStream, fun);
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to read file: " + path);
        }
    }

    /**
     * Retrieves all the {@link LocaleFile}s in a directory.
     *
     * @param directory
     *     The directory to search in.
     * @param baseName
     *     The base name of the locale files.
     * @return A list of {@link LocaleFile}s found in the directory.
     */
    static List<LocaleFile> getLocaleFilesInDirectory(Path directory, @Nullable String baseName)
    {
        try (Stream<Path> stream = Files.list(directory))
        {
            return getLocaleFiles(baseName, stream.filter(file -> !Files.isDirectory(file)).toList());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a list of {@link LocaleFile}s from a base name and a list of files.
     *
     * @param baseName
     *     The base name of the translation files.
     * @param files
     *     The list of files to look at.
     * @return A list of {@link LocaleFile}s that includes all files that meet the correct naming format of
     * "directory/basename[_locale].properties".
     */
    static List<LocaleFile> getLocaleFiles(@Nullable String baseName, List<Path> files)
    {
        final ArrayList<LocaleFile> ret = new ArrayList<>(files.size());
        for (final Path file : files)
        {
            final @Nullable String locale = parseLocaleFile(baseName, file.getFileName().toString());
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
    static List<LocaleFile> getLocaleFiles(List<String> resources)
    {
        return getLocaleFiles(FileSystems.getDefault(), resources);
    }

    /**
     * Gets a list of {@link LocaleFile}s from a list of filenames.
     * <p>
     * The file names should not include any directories.
     *
     * @param fileSystem
     *     The {@link FileSystem} to create the paths on.
     * @param resources
     *     The list of file names.
     * @return A list of {@link LocaleFile}s that includes all files that meet the correct naming format:
     * "whatever-filename[_locale].properties".
     */
    static List<LocaleFile> getLocaleFiles(FileSystem fileSystem, List<String> resources)
    {
        final ArrayList<LocaleFile> ret = new ArrayList<>(resources.size());
        for (final String resource : resources)
        {
            final @Nullable String locale = parseLocaleFile(resource);
            if (locale != null)
                ret.add(new LocaleFile(fileSystem.getPath(resource), locale));
        }
        ret.trimToSize();
        return ret;
    }

    /**
     * Retrieves the locale from a locale file with the format "basename[_locale].properties".
     *
     * @param baseName
     *     The base name of the translation files.
     * @param fileName
     *     The name of the location file. This should only include the name of the file itself and not its parent
     *     directories.
     * @return The locale as used in the filename.
     */
    static @Nullable String parseLocaleFile(@Nullable String baseName, String fileName)
    {
        if (baseName != null && !fileName.startsWith(baseName))
            return null;

        if (!fileName.endsWith(".properties"))
            return null;

        String locale = fileName.replace(".properties", "");
        if (baseName != null)
            locale = locale.replace(baseName, "");

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
     * @param fileName
     *     The name of a locale file.
     * @return The locale as extracted from the locale file or null if the file is not a properties file.
     * <p>
     * For example, in the case of a file "Translated_en_US.properties", this method would return "en_US".
     * <p>
     * If the file does not have a locale (e.g. "Translated.properties"), an empty String is returned.
     */
    static @Nullable String parseLocaleFile(String fileName)
    {
        if (!fileName.endsWith(".properties"))
            return null;
        final String[] parts = fileName.replace(".properties", "").split("_", 2);
        return parts.length == 1 ? "" : parts[1];
    }

    /**
     * Creates a new {@link FileSystem} for a zip file.
     * <p>
     * Don't forget to close it when you're done!
     *
     * @param zipFile
     *     The zip file for which to create a new FileSystem.
     * @return The newly created FileSystem.
     *
     * @throws IOException
     *     If an I/O error occurs creating the file system.
     * @throws FileSystemAlreadyExistsException
     *     When a FileSystem already exists for the provided file.
     */
    static FileSystem createNewFileSystem(Path zipFile)
        throws IOException, URISyntaxException, ProviderNotFoundException
    {
        return FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), Map.of());
    }

    /**
     * Retrieves the filename of the output locale file for a specific locale.
     *
     * @param locale
     *     The locale for which to find the output filename.
     * @return The filename of the output locale file for the provided locale.
     */
    static String getOutputLocaleFileName(String outputBaseName, String locale)
    {
        return String.format("%s%s.properties", outputBaseName, locale.length() == 0 ? "" : ("_" + locale));
    }

    /**
     * Ensures a given zip file exists.
     */
    static void ensureZipFileExists(Path zipFile)
    {
        if (Files.exists(zipFile))
            return;

        final Path parent = zipFile.getParent();
        if (parent != null)
        {
            try
            {
                Files.createDirectories(parent);
            }
            catch (IOException e)
            {
                BigDoors.get().getPLogger().logThrowable(e, "Failed to create directories: " + parent);
                return;
            }
        }

        // Just opening the ZipOutputStream and then letting it close
        // on its own is enough to create a new zip file.
        //noinspection EmptyTryBlock
        try (ZipOutputStream ignored = new ZipOutputStream(Files.newOutputStream(zipFile)))
        {
            // ignored
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to create file: " + zipFile);
        }
    }

    /**
     * Gets the currently-available list of {@link Locale}s as found in the provided zip-file.
     *
     * @param zipFile
     *     The zip file in which to look for localization files.
     * @param baseName
     *     The base name of the localization files.
     */
    static List<Locale> getLocalesInZip(Path zipFile, String baseName)
    {
        try (FileSystem fs = createNewFileSystem(zipFile))
        {
            return LocalizationUtil.getLocaleFilesInDirectory(fs.getPath("."), baseName).stream()
                                   .map(localeFile -> getLocale(localeFile.locale())).toList();
        }
        catch (IOException | URISyntaxException | ProviderNotFoundException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to find locales in file: " + zipFile);
            return Collections.emptyList();
        }
    }

    /**
     * Gets a {@link Locale} from a String representing a locale. E.g. "en_US".
     * <p>
     * If the locale string is empty, the root locale is returned. See {@link Locale#ROOT}.
     *
     * @param localeStr
     *     A String representing a locale.
     * @return The decoded Locale.
     */
    public static Locale getLocale(String localeStr)
    {
        final String[] parts = localeStr.split("_", 3);
        if (parts[0].isBlank())
            return Locale.ROOT;

        if (parts.length == 1)
            return new Locale(parts[0]);
        if (parts.length == 2)
            return new Locale(parts[0], parts[1]);
        return new Locale(parts[0], parts[1], parts[2]);
    }
}
