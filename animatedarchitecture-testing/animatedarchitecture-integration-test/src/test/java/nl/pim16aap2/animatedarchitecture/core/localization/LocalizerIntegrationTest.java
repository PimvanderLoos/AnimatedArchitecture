package nl.pim16aap2.animatedarchitecture.core.localization;

import nl.pim16aap2.testing.annotations.FileSystemTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

@ExtendWith(MockitoExtension.class)
class LocalizerIntegrationTest
{
    private static final Locale LOCALE_DUTCH = new Locale.Builder().setLanguage("nl").setRegion("NL").build();
    private static final String BASE_NAME = "Translation";

    private Path directory;
    private Path bundle;

    void init(FileSystem fs)
        throws IOException
    {
        directory = Files.createDirectories(fs.getPath("./test_output/translations"));
        bundle = directory.resolve(BASE_NAME + ".bundle");

        final ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(bundle));
        String baseFileContents = """
            key0=value0
            key1=value1
            key2=value2
            """;

        LocalizationTestingUtilities.writeEntry(
            zipOutputStream,
            BASE_NAME + ".properties",
            baseFileContents.getBytes(StandardCharsets.UTF_8)
        );

        String dutchFileContents = """
            key0=waarde0
            key1={0}
            """;

        LocalizationTestingUtilities.writeEntry(
            zipOutputStream,
            BASE_NAME + "_nl_NL.properties",
            dutchFileContents.getBytes(StandardCharsets.UTF_8)
        );

        zipOutputStream.close();
    }

    @FileSystemTest
    void testGetMessage(FileSystem fs)
        throws IOException
    {
        init(fs);
        final Localizer localizer = new Localizer(directory, BASE_NAME, false);
        localizer.allowClientLocales(true);
        Assertions.assertEquals("waarde0", localizer.getMessage("key0", LOCALE_DUTCH));
        final String input = "A_B_C_D_E";
        Assertions.assertEquals(input, localizer.getMessage("key1", LOCALE_DUTCH, input));
        Assertions.assertEquals("value1", localizer.getMessage("key1", input));
        Assertions.assertEquals("value2", localizer.getMessage("key2", LOCALE_DUTCH, input));
    }

    @FileSystemTest
    void testAppendingMessages(FileSystem fs)
        throws Exception
    {
        init(fs);

        final Localizer localizer = new Localizer(directory, BASE_NAME, false);
        // Just ensure that it's loaded properly.
        Assertions.assertEquals("value0", localizer.getMessage("key0"));
        // Ensure that the key doesn't exist (yet!).
        Assertions.assertEquals(Localizer.KEY_NOT_FOUND_MESSAGE + "key3", localizer.getMessage("key3"));

        localizer.shutdown();
        Assertions.assertEquals(Localizer.KEY_NOT_FOUND_MESSAGE + "key0", localizer.getMessage("key0"));

        final String value3 = "THIS WAS JUST ADDED! DID ANYONE SEE THAT?";
        LocalizationTestingUtilities.appendToFileInZip(bundle, BASE_NAME + ".properties", "key3=" + value3);

        localizer.reInit();
        Assertions.assertEquals("value0", localizer.getMessage("key0"));
        Assertions.assertEquals(value3, localizer.getMessage("key3"));
    }
}
