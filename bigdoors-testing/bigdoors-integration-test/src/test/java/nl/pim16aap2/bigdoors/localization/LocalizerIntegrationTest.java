package nl.pim16aap2.bigdoors.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import static nl.pim16aap2.bigdoors.localization.LocalizationTestingUtilities.appendToFileInZip;
import static nl.pim16aap2.bigdoors.localization.LocalizationTestingUtilities.writeEntry;


class LocalizerIntegrationTest
{
    private static final Locale LOCALE_DUTCH = new Locale.Builder().setLanguage("nl").setRegion("NL").build();
    private static final String BASE_NAME = "Translation";

    private FileSystem fs;
    private Path directory;
    private Path bundle;

    private void initFileSystem()
        throws IOException
    {
        fs = Jimfs.newFileSystem(Configuration.unix());
        directory = Files.createDirectories(fs.getPath("./test_output/translations"));
        bundle = directory.resolve(BASE_NAME + ".bundle");
    }

    @BeforeEach
    void init()
        throws IOException
    {
        initFileSystem();

        final ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(bundle));
        String baseFileContents = """
                                  key0=value0
                                  key1=value1
                                  key2=value2
                                  """;
        writeEntry(zipOutputStream, BASE_NAME + ".properties", baseFileContents.getBytes());
        String dutchFileContents = """
                                   key0=waarde0
                                   key1={0}
                                   """;
        writeEntry(zipOutputStream, BASE_NAME + "_nl_NL.properties", dutchFileContents.getBytes());
        zipOutputStream.close();
    }

    @AfterEach
    void cleanup()
        throws IOException
    {
        fs.close();
    }

    @Test
    void testGetMessage()
    {
        final Localizer localizer = new Localizer(directory, BASE_NAME, false);
        Assertions.assertEquals("waarde0", localizer.getMessage("key0", LOCALE_DUTCH));
        final String input = "A_B_C_D_E";
        Assertions.assertEquals(input, localizer.getMessage("key1", LOCALE_DUTCH, input));
        Assertions.assertEquals("value1", localizer.getMessage("key1", input));
        Assertions.assertEquals("value2", localizer.getMessage("key2", LOCALE_DUTCH, input));
    }

    @Test
    void testAppendingMessages()
        throws IOException, URISyntaxException
    {
        final Localizer localizer = new Localizer(directory, BASE_NAME, false);
        // Just ensure that it's loaded properly.
        Assertions.assertEquals("value0", localizer.getMessage("key0"));
        // Ensure that the key doesn't exist (yet!).
        Assertions.assertEquals(Localizer.KEY_NOT_FOUND_MESSAGE + "key3", localizer.getMessage("key3"));

        localizer.shutdown();
        Assertions.assertEquals(Localizer.KEY_NOT_FOUND_MESSAGE + "key0", localizer.getMessage("key0"));

        final String value3 = "THIS WAS JUST ADDED! DID ANYONE SEE THAT?";
        appendToFileInZip(bundle, BASE_NAME + ".properties", "key3=" + value3);

        localizer.reInit();
        Assertions.assertEquals("value0", localizer.getMessage("key0"));
        Assertions.assertEquals(value3, localizer.getMessage("key3"));
    }
}
