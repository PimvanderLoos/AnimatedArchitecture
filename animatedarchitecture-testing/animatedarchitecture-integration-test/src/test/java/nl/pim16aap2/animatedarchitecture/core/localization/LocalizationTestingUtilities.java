package nl.pim16aap2.animatedarchitecture.core.localization;

import nl.pim16aap2.animatedarchitecture.core.util.FileUtil;

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

    static FileSystem createFileSystem(Path zipFile)
        throws URISyntaxException, IOException
    {
        return FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), Map.of());
    }

    /**
     * Creates a new {@link URLClassLoader} and loads a jar with it.
     *
     * @param jar
     *     The path to the jar to load.
     * @param parentClassLoader
     *     The parent class loader.
     * @return A new {@link URLClassLoader}.
     *
     * @throws MalformedURLException
     */
    static URLClassLoader loadJar(Path jar, ClassLoader parentClassLoader)
        throws MalformedURLException
    {
        return new URLClassLoader(
            "URLClassLoader_LocalizationGeneratorIntegrationTest",
            new URL[]{jar.toUri().toURL()},
            parentClassLoader
        );
    }

    static void addFilesToZip(Path zipFile, String... names)
        throws IOException
    {
        final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(zipFile));
        for (String name : names)
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
     * @param file
     *     The file to append the lines to.
     * @param lines
     *     The lines to write to the file.
     * @throws IOException
     */
    static void writeToFile(Path file, List<String> lines)
        throws IOException
    {
        FileUtil.ensureFileExists(file);
        FileUtil.appendToFile(file, lines);
    }

    /**
     * Writes a new entry (file) in a zip file.
     *
     * @param outputStream
     *     The output stream to write the new entry to.
     * @param fileName
     *     The name of the entry (file) to write in the zip file.
     * @param lines
     *     The lines to write to the entry.
     * @throws IOException
     */
    static void writeEntry(ZipOutputStream outputStream, String fileName, List<String> lines)
        throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(line).append('\n');
        writeEntry(outputStream, fileName, sb.toString().getBytes());
    }


    /**
     * Writes a new entry (file) in a zip file.
     *
     * @param outputStream
     *     The output stream to write the new entry to.
     * @param fileName
     *     The name of the entry (file) to write in the zip file.
     * @param data
     *     The data to write to the entry.
     * @throws IOException
     */
    static void writeEntry(ZipOutputStream outputStream, String fileName, byte[] data)
        throws IOException
    {
        outputStream.putNextEntry(new ZipEntry(fileName));
        outputStream.write(data, 0, data.length);
        outputStream.closeEntry();
    }

    @SuppressWarnings("SameParameterValue")
    static void appendToFileInZip(Path zipFile, String file, String toAppend)
        throws URISyntaxException, IOException
    {
        final FileSystem bundleFileSystem = FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), Map.of());
        Files.writeString(bundleFileSystem.getPath(file), toAppend, StandardOpenOption.APPEND);
        bundleFileSystem.close();
    }
}
