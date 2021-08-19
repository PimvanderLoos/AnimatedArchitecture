package nl.pim16aap2.bigdoors.localization;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class LocalizationTestingUtilities
{

    static @NotNull FileSystem createFileSystem(@NotNull Path zipFile)
        throws URISyntaxException, IOException
    {
        return FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), Map.of());
    }

    /**
     * Creates a new {@link URLClassLoader} and loads a jar with it.
     *
     * @param jar               The path to the jar to load.
     * @param parentClassLoader The parent class loader.
     * @return A new {@link URLClassLoader}.
     *
     * @throws MalformedURLException
     */
    static @NotNull URLClassLoader loadJar(@NotNull Path jar, ClassLoader parentClassLoader)
        throws MalformedURLException
    {
        return new URLClassLoader("URLClassLoader_LocalizationGeneratorIntegrationTest",
                                  new URL[]{jar.toUri().toURL()}, parentClassLoader);
    }

    static void addFilesToZip(@NotNull Path zipFile, @NotNull String... names)
        throws IOException
    {
        final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(zipFile));
        for (final String name : names)
        {
            final byte[] data = "".getBytes();
            outputStream.putNextEntry(new ZipEntry(name));
            outputStream.write(data, 0, data.length);
            outputStream.closeEntry();
        }
        outputStream.close();
    }

    /**
     * Appends a list of Strings to a file. Each entry in the list will be printed on its own line.
     *
     * @param file  The file to append the lines to.
     * @param lines The lines to write to the file.
     * @throws IOException
     */
    static void writeToFile(@NotNull Path file, @NotNull List<String> lines)
        throws IOException
    {
        LocalizationUtil.ensureFileExists(file);
        LocalizationUtil.appendToFile(file, lines);
    }

    /**
     * Writes a new entry (file) in a zip file.
     *
     * @param outputStream The output stream to write the new entry to.
     * @param fileName     The name of the entry (file) to write in the zip file.
     * @param lines        The lines to write to the entry.
     * @throws IOException
     */
    static void writeEntry(@NotNull ZipOutputStream outputStream, @NotNull String fileName,
                           @NotNull List<String> lines)
        throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        for (final String line : lines)
            sb.append(line).append("\n");
        writeEntry(outputStream, fileName, sb.toString().getBytes());
    }


    /**
     * Writes a new entry (file) in a zip file.
     *
     * @param outputStream The output stream to write the new entry to.
     * @param fileName     The name of the entry (file) to write in the zip file.
     * @param data         The data to write to the entry.
     * @throws IOException
     */
    static void writeEntry(@NotNull ZipOutputStream outputStream, @NotNull String fileName,
                           byte[] data)
        throws IOException
    {
        outputStream.putNextEntry(new ZipEntry(fileName));
        outputStream.write(data, 0, data.length);
        outputStream.closeEntry();
    }

    @SuppressWarnings("SameParameterValue")
    static void appendToFile(@NotNull Path zipFile, @NotNull String file, @NotNull String toAppend)
        throws URISyntaxException, IOException
    {
        final FileSystem bundleFileSystem = FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), Map.of());
        Files.write(bundleFileSystem.getPath(file), toAppend.getBytes(), StandardOpenOption.APPEND);
        bundleFileSystem.close();
    }
}
