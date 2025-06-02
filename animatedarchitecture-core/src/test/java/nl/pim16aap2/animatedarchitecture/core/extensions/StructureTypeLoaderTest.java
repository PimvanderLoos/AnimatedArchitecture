package nl.pim16aap2.animatedarchitecture.core.extensions;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.testing.assertions.AssertionBuilder;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.semver4j.Semver;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@Timeout(1)
@WithLogCapture
class StructureTypeLoaderTest
{
    @Test
    void logPreloadCheckResults_shouldLogResults(LogCaptor logCaptor)
    {
        // Setup
        logCaptor.disableConsoleOutput();
        logCaptor.setLogLevelToInfo();

        final var infoPass = getDefaultStructureTypeInfo("type-pass");
        final var infoAlreadyLoaded = getDefaultStructureTypeInfo("type-already-loaded");
        final var infoApiVersionNotSupported = getDefaultStructureTypeInfo("type-api-version-not-supported");

        final var map = Map.of(
            StructureTypeLoader.PreloadCheckResult.PASS, List.of(infoPass),
            StructureTypeLoader.PreloadCheckResult.ALREADY_LOADED, List.of(infoAlreadyLoaded),
            StructureTypeLoader.PreloadCheckResult.API_VERSION_NOT_SUPPORTED, List.of(infoApiVersionNotSupported)
        );

        // Execute
        StructureTypeLoader.logPreloadCheckResults(map);

        // Verify
        AssertionBuilder
            .assertLogged(logCaptor)
            .message("Loading structure type: %s", infoPass.getFullKey())
            .atInfo()
            .assertLogged();

        AssertionBuilder
            .assertLogged(logCaptor)
            .message("Structure type '%s' is already loaded, skipping.", infoAlreadyLoaded.getFullKey())
            .atInfo()
            .assertLogged();

        AssertionBuilder
            .assertLogged(logCaptor)
            .message(
                "Current API version '%s' out of the supported range '%s' for structure type: '%s'",
                StructureTypeLoader.CURRENT_EXTENSION_API_VERSION,
                infoApiVersionNotSupported.getSupportedApiVersions(),
                infoApiVersionNotSupported.getFullKey())
            .atSevere()
            .assertLogged();
    }

    // This test kind of tests the library code, and only a subset of its functionality.
    // However, the test is still here to make sure that at least the functionality that
    // is officially supported by our extension API is working as expected, even if/when
    // we make changes to how we use the library.
    @Test
    void testIsSupported()
    {
        final var apiVersion = Semver.of(1, 2, 3);

        for (final String allowed : new String[]{
            "=1.2.3",
            ">=1.2.3",
            "1.0.0 - 1.2.3",
            "1.2.3 - 1.2.4",
            "1.x",
            "1.2.x",
        })
        {
            Assertions.assertTrue(
                StructureTypeLoader.isSupported(apiVersion, allowed),
                "Version range '" + allowed + "' should be supported."
            );
        }

        //noinspection DataFlowIssue
        for (final String disallowed : new String[]{
            "",
            null,
            "1.2.4",
            "1.2.2",
            "1.3.0",
            "2.x",
            "2.0.0",
            ">=2.0.0",
        })
        {
            Assertions.assertFalse(
                StructureTypeLoader.isSupported(apiVersion, disallowed),
                "Version range '" + disallowed + "' should not be supported."
            );
        }
    }

    @Test
    void testPerformPreloadCheck()
    {
        final String namespace = Constants.PLUGIN_NAME;
        final Path jarFile = Paths.get("/does/not/exist.jar");

        final String alreadyLoadedTypeName = "already-loaded-type";
        final String unloadedTypeName = "not-loaded-type";

        final Semver apiVersion = Semver.of(1, 0, 0);

        final var alreadyLoadedTypes = new HashSet<String>();

        final var typeInfo = new StructureTypeInfo(
            new NamespacedKey(namespace, alreadyLoadedTypeName),
            1,
            "com.example.MainClass",
            jarFile,
            "1.2.3",
            null
        );

        alreadyLoadedTypes.add(typeInfo.getFullKey());

        Assertions.assertEquals(
            StructureTypeLoader.PreloadCheckResult.ALREADY_LOADED,
            StructureTypeLoader.performPreloadCheck(apiVersion, alreadyLoadedTypes, typeInfo)
        );

        final var typeInfo2 = new StructureTypeInfo(
            new NamespacedKey(namespace, unloadedTypeName),
            1,
            "com.example.MainClass",
            jarFile,
            "2.0.0",
            null
        );

        Assertions.assertEquals(
            StructureTypeLoader.PreloadCheckResult.API_VERSION_NOT_SUPPORTED,
            StructureTypeLoader.performPreloadCheck(apiVersion, alreadyLoadedTypes, typeInfo2)
        );

        final var typeInfo3 = new StructureTypeInfo(
            new NamespacedKey(namespace, unloadedTypeName),
            1,
            "com.example.MainClass",
            jarFile,
            "1.*",
            null
        );

        Assertions.assertEquals(
            StructureTypeLoader.PreloadCheckResult.PASS,
            StructureTypeLoader.performPreloadCheck(apiVersion, alreadyLoadedTypes, typeInfo3)
        );
    }

    @Test
    void testGetStructureTypeInfo()
    {
        final String namespace = Constants.PLUGIN_NAME;
        final String name = "TypeName";
        final var key = new NamespacedKey(namespace, name);

        final var manifest = new Manifest();
        final var attributes = new Attributes();
        attributes.putValue("Namespace", namespace);
        attributes.putValue("TypeName", name);
        attributes.putValue("Version", "1");
        attributes.putValue("SupportedApiVersions", "1.2.3");
        attributes.putValue("TypeDependencies", namespace + ":door(1;2)");
        manifest.getEntries().put("EntryTitle", attributes);
        manifest.getMainAttributes().putValue(Attributes.Name.MAIN_CLASS.toString(), "MainClass");

        final var file = Paths.get("/does/not/exist.jar");
        final var structureTypeInfoOpt = StructureTypeLoader.getStructureTypeInfo("EntryTitle", file, manifest);

        Assertions.assertTrue(structureTypeInfoOpt.isSuccessful());
        final var structureTypeInfo = structureTypeInfoOpt.structureTypeInfo();
        Assertions.assertNotNull(structureTypeInfo);

        Assertions.assertEquals(key, structureTypeInfo.getNamespacedKey());
        Assertions.assertEquals(1, structureTypeInfo.getVersion());
        Assertions.assertEquals("MainClass", structureTypeInfo.getMainClass());
        Assertions.assertEquals(file, structureTypeInfo.getJarFile());
        Assertions.assertEquals("1.2.3", structureTypeInfo.getSupportedApiVersions());
        Assertions.assertEquals(
            List.of(new StructureTypeInfo.Dependency("door", 1, 2)),
            structureTypeInfo.getDependencies()
        );

        Assertions.assertEquals(
            StructureTypeLoader.StructureTypeInfoParseResult.NO_ATTRIBUTES,
            StructureTypeLoader.getStructureTypeInfo("EntryTitle", file, new Manifest()).parseResult()
        );
    }

    @Nested
    @ParameterizedClass
    @MethodSource("fileSystemConfigurationProvider")
    class FileSystemTests
    {
        @Parameter
        @SuppressWarnings("unused")
        private Configuration configuration;

        private FileSystem fs;

        @BeforeEach
        void init()
        {
            fs = Jimfs.newFileSystem(configuration);
        }

        @AfterEach
        void cleanup()
            throws IOException
        {
            fs.close();
        }

        @Test
        void ensureDirectoryExists_shouldReturnFalseAndLogMessageIfDirectoryAlreadyExistsAsFile(LogCaptor logCaptor)
            throws IOException
        {
            // Setup
            logCaptor.disableConsoleOutput();
            logCaptor.setLogLevelToInfo();

            final String fileName = "file.txt";

            final Path path = fs.getPath(fileName);
            Files.createFile(path);

            assertThat(path).exists().isRegularFile();

            // Execute
            final boolean result = StructureTypeLoader.ensureDirectoryExists(path);

            // Verify
            assertThat(result).isFalse();

            AssertionBuilder
                .assertLogged(logCaptor)
                .atSevere()
                .message("Failed to create directory: %s", path)
                .assertLoggedExceptionOfType(FileAlreadyExistsException.class)
                .hasMessage(fileName);
        }

        @Test
        void ensureDirectoryExists_shouldCreateDirectoriesAndReturnTrueIfDirectoryDoesNotExist()
        {
            // Setup
            final Path path = fs.getPath("test", "deep", "directory");

            // Execute
            final boolean result = StructureTypeLoader.ensureDirectoryExists(path);

            // Verify
            assertThat(result).isTrue();
            assertThat(path).exists().isDirectory();
        }

        static Stream<Configuration> fileSystemConfigurationProvider()
        {
            return Stream.of(
                Configuration.unix(),
                Configuration.windows(),
                Configuration.osX()
            );
        }
    }

    private static StructureTypeInfo getDefaultStructureTypeInfo(String name)
    {
        return new StructureTypeInfo(
            new NamespacedKey(Constants.PLUGIN_NAME, name),
            1,
            "com.example.MainClass",
            Paths.get("/does/not/exist.jar"),
            "1.2.3",
            null
        );
    }
}
