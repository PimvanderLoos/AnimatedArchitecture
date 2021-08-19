package nl.pim16aap2.bigdoors.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import static nl.pim16aap2.bigdoors.localization.LocalizationTestingUtilities.writeEntry;
import static nl.pim16aap2.bigdoors.localization.LocalizationTestingUtilities.writeToFile;

class LocalizationManagerIntegrationTest
{
    private FileSystem fs;
    private Path directoryOutput;

    @BeforeEach
    void init()
        throws IOException
    {
        final IBigDoorsPlatform platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());

        fs = Jimfs.newFileSystem(Configuration.unix());
        directoryOutput = Files.createDirectory(fs.getPath("/output"));
    }

    @AfterEach
    void cleanup()
        throws IOException
    {
        fs.close();
    }

    @Test
    void testAvoidPatching()
        throws IOException
    {
        final String baseName = "Translation";
        final IConfigLoader configLoader = Mockito.mock(IConfigLoader.class);
        Mockito.when(configLoader.locale()).thenReturn(Locale.ROOT);

        final Path outputBundle = directoryOutput.resolve(baseName + ".bundle");
        final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(outputBundle));
        writeEntry(outputStream, baseName + ".properties", List.of("key0=value0", "key1=value1", "key2=value2"));
        outputStream.close();

        final LocalizationManager localizationManager =
            new LocalizationManager(Mockito.mock(IRestartableHolder.class), directoryOutput,
                                    baseName, configLoader, -1);

        localizationManager.restart();

        // There aren't any patch files, so the patch generator should not be created
        // either on startup or during a restart.
        Assertions.assertFalse(localizationManager.isPatched());
    }

    @Test
    void testPatching()
        throws IOException
    {
        final String baseName = "Translation";
        final IConfigLoader configLoader = Mockito.mock(IConfigLoader.class);
        Mockito.when(configLoader.locale()).thenReturn(Locale.ROOT);

        final List<String> baseItems = List.of("key0=value0", "key1=value1", "key2=value2",
                                               "key3=value3", "key4=value4", "key5=value5");
        final List<String> patchItems = List.of("key0=", "key3=remapped", "key6=value6");

        final Path outputBundle = directoryOutput.resolve(baseName + ".bundle");

        final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(outputBundle));
        writeEntry(outputStream, baseName + ".properties", baseItems);
        outputStream.close();

        final LocalizationManager localizationManager =
            new LocalizationManager(Mockito.mock(IRestartableHolder.class), directoryOutput,
                                    baseName, configLoader, -1);

        Assertions.assertEquals("value0", localizationManager.getLocalizer().getMessage("key0"));
        Assertions.assertEquals("value3", localizationManager.getLocalizer().getMessage("key3"));
        Assertions.assertEquals(Localizer.KEY_NOT_FOUND_MESSAGE + "key6",
                                localizationManager.getLocalizer().getMessage("key6"));

        writeToFile(directoryOutput.resolve(baseName + ".properties"), patchItems);

        // Restarting the manager will apply all patches.
        localizationManager.restart();

        Assertions.assertEquals("value0", localizationManager.getLocalizer().getMessage("key0"));
        Assertions.assertEquals("remapped", localizationManager.getLocalizer().getMessage("key3"));
        Assertions.assertEquals("value6", localizationManager.getLocalizer().getMessage("key6"));
    }
}
