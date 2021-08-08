package nl.pim16aap2.bigdoors.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class LocalizationGeneratorIntegrationTest
{
    private static final @NotNull String BASE_NAME = "Translation";
    private static final @NotNull String BASE_NAME_A = "Translation-A";
    private static final @NotNull String BASE_NAME_B = "Translation-B";

    private static final @NotNull List<String> INPUT_A_0 =
        List.of("a_key0=val0", "a_key1=val1", "a_key2=val2", "a_key3=val3", "a_key4=val4");

    private static final @NotNull List<String> INPUT_A_1 =
        List.of("b_key0=val0", "b_key1=val1", "b_key2=val2", "b_key3=val3", "b_key4=val4");

    private static final @NotNull List<String> INPUT_B_0 =
        List.of("a_key3=val100", "a_key4=val101", "a_key5=val102", "a_key6=val103", "a_key7=val104");

    private static final @NotNull List<String> OUTPUT_0 = List.of(
        // From INPUT_A_0
        "a_key0=val0", "a_key1=val1", "a_key2=val2", "a_key3=val3", "a_key4=val4",
        // From INPUT_B_0
        "a_key5=val102", "a_key6=val103", "a_key7=val104");

    private static final @NotNull List<String> OUTPUT_1 = List.of(
        // From INPUT_A_1
        "b_key0=val0", "b_key1=val1", "b_key2=val2", "b_key3=val3", "b_key4=val4");

    /**
     * byte array representation of the class compiled from the following source:
     * <p>
     * {@code package org.example.project; public class LocalizationGeneratorDummyClass { }}
     */
    private static final byte[] LOCALIZATION_GENERATOR_DUMMY_CLASS_DATA = Base64.getDecoder().decode(
        "yv66vgAAADwADQoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQAzb3JnL2V4" +
            "YW1wbGUvcHJvamVjdC9Mb2NhbGl6YXRpb25HZW5lcmF0b3JEdW1teUNsYXNzAQAEQ29kZQEAD0xpbmVOdW1iZXJU" +
            "YWJsZQEAClNvdXJjZUZpbGUBACRMb2NhbGl6YXRpb25HZW5lcmF0b3JEdW1teUNsYXNzLmphdmEAIQAHAAIAAAAAA" +
            "AEAAQAFAAYAAQAJAAAAHQABAAEAAAAFKrcAAbEAAAABAAoAAAAGAAEAAAABAAEACwAAAAIADA==");


    private FileSystem fs;
    private Path directoryOutput;

    @BeforeEach
    void init()
        throws IOException
    {
        val platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());

        fs = Jimfs.newFileSystem(Configuration.unix());
        directoryOutput = Files.createDirectory(fs.getPath("/output"));
    }

    @AfterEach
    void cleanup()
        throws IOException
    {
        fs.close();
    }

    @Test
    void testAddResources()
        throws IOException, URISyntaxException
    {
        val directoryA = Files.createDirectory(fs.getPath("/input_a"));
        val directoryB = Files.createDirectory(fs.getPath("/input_b"));

        val inputPathA0 = directoryA.resolve(BASE_NAME_A + ".properties");
        val inputPathA1 = directoryA.resolve(BASE_NAME_A + "_en_US.properties");
        val inputPathB0 = directoryB.resolve(BASE_NAME_B + ".properties");

        writeToFile(inputPathA0, INPUT_A_0);
        writeToFile(inputPathA1, INPUT_A_1);
        writeToFile(inputPathB0, INPUT_B_0);

        val localizationGenerator = new LocalizationGenerator(directoryOutput, BASE_NAME);
        localizationGenerator.addResources(directoryA, BASE_NAME_A);
        localizationGenerator.addResources(directoryB, BASE_NAME_B);

        val outputFileSystem = createFileSystem(directoryOutput.resolve(BASE_NAME + ".bundle"));
        val outputFile0 = outputFileSystem.getPath(BASE_NAME + ".properties");
        val outputFile1 = outputFileSystem.getPath(BASE_NAME + "_en_US.properties");

        Assertions.assertEquals(OUTPUT_0, LocalizationUtil.readFile(Files.newInputStream(outputFile0)));
        Assertions.assertEquals(OUTPUT_1, LocalizationUtil.readFile(Files.newInputStream(outputFile1)));
    }

    @NotNull FileSystem createFileSystem(@NotNull Path zipFile)
        throws URISyntaxException, IOException
    {
        return FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), Map.of());
    }

    @Test
    void testAddResourcesFromJar()
        throws IOException, URISyntaxException
    {
        val jarFile = Files.createFile(Files.createDirectory(fs.getPath("/input")).resolve("test.jar"));
        createJar(jarFile).close();

        val localizationGenerator = new LocalizationGenerator(directoryOutput, BASE_NAME);
        localizationGenerator.addResourcesFromZip(jarFile, null);

        verifyJarOutput();
    }

    @Test
    void testAddResourcesFromClass()
        throws IOException, ClassNotFoundException, URISyntaxException
    {
        val jarFile = Files.createFile(Files.createDirectory(fs.getPath("/input")).resolve("test.jar"));
        val outputStream = createJar(jarFile);
        writeEntry(outputStream, "org/example/project/LocalizationGeneratorDummyClass.class",
                   LOCALIZATION_GENERATOR_DUMMY_CLASS_DATA);
        outputStream.close();

        val dummyClass = Class.forName("org.example.project.LocalizationGeneratorDummyClass", true, loadJar(jarFile));
        val localizationGenerator = new LocalizationGenerator(directoryOutput, BASE_NAME);
        localizationGenerator.addResources(dummyClass, null);

        verifyJarOutput();
    }

    /**
     * Verifies that the files generated by the {@link LocalizationGenerator} based on the jar created with {@link
     * #createJar(Path)} are correct.
     *
     * @throws IOException
     */
    private void verifyJarOutput()
        throws IOException, URISyntaxException
    {
        val outputFileSystem = createFileSystem(directoryOutput.resolve(BASE_NAME + ".bundle"));
        val outputFile0 = outputFileSystem.getPath(BASE_NAME + ".properties");
        val outputFile1 = outputFileSystem.getPath(BASE_NAME + "_en_US.properties");


        Assertions.assertTrue(Files.exists(outputFile0));
        Assertions.assertTrue(Files.exists(outputFile1));

        Assertions.assertEquals(INPUT_B_0, LocalizationUtil.readFile(Files.newInputStream(outputFile1)));

        val outputBase = new ArrayList<>(10);
        outputBase.addAll(INPUT_A_0);
        outputBase.addAll(INPUT_A_1);
        Assertions.assertEquals(outputBase, LocalizationUtil.readFile(Files.newInputStream(outputFile0)));
    }

    /**
     * Creates a jar file with three locale files.
     *
     * @param jarFile An (existing) jar file.
     * @return The {@link ZipOutputStream} with the jar file. This can be used to append more data. Don't forget to
     * close this.
     *
     * @throws IOException
     */
    private @NotNull ZipOutputStream createJar(@NotNull Path jarFile)
        throws IOException
    {
        val outputStream = new ZipOutputStream(Files.newOutputStream(jarFile));
        writeEntry(outputStream, BASE_NAME_A + ".properties", INPUT_A_0);
        writeEntry(outputStream, BASE_NAME + ".properties", INPUT_A_1);
        writeEntry(outputStream, BASE_NAME_B + "_en_US.properties", INPUT_B_0);
        return outputStream;
    }

    /**
     * Creates a new {@link URLClassLoader} and loads a jar with it.
     *
     * @param jar The path to the jar to load.
     * @return A new {@link URLClassLoader}.
     *
     * @throws MalformedURLException
     */
    private @NotNull URLClassLoader loadJar(@NotNull Path jar)
        throws MalformedURLException
    {
        return new URLClassLoader("URLClassLoader_LocalizationGeneratorIntegrationTest",
                                  new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
    }

    /**
     * Appends a list of Strings to a file. Each entry in the list will be printed on its own line.
     *
     * @param file  The file to append the lines to.
     * @param lines The lines to write to the file.
     * @throws IOException
     */
    private static void writeToFile(@NotNull Path file, @NotNull List<String> lines)
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
    private static void writeEntry(@NotNull ZipOutputStream outputStream, @NotNull String fileName,
                                   @NotNull List<String> lines)
        throws IOException
    {
        val sb = new StringBuilder();
        for (val line : lines)
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
}
