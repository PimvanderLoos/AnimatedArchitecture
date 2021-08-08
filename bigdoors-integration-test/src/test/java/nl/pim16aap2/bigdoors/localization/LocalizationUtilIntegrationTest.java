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
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        val zipFile = fs.getPath("./test.jar");
        val base = "translation";
        addFilesToZip(zipFile,
                      base + ".properties",
                      base + "_en_us.properties",
                      base + "_en_us_some_random_variation.properties",
                      base + "_nl.properties",
                      base + "_nl_NL.properties");

        val locales = LocalizationUtil.getLocalesInZip(zipFile, base);
        Assertions.assertEquals(5, locales.size());
        Assertions.assertTrue(locales.contains(Locale.ROOT));
        Assertions.assertTrue(locales.contains(Locale.US));
        Assertions.assertTrue(locales.contains(new Locale("en", "US", "some_random_variation")));
        Assertions.assertTrue(locales.contains(new Locale("nl")));
        Assertions.assertTrue(locales.contains(new Locale("nl", "NL")));
    }

    private void addFilesToZip(@NotNull Path zipFile, @NotNull String... names)
        throws IOException
    {
        val outputStream = new ZipOutputStream(Files.newOutputStream(zipFile));
        for (val name : names)
        {
            val data = "".getBytes();
            outputStream.putNextEntry(new ZipEntry(name));
            outputStream.write(data, 0, data.length);
            outputStream.closeEntry();
        }
        outputStream.close();
    }
}
