package nl.pim16aap2.bigdoors.localization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
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
import java.util.Map;

class LocalizationPatcherIntegrationTest
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
    void testUpdateRootKeys()
        throws IOException
    {
        final Path file0 = LocalizationUtil.ensureFileExists(directoryOutput.resolve("patch.properties"));
        LocalizationUtil.appendToFile(file0, List.of("key0=aaa", "key1=aba", "key2=aab", "key3=baa"));

        final Path file1 = LocalizationUtil.ensureFileExists(directoryOutput.resolve("patch_en_US.properties"));
        LocalizationUtil.appendToFile(file1, List.of("key10=aaa", "key11=aba", "key12=aab", "key13=baa"));

        final LocalizationPatcher patcher = new LocalizationPatcher(directoryOutput, "patch");
        patcher.updatePatchKeys(List.of("key1", "key4", "key5"));

        Assertions.assertArrayEquals(new Object[]{"key0", "key1", "key2", "key3", "key4", "key5"},
                                     LocalizationUtil.getKeySet(file0).toArray());

        Assertions.assertArrayEquals(new Object[]{"key10", "key11", "key12", "key13", "key1", "key4", "key5"},
                                     LocalizationUtil.getKeySet(file1).toArray());
    }

    @Test
    void testGetPatches()
        throws IOException
    {
        final Path file = LocalizationUtil.ensureFileExists(directoryOutput.resolve("patch.properties"));
        LocalizationUtil.appendToFile(file, List.of("key0=", "key1= ", "key2=aab", "key3=baa"));

        final LocalizationPatcher patcher = new LocalizationPatcher(directoryOutput, "patch");
        final Map<String, String> patches = patcher.getPatches("");

        Assertions.assertNull(patches.get("key0"));
        Assertions.assertEquals(" ", patches.get("key1"));
        Assertions.assertEquals("aab", patches.get("key2"));
        Assertions.assertEquals("baa", patches.get("key3"));
    }
}
