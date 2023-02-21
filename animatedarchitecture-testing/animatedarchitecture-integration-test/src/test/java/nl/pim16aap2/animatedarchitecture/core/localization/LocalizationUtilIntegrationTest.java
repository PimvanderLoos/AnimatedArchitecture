package nl.pim16aap2.animatedarchitecture.core.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


class LocalizationUtilIntegrationTest
{
    private FileSystem fs;

    @BeforeEach
    void init()
    {
        fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    void cleanup()
        throws IOException
    {
        fs.close();
    }

    @Test
    void testGetLocalesInDirectory()
        throws IOException
    {
        final Path zipFile = fs.getPath("./test.jar");
        final String base = "translation";
        LocalizationTestingUtilities.addFilesToZip(zipFile,
                                                   base + ".properties",
                                                   base + "_en_us.properties",
                                                   base + "_en_us_random.properties",
                                                   base + "_nl.properties",
                                                   base + "_nl_NL.properties");

        final List<Locale> locales = LocalizationUtil.getLocalesInZip(zipFile, base);
        Assertions.assertEquals(5, locales.size());
        Assertions.assertTrue(locales.contains(Locale.ROOT));
        Assertions.assertTrue(locales.contains(Locale.US));
        Assertions.assertTrue(locales.contains(
            new Locale.Builder().setLanguage("en").setRegion("US").setVariant("random").build()));
        Assertions.assertTrue(locales.contains(new Locale.Builder().setLanguage("nl").build()));
        Assertions.assertTrue(locales.contains(new Locale.Builder().setLanguage("nl").setRegion("NL").build()));
    }
}
