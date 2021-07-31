package nl.pim16aap2.bigdoors.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class LocalizationGeneratorIntegrationTest
{
    private static final @NotNull FileSystem FS = Jimfs.newFileSystem(Configuration.unix());

    private static final @NotNull String BASE_NAME = "Translation";
    private static final @NotNull String BASE_NAME_A = "Translation_A";
    private static final @NotNull String BASE_NAME_B = "Translation_B";
    private static final @NotNull Path DIRECTORY_OUTPUT = FS.getPath("/output");
    private static final @NotNull Path DIRECTORY_A = FS.getPath("/input_a");
    private static final @NotNull Path DIRECTORY_B = FS.getPath("/input_b");
    private static final @NotNull Path INPUT_PATH_A_0 = DIRECTORY_A.resolve(BASE_NAME_A + ".properties");
    private static final @NotNull Path INPUT_PATH_A_1 = DIRECTORY_A.resolve(BASE_NAME_A + "_en_US.properties");
    private static final @NotNull Path INPUT_PATH_B_0 = DIRECTORY_B.resolve(BASE_NAME_B + ".properties");
    private static final @NotNull Path OUTPUT_PATH_0 = DIRECTORY_OUTPUT.resolve(BASE_NAME + ".properties");
    private static final @NotNull Path OUTPUT_PATH_1 = DIRECTORY_OUTPUT.resolve(BASE_NAME + "_en_US.properties");

    private static final @NotNull List<String> INPUT_A_0 = new ArrayList<>(5);
    private static final @NotNull List<String> INPUT_A_1 = new ArrayList<>(5);
    private static final @NotNull List<String> INPUT_B_0 = new ArrayList<>(5);
    private static final @NotNull List<String> OUTPUT_0 = new ArrayList<>(13);
    private static final @NotNull List<String> OUTPUT_1 = new ArrayList<>(13);

    static
    {
        INPUT_A_0.add("a_key0=val0");
        INPUT_A_0.add("a_key1=val1");
        INPUT_A_0.add("a_key2=val2");
        INPUT_A_0.add("a_key3=val3");
        INPUT_A_0.add("a_key4=val4");

        INPUT_A_1.add("b_key0=val0");
        INPUT_A_1.add("b_key1=val1");
        INPUT_A_1.add("b_key2=val2");
        INPUT_A_1.add("b_key3=val3");
        INPUT_A_1.add("b_key4=val4");

        INPUT_B_0.add("a_key3=val100");
        INPUT_B_0.add("a_key4=val101");
        INPUT_B_0.add("a_key5=val102");
        INPUT_B_0.add("a_key6=val103");
        INPUT_B_0.add("a_key7=val104");

        // From INPUT_A_0
        OUTPUT_0.add("a_key0=val0");
        OUTPUT_0.add("a_key1=val1");
        OUTPUT_0.add("a_key2=val2");
        OUTPUT_0.add("a_key3=val3");
        OUTPUT_0.add("a_key4=val4");

        // From INPUT_B_0
        OUTPUT_0.add("a_key5=val102");
        OUTPUT_0.add("a_key6=val103");
        OUTPUT_0.add("a_key7=val104");

        // From INPUT_A_1
        OUTPUT_1.add("b_key0=val0");
        OUTPUT_1.add("b_key1=val1");
        OUTPUT_1.add("b_key2=val2");
        OUTPUT_1.add("b_key3=val3");
        OUTPUT_1.add("b_key4=val4");
    }

    @BeforeAll
    static void init()
        throws IOException
    {
        Files.createDirectory(DIRECTORY_OUTPUT);
        Files.createDirectory(DIRECTORY_A);
        Files.createDirectory(DIRECTORY_B);

        writeToFile(INPUT_PATH_A_0, INPUT_A_0);
        writeToFile(INPUT_PATH_A_1, INPUT_A_1);
        writeToFile(INPUT_PATH_B_0, INPUT_B_0);
    }

    @Test
    void testAddResources()
        throws IOException
    {
        val localizationGenerator = new LocalizationGenerator(DIRECTORY_OUTPUT, BASE_NAME);
        localizationGenerator.addResources(DIRECTORY_A, BASE_NAME_A);
        localizationGenerator.addResources(DIRECTORY_B, BASE_NAME_B);

        System.out.println("Reading from file: " + OUTPUT_PATH_0);
        Assertions.assertEquals(OUTPUT_0, LocalizationUtil.readFile(Files.newInputStream(OUTPUT_PATH_0)));
        Assertions.assertEquals(OUTPUT_1, LocalizationUtil.readFile(Files.newInputStream(OUTPUT_PATH_1)));
    }

    private static void writeToFile(@NotNull Path file, @NotNull List<String> lines)
        throws IOException
    {
        LocalizationUtil.ensureFileExists(file);
        LocalizationUtil.appendToFile(file, lines);
    }
}
