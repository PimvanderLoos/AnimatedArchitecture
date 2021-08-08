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
        val dir = fs.getPath(".");
        val base = "translation";
        createFiles(dir,
                    base + ".properties",
                    base + "_en_us.properties",
                    base + "_en_us_some_random_variation.properties",
                    base + "_nl.properties",
                    base + "_nl_NL.properties");

        val locales = LocalizationUtil.getLocalesInDirectory(dir, base);
        Assertions.assertEquals(5, locales.size());
        Assertions.assertEquals(Locale.getDefault(), locales.get(0));
        Assertions.assertEquals(new Locale("en", "US"), locales.get(1));
        Assertions.assertEquals("some_random_variation", locales.get(2).getVariant());
        Assertions.assertEquals(new Locale("nl"), locales.get(3));
        Assertions.assertEquals(new Locale("nl", "NL"), locales.get(4));
    }

    private void createFiles(@NotNull Path dir, @NotNull String... names)
        throws IOException
    {
        for (val name : names)
            Files.createFile(dir.resolve(name));
    }
}
