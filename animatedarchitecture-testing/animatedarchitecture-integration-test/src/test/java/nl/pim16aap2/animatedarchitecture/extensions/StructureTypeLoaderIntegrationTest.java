package nl.pim16aap2.animatedarchitecture.extensions;

import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.extensions.StructureTypeLoader;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
// This class lives in the integration test module because it requires all the structure types to have been built.
class StructureTypeLoaderIntegrationTest
{
    @Mock
    private RestartableHolder restartableHolder;

    @Mock
    private StructureTypeManager structureTypeManager;

    @Mock
    private IConfig config;

    @BeforeEach
    public void init()
    {
        Mockito.when(config.debug()).thenReturn(true);
    }

    @Test
    void test()
    {
        final Path extensionsPath = Path.of("../../structures/StructuresOutput").toAbsolutePath();

        final int jarCount =
            Objects.requireNonNull(extensionsPath.toFile().list((dir, name) -> name.endsWith(".jar"))).length;

        Assertions.assertEquals(
            9,
            jarCount,
            "Expected 9 jar files, but found " + jarCount + " in directory " + extensionsPath.toAbsolutePath()
        );

        final var structureTypeLoader = new StructureTypeLoader(
            restartableHolder,
            structureTypeManager,
            config,
            extensionsPath
        );

        structureTypeLoader.initialize();

        Assertions.assertEquals(
            jarCount,
            structureTypeLoader.loadStructureTypesFromDirectory(extensionsPath).size()
        );
    }
}
