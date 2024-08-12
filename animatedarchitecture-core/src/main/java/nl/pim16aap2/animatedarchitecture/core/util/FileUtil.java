package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.extern.flogger.Flogger;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for file operations.
 */
@Flogger
public final class FileUtil
{

    private FileUtil()
    {
    }

    /**
     * Deletes a file if it exists.
     * <p>
     * If an exception occurs while deleting the file, it will be logged, but the method will not throw an exception.
     * <p>
     * If the file does not exist, nothing happens.
     *
     * @param file
     *     The file to delete.
     */
    public static void deleteFile(Path file)
    {
        try
        {
            Files.deleteIfExists(file);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to delete file: '%s'", file);
        }
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
    public static Path ensureFileExists(Path file)
        throws IOException
    {
        if (Files.isRegularFile(file))
            return file;

        final Path parent = file.getParent();
        if (parent != null && !Files.isDirectory(parent))
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
    public static void appendToFile(Path path, List<String> append)
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
     * Ensures a directory exists.
     */
    public static void ensureDirectoryExists(Path dir)
    {
        if (Files.exists(dir))
            return;

        try
        {
            Files.createDirectories(dir);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to create directories: %s", dir);
        }
    }

    /**
     * Ensures a given zip file exists.
     */
    public static void ensureZipFileExists(Path zipFile)
    {
        if (Files.exists(zipFile))
            return;

        final Path parent = zipFile.getParent();
        if (parent != null)
        {
            try
            {
                if (!Files.isDirectory(parent))
                    Files.createDirectories(parent);
            }
            catch (IOException e)
            {
                log.atSevere().withCause(e).log("Failed to create directories: %s", parent);
                return;
            }
        }

        // Just opening the ZipOutputStream and then letting it close
        // on its own is enough to create a new zip file.
        //noinspection EmptyTryBlock
        try (
            OutputStream outputStream = Files.newOutputStream(zipFile);
            ZipOutputStream ignored = new ZipOutputStream(outputStream))
        {
            // ignored
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to create file: %s", zipFile);
        }
    }

    /**
     * Gets the file of the jar that contained a specific class.
     *
     * @param clz
     *     The class for which to find the jar file.
     * @return The location of the jar file.
     */
    public static Path getJarFile(Class<?> clz)
    {
        try
        {
            return Path.of(clz.getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to find jar file for class: " + clz, e);
        }
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
    public static FileSystem createNewFileSystem(Path zipFile)
        throws IOException, URISyntaxException, ProviderNotFoundException
    {
        final URI uri = new URI("jar:" + zipFile.toUri());
        try
        {
            return FileSystems.newFileSystem(uri, Map.of());
        }
        catch (FileSystemAlreadyExistsException e)
        {
            throw new RuntimeException("Failed to create new filesystem: '" + uri + "'", e);
        }
    }

    /**
     * Gets the names of all files inside a jar that match a certain pattern.
     *
     * @param jarFile
     *     The jar file to search in.
     * @param pattern
     *     The pattern to match the file names against.
     * @return The names of all files in the jar that match the pattern.
     *
     * @throws IOException
     *     If an I/O error occurs.
     */
    public static List<String> getFilesInJar(Path jarFile, Pattern pattern)
        throws IOException
    {
        final List<String> ret = new ArrayList<>();

        try (val zipInputStream = new ZipInputStream(Files.newInputStream(jarFile)))
        {
            @Nullable ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                final var name = entry.getName();
                if (pattern.matcher(name).matches())
                    ret.add(name);
            }
        }
        return ret;
    }
}
