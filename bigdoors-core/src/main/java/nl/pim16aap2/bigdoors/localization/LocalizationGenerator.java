package nl.pim16aap2.bigdoors.localization;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a class that can generate a localization file from multiple sources.
 * <p>
 * All sources are merged and written to a new set of files in the specified output directory with the specified base
 * name.
 *
 * @author Pim
 */
public class LocalizationGenerator extends Restartable
{
    private final @NotNull Object lck = new Object();

    private final @NotNull Path outputDirectory;
    private final @NotNull String outputBaseName;
    private final @NotNull Map<String, LocaleFile> outputLocaleFiles = new HashMap<>();

    /**
     * @param holder          The {@link IRestartableHolder} to register this {@link Restartable} with.
     * @param outputDirectory The output directory to write all the combined localizations into.
     * @param outputBaseName  The base name of the properties files in the output directory.
     */
    public LocalizationGenerator(@NotNull IRestartableHolder holder,
                                 @NotNull Path outputDirectory, @NotNull String outputBaseName)
    {
        super(holder);
        this.outputDirectory = outputDirectory;
        this.outputBaseName = outputBaseName;
    }

    /**
     * @param outputDirectory The output directory to write all the combined localizations into.
     * @param outputBaseName  The base name of the properties files in the output directory.
     */
    public LocalizationGenerator(@NotNull Path outputDirectory, @NotNull String outputBaseName)
    {
        this(BigDoors.get(), outputDirectory, outputBaseName);
    }

    /**
     * Adds a new set of resources to the current localization set.
     * <p>
     * Only files of the format "directory/basename[_locale].properties" are included. "[_locale]" is optional here.
     *
     * @param baseName  The base name of the properties files.
     * @param directory The directory of the properties files.
     */
    public void addResources(@NotNull String baseName, @NotNull Path directory)
    {
        synchronized (lck)
        {
            try
            {
                val localeFiles = getLocaleFilesInDirectory(baseName, directory);
                for (val localeFile : localeFiles)
                    mergeWithLocaleFile(localeFile);
            }
            catch (Exception e)
            {
                BigDoors.get().getPLogger().logThrowable(e, "Failed to add resources from directory \"" +
                    directory + "\" with base name: \"" + baseName + "\"");
            }
        }
    }

    @GuardedBy("lck")
    void mergeWithLocaleFile(@NotNull LocaleFile localeFile)
        throws IOException
    {
        val existingLocaleFile = getExistingLocaleFile(localeFile.locale());
        ensureFileExists(existingLocaleFile);
        val existing = readFile(existingLocaleFile);
        val newlines = readFile(localeFile.path());
        val appendable = getAppendable(existing, newlines);
        appendToFile(existingLocaleFile, appendable);
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

    static void appendToFile(@NotNull Path path, @NotNull List<String> append)
    {
        System.out.println("Writing to file: " + path);
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
     * Gets a set containing all the keys in a list of string, assuming
     *
     * @param lines
     * @return
     */
    static @NotNull Set<String> getKeySet(@NotNull List<String> lines)
    {
        final Set<String> ret = new HashSet<>();
        for (val line : lines)
        {
            val key = getKeyFromLine(line);
            if (key != null)
                ret.add(key);
        }
        return ret;
    }

    static @Nullable String getKeyFromLine(@NotNull String line)
    {
        val parts = line.split("=", 2);
        return parts.length == 2 ? parts[0] : null;
    }

    @NotNull Path getExistingLocaleFile(@NotNull String locale)
    {
        val fileName = String.format("%s%s.properties", outputBaseName, locale.length() == 0 ? "" : ("_" + locale));
        return outputDirectory.resolve(fileName);
    }

    static @NotNull List<String> readFile(@NotNull Path file)
    {
        if (!Files.isRegularFile(file))
            return Collections.emptyList();
        final ArrayList<String> tmp = new ArrayList<>();

        try (val bufferedReader = Files.newBufferedReader(file))
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
            BigDoors.get().getPLogger().logThrowable(e, "Failed to read localization file: " + file);
            return Collections.emptyList();
        }
        return tmp;
    }

    static @NotNull List<LocaleFile> getLocaleFilesInDirectory(@NotNull String baseName, @NotNull Path directory)
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
     * @return A list of {@link LocaleFile}s that includes all files that met the correct naming format of
     * "directory/basename[_locale].properties".
     */
    static @NotNull List<LocaleFile> getLocaleFiles(@NotNull String baseName, @NotNull List<Path> files)
    {
        final @NotNull ArrayList<LocaleFile> ret = new ArrayList<>(files.size());
        for (val file : files)
        {
            @Nullable val localeFile = parseLocaleFile(baseName, file.getFileName().toString());
            if (localeFile != null)
                ret.add(new LocaleFile(file, localeFile));
        }
        ret.trimToSize();
        return ret;
    }

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

    @Override
    public void restart()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        outputLocaleFiles.clear();
    }

    record LocaleFile(@NotNull Path path, @NotNull String locale) {}
}
