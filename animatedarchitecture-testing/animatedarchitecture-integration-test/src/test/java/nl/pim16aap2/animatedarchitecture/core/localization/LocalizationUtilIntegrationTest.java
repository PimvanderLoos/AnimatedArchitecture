package nl.pim16aap2.animatedarchitecture.core.localization;

import nl.pim16aap2.testing.annotations.FileSystemTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@ExtendWith(MockitoExtension.class)
class LocalizationUtilIntegrationTest
{
    @FileSystemTest
    void testGetLocalesInDirectory(Path rootDir)
        throws IOException
    {
        final Path zipFile = rootDir.resolve("test.jar");
        final String base = "translation";
        LocalizationTestingUtilities.addFilesToZip(
            zipFile,
            base + ".properties",
            base + "_en_us.properties",
            base + "_nl.properties",
            base + "_nl_NL.properties"
        );

        final List<Locale> locales = LocalizationUtil.getLocalesInZip(zipFile, base);
        Assertions.assertEquals(4, locales.size());
        Assertions.assertTrue(locales.contains(Locale.ROOT));
        Assertions.assertTrue(locales.contains(Locale.US));
        Assertions.assertTrue(locales.contains(new Locale.Builder().setLanguage("nl").build()));
        Assertions.assertTrue(locales.contains(new Locale.Builder().setLanguage("nl").setRegion("NL").build()));
    }
}
