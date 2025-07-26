package nl.pim16aap2.animatedarchitecture.core.localization;

import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.testing.annotations.FileSystemTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import static nl.pim16aap2.animatedarchitecture.core.localization.LocalizationTestingUtilities.writeEntry;
import static nl.pim16aap2.animatedarchitecture.core.localization.LocalizationTestingUtilities.writeToFile;

@ExtendWith(MockitoExtension.class)
class LocalizationManagerIntegrationTest
{
    @FileSystemTest
    void testAvoidPatching(Path rootDir)
        throws IOException
    {
        final String baseName = "Translation";
        final IConfig config = Mockito.mock(IConfig.class);
        Mockito.when(config.locale()).thenReturn(Locale.ROOT);

        final Path outputBundle = rootDir.resolve(baseName + ".bundle");
        final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(outputBundle));
        writeEntry(outputStream, baseName + ".properties", List.of("key0=value0", "key1=value1", "key2=value2"));
        outputStream.close();

        final LocalizationManager localizationManager = new LocalizationManager(
            Mockito.mock(RestartableHolder.class),
            rootDir,
            baseName,
            config,
            false
        );

        localizationManager.shutDown();
        localizationManager.initialize();

        // There aren't any patch files, so the patch generator should not be created
        // either on startup or during a restart.
        Assertions.assertFalse(localizationManager.isPatched());
    }

    @FileSystemTest
    void testPatching(Path rootDir)
        throws IOException
    {
        final String baseName = "Translation";
        final IConfig config = Mockito.mock(IConfig.class);
        Mockito.when(config.locale()).thenReturn(Locale.ROOT);

        final List<String> baseItems = List.of(
            "key0=value0",
            "key1=value1",
            "key2=value2",
            "key3=value3",
            "key4=value4",
            "key5=value5"
        );
        final List<String> patchItems = List.of("key0=", "key3=remapped", "key6=value6");

        final Path outputBundle = rootDir.resolve(baseName + ".bundle");

        final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(outputBundle));
        writeEntry(outputStream, baseName + ".properties", baseItems);
        outputStream.close();

        final LocalizationManager localizationManager = new LocalizationManager(
            Mockito.mock(RestartableHolder.class),
            rootDir,
            baseName,
            config,
            false
        );

        Assertions.assertEquals("value0", localizationManager.getLocalizer().getMessage("key0"));
        Assertions.assertEquals("value3", localizationManager.getLocalizer().getMessage("key3"));
        Assertions.assertEquals(
            Localizer.KEY_NOT_FOUND_MESSAGE + "key6",
            localizationManager.getLocalizer().getMessage("key6")
        );

        writeToFile(rootDir.resolve(baseName + ".properties"), patchItems);

        // Restarting the manager will apply all patches.
        localizationManager.shutDown();
        localizationManager.initialize();

        Assertions.assertEquals("value0", localizationManager.getLocalizer().getMessage("key0"));
        Assertions.assertEquals("remapped", localizationManager.getLocalizer().getMessage("key3"));
        Assertions.assertEquals("value6", localizationManager.getLocalizer().getMessage("key6"));
    }
}
