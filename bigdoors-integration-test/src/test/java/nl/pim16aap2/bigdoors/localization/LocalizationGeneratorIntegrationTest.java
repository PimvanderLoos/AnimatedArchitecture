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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private FileSystem fs;
    private Path directoryOutput;
    private Path outputPath0;
    private Path outputPath1;

    @BeforeEach
    void init()
        throws IOException
    {
        val platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());

        fs = Jimfs.newFileSystem(Configuration.unix());
        directoryOutput = Files.createDirectory(fs.getPath("/output"));
        outputPath0 = directoryOutput.resolve(BASE_NAME + ".properties");
        outputPath1 = directoryOutput.resolve(BASE_NAME + "_en_US.properties");
    }

    @AfterEach
    void cleanup()
        throws IOException
    {
        fs.close();
    }

    @Test
    void testAddResources()
        throws IOException
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

        Assertions.assertEquals(OUTPUT_0, LocalizationUtil.readFile(Files.newInputStream(outputPath0)));
        Assertions.assertEquals(OUTPUT_1, LocalizationUtil.readFile(Files.newInputStream(outputPath1)));
    }

    private static void writeToFile(@NotNull Path file, @NotNull List<String> lines)
        throws IOException
    {
        LocalizationUtil.ensureFileExists(file);
        LocalizationUtil.appendToFile(file, lines);
    }

    @Test
    void testAddResourcesFromJar()
        throws IOException
    {
        val fileDir = Files.createDirectory(fs.getPath("/input"));
        val jarFile = Files.createFile(fileDir.resolve("test.zip"));
        val outputStream = new ZipOutputStream(Files.newOutputStream(jarFile));
        writeEntry(outputStream, BASE_NAME_A + ".properties", INPUT_A_0);
        writeEntry(outputStream, BASE_NAME + ".properties", INPUT_A_1);
        writeEntry(outputStream, BASE_NAME_B + "_en_US.properties", INPUT_B_0);
        outputStream.close();

        val localizationGenerator = new LocalizationGenerator(directoryOutput, BASE_NAME);
        localizationGenerator.addResourcesFromZip(jarFile, null);

        Assertions.assertTrue(Files.exists(outputPath0));
        Assertions.assertTrue(Files.exists(outputPath1));

        Assertions.assertEquals(INPUT_B_0, LocalizationUtil.readFile(Files.newInputStream(outputPath1)));

        val outputBase = new ArrayList<>(10);
        outputBase.addAll(INPUT_A_0);
        outputBase.addAll(INPUT_A_1);
        Assertions.assertEquals(outputBase, LocalizationUtil.readFile(Files.newInputStream(outputPath0)));
    }

    private static void writeEntry(@NotNull ZipOutputStream outputStream, @NotNull String fileName,
                                   @NotNull List<String> lines)
        throws IOException
    {
        val sb = new StringBuilder();
        for (val line : lines)
            sb.append(line).append("\n");
        writeEntry(outputStream, fileName, sb.toString().getBytes());
    }

    private static void writeEntry(@NotNull ZipOutputStream outputStream, @NotNull String fileName,
                                   byte[] data)
        throws IOException
    {
        outputStream.putNextEntry(new ZipEntry(fileName));
        outputStream.write(data, 0, data.length);
        outputStream.closeEntry();
    }
}
