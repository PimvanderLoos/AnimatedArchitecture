package nl.pim16aap2.animatedarchitecture.extensions;

import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.extensions.StructureTypeLoader;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

class StructureTypeLoaderTest
{
    @Mock
    private RestartableHolder restartableHolder;

    @Mock
    private StructureTypeManager structureTypeManager;

    @Mock
    private IConfig config;

    private AutoCloseable mocks;

    @BeforeEach
    public void init()
    {
        mocks = MockitoAnnotations.openMocks(this);
        Mockito.when(config.debug()).thenReturn(true);
    }

    @AfterEach
    void cleanup()
        throws Exception
    {
        mocks.close();
    }

    @Test
    void test()
    {
        final Path basePath = Path.of("");
        final String extensionsPath =
            basePath
                .toAbsolutePath()
                .getParent()
                .getParent()
                .resolve("structures")
                .resolve("StructuresOutput")
                .toAbsolutePath()
                .toString();

        final int inputCount = Objects.requireNonNull(new File(extensionsPath).list()).length;

        final var structureTypeLoader = new StructureTypeLoader(
            restartableHolder,
            structureTypeManager,
            config,
            basePath
        );

        structureTypeLoader.initialize();

        Assertions.assertEquals(
            inputCount,
            structureTypeLoader.loadStructureTypesFromDirectory(Path.of(extensionsPath)).size()
        );
    }
}
