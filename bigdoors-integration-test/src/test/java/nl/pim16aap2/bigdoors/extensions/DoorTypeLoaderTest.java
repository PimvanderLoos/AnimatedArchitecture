package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

class DoorTypeLoaderTest
{
    @Mock
    private RestartableHolder restartableHolder;

    @Mock
    private DoorTypeManager doorTypeManager;

    @BeforeEach
    public void init()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test()
        throws IOException
    {
        final String extensionsPath =
            new File(".").getCanonicalPath()
                         .replace("bigdoors-integration-test", "bigdoors-doors" + File.separator + "DoorTypes");
        final int inputCount = Objects.requireNonNull(new File(extensionsPath).list()).length;

        Assertions.assertEquals(inputCount,
                                new DoorTypeLoader(restartableHolder, doorTypeManager, Path.of(""))
                                    .loadDoorTypesFromDirectory(Path.of(extensionsPath)).size());
    }
}
