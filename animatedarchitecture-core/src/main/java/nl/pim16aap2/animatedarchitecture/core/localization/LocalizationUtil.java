package nl.pim16aap2.animatedarchitecture.core.localization;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.FileUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Represents a utility class with methods that can be used for the localization system.
 */
@Flogger
public final class LocalizationUtil
{
    /**
     * The list of all installed locales.
     *
     * @see Locale#getAvailableLocales()
     */
    private static final List<Locale> AVAILABLE_LOCALES = List.of(Locale.getAvailableLocales());

    /**
     * Looks for top-level .properties files.
     */
    private static final Pattern LOCALE_FILE_PATTERN = Pattern.compile("^[\\w-]+\\.properties");

    private LocalizationUtil()
    {
        // utility class
    }

    /**
     * Gets the names of all locale files in a jar.
     * <p>
     * The name of each locale file is the name of the file itself, with optional relative path.
     *
     * @param jarFile
     *     The jar file to search in.
     * @return The names of all locale files in the jar.
     *
     * @throws IOException
     *     If an I/O error occurs.
     */
    public static List<String> getLocaleFilesInJar(Path jarFile)
        throws IOException
    {
        return FileUtil.getFilesInJar(jarFile, LOCALE_FILE_PATTERN);
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
            final @Nullable LocalizationEntry entry = getEntryFromLine(line);
            if (entry == null || keys.contains(entry.key()))
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
            final @Nullable LocalizationEntry entry = getEntryFromLine(line);
            if (entry != null)
                ret.add(entry.key());
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
            log.atSevere().withCause(e).log("Failed to get keys from file: %s", path);
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
        final Set<String> ret = new LinkedHashSet<>(MathUtil.ceil(1.25 * lines.size()));
        for (final String line : lines)
        {
            final @Nullable LocalizationEntry entry = getEntryFromLine(line);
            if (entry != null)
                ret.add(entry.key());
        }
        return ret;
    }

    /**
     * Gets a set containing all the keys in an input stream.
     *
     * @param inputStream
     *     The input stream to read lines of Strings from. These lines are expected to be of the format "key=value".
     * @return A set with all the keys used in the input stream.
     */
    static Map<String, String> getEntryMap(InputStream inputStream)
    {
        final Map<String, String> ret = new TreeMap<>();
        readFile(inputStream, line ->
        {
            final @Nullable LocalizationEntry key = getEntryFromLine(line);
            if (key != null)
                ret.put(key.key(), key.value());
        });
        return ret;
    }

    /**
     * Gets a map containing all the key-value pairs in a file.
     *
     * @param path
     *     The path to a file.
     * @return A map with all the key-value pairs in the file.
     */
    static Map<String, String> getEntryMap(Path path)
    {
        if (!Files.isRegularFile(path))
            return Collections.emptyMap();
        try (InputStream inputStream = Files.newInputStream(path))
        {
            return getEntryMap(inputStream);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to get entries from file: %s", path);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves the key from a String with the format "key=value".
     *
     * @param line
     *     A string containing a key/value pair.
     * @return The key as used in the line.
     */
    static @Nullable LocalizationEntry getEntryFromLine(String line)
    {
        final char[] chars = new char[line.length()];
        line.getChars(0, line.length(), chars, 0);

        for (int idx = 0; idx < line.length(); ++idx)
            if (chars[idx] == '=')
                return new LocalizationEntry(line.substring(0, idx), line.substring(idx + 1));
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
        try (
            BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
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
            log.atSevere().withCause(e).log("Failed to read localization file!");
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
     * Reads all the lines from a file and applies a function to each line.
     *
     * @param path
     *     The path to the file to read the data from.
     * @param fun
     *     The function to apply for each line retrieved from the input stream.
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
            log.atSevere().withCause(e).log("Failed to read file: %s", path);
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
     * Gets the currently-available list of {@link Locale}s as found in the provided zip-file.
     *
     * @param zipFile
     *     The zip file in which to look for localization files.
     * @param baseName
     *     The base name of the localization files.
     */
    static List<Locale> getLocalesInZip(Path zipFile, String baseName)
    {
        try (FileSystem fs = FileUtil.createNewFileSystem(zipFile))
        {
            return LocalizationUtil
                .getLocaleFilesInDirectory(fs.getPath("."), baseName)
                .stream()
                .map(localeFile -> parseLocale(localeFile.locale()))
                .filter(Objects::nonNull)
                .toList();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to find locales in file: %s", zipFile);
            return Collections.emptyList();
        }
    }

    /**
     * Parses a {@link Locale} from a String representing a locale. E.g. "en_US".
     * <p>
     * When the provided locale is null or blank, {@link Locale#ROOT} is returned.
     *
     * @param locale
     *     A String representing a locale.
     * @return The parse Locale.
     */
    public static @Nullable Locale parseLocale(@Nullable String locale)
    {
        if (locale == null || locale.isBlank())
            return Locale.ROOT;

        final var languages = Locale.LanguageRange.parse(locale.replace('_', '-'));
        final Locale parsed = Locale.lookup(languages, AVAILABLE_LOCALES);

        if (parsed == null)
            log.atSevere().log("Failed to parse locale: '%s'", locale);

        log.atFine().log("Parsed locale: '%s' -> '%s'", locale, parsed);
        return parsed;
    }
}
