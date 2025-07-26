package nl.pim16aap2.animatedarchitecture.core.localization;

import nl.pim16aap2.animatedarchitecture.core.util.FileUtil;
import nl.pim16aap2.testing.annotations.FileSystemTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class LocalizationPatcherIntegrationTest
{
    @FileSystemTest
    void testUpdateRootKeys(Path rootDirectory)
        throws IOException
    {
        final Path directoryOutput = Files.createDirectories(rootDirectory.resolve("output"));

        final Path file0 = FileUtil.ensureFileExists(directoryOutput.resolve("patch.properties"));
        FileUtil.appendToFile(file0, List.of("key0=aaa", "key3=baa", "key1=aba", "key2=aab"));

        final Path file1 = FileUtil.ensureFileExists(directoryOutput.resolve("patch_en_US.properties"));
        FileUtil.appendToFile(file1, List.of("key10=aaa", "key13=baa", "key12=aab", "key11=aba"));

        final LocalizationPatcher patcher = new LocalizationPatcher(directoryOutput, "patch");
        patcher.updatePatchKeys(List.of("key1", "key5", "key4"));

        Assertions.assertArrayEquals(
            new Object[]{"key0", "key1", "key2", "key3", "key4", "key5"},
            LocalizationUtil.getKeySet(file0).toArray()
        );

        Assertions.assertArrayEquals(
            new Object[]{"key1", "key10", "key11", "key12", "key13", "key4", "key5"},
            LocalizationUtil.getKeySet(file1).toArray()
        );
    }

    @FileSystemTest
    void testGetPatches(Path rootDirectory)
        throws IOException
    {
        final Path directoryOutput = Files.createDirectories(rootDirectory.resolve("output"));

        final Path file = FileUtil.ensureFileExists(directoryOutput.resolve("patch.properties"));
        FileUtil.appendToFile(file, List.of("key0=", "key1= ", "key2=aab", "key3=baa"));

        final LocalizationPatcher patcher = new LocalizationPatcher(directoryOutput, "patch");
        final Map<String, String> patches = patcher.getPatches(new LocaleFile(file, ""));

        final String[] patchedLines = patches.values().toArray(new String[0]);
        Assertions.assertArrayEquals(new String[]{"key1= ", "key2=aab", "key3=baa"}, patchedLines);
    }
}
