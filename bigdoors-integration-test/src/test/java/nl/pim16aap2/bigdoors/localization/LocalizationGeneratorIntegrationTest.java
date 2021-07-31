package nl.pim16aap2.bigdoors.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class LocalizationGeneratorIntegrationTest
{
    private static final @NotNull String BASE_NAME = "Translation";
    private static final @NotNull String BASE_NAME_A = "Translation_A";
    private static final @NotNull String BASE_NAME_B = "Translation_B";

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
        fs = Jimfs.newFileSystem(Configuration.unix());
        directoryOutput = fs.getPath("/output");
        outputPath0 = directoryOutput.resolve(BASE_NAME + ".properties");
        outputPath1 = directoryOutput.resolve(BASE_NAME + "_en_US.properties");

        Files.createDirectory(directoryOutput);
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
        final Path directoryA = fs.getPath("/input_a");
        final Path directoryB = fs.getPath("/input_b");
        Files.createDirectory(directoryA);
        Files.createDirectory(directoryB);


        final Path inputPathA0 = directoryA.resolve(BASE_NAME_A + ".properties");
        final Path inputPathA1 = directoryA.resolve(BASE_NAME_A + "_en_US.properties");
        final Path inputPathB0 = directoryB.resolve(BASE_NAME_B + ".properties");

        writeToFile(inputPathA0, INPUT_A_0);
        writeToFile(inputPathA1, INPUT_A_1);
        writeToFile(inputPathB0, INPUT_B_0);


        val localizationGenerator = new LocalizationGenerator(directoryOutput, BASE_NAME);
        localizationGenerator.addResources(directoryA, BASE_NAME_A);
        localizationGenerator.addResources(directoryB, BASE_NAME_B);

        System.out.println("Reading from file: " + outputPath0);
        Assertions.assertEquals(OUTPUT_0, LocalizationUtil.readFile(Files.newInputStream(outputPath0)));
        Assertions.assertEquals(OUTPUT_1, LocalizationUtil.readFile(Files.newInputStream(outputPath1)));
    }

    private static void writeToFile(@NotNull Path file, @NotNull List<String> lines)
        throws IOException
    {
        LocalizationUtil.ensureFileExists(file);
        LocalizationUtil.appendToFile(file, lines);
    }
}
