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
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.MissingResourceException;


class LocalizerIntegrationTest
{
    private static final @NotNull Locale LOCALE_DUTCH = new Locale("nl", "NL");
    private static final @NotNull String BASE_NAME = "Translation";

    private FileSystem fs;
    private Path directory;
    private Path baseFile;
    private Path dutchFile;

    private void initFileSystem()
        throws IOException
    {
        fs = Jimfs.newFileSystem(Configuration.unix());
        directory = Files.createDirectory(fs.getPath("/translations"));
        baseFile = fs.getPath("/translations/" + BASE_NAME + ".properties");
        dutchFile = fs.getPath("/translations/" + BASE_NAME + "_nl_NL.properties");
    }

    @BeforeEach
    void init()
        throws IOException
    {
        initFileSystem();
        Files.write(baseFile,
                    """
                    key0=value0
                    key1=value1
                    key2=value2
                    """.getBytes());
        Files.write(dutchFile,
                    """
                    key0=waarde0
                    key1={0}
                    """.getBytes());
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
        val localizer = new Localizer(directory, BASE_NAME);
        Assertions.assertEquals("waarde0", localizer.getMessage("key0", LOCALE_DUTCH));
        val input = "ABCDE";
        Assertions.assertEquals(input, localizer.getMessage("key1", LOCALE_DUTCH, input));
        Assertions.assertEquals("value1", localizer.getMessage("key1", input));
        Assertions.assertEquals("value2", localizer.getMessage("key2", LOCALE_DUTCH, input));
    }

    @Test
    void testAppendingMessages()
        throws IOException
    {
        val localizer = new Localizer(directory, BASE_NAME);
        // Just ensure that it's loaded properly.
        Assertions.assertEquals("value0", localizer.getMessage("key0"));
        // Ensure that the key doesn't exist (yet!).
        Assertions.assertThrows(MissingResourceException.class, () -> localizer.getMessage("key3"));

        localizer.shutdown();
        Assertions.assertEquals(Localizer.KEY_NOT_FOUND_MESSAGE + "key0", localizer.getMessage("key0"));

        val value3 = "THIS WAS JUST ADDED! DID ANYONE SEE THAT?";
        Files.write(baseFile, ("key3=" + value3).getBytes(), StandardOpenOption.APPEND);

        localizer.restart();
        Assertions.assertEquals("value0", localizer.getMessage("key0"));
        Assertions.assertEquals(value3, localizer.getMessage("key3"));
    }
}
