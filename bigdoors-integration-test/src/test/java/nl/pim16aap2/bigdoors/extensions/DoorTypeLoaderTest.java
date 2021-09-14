package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

class DoorTypeLoaderTest
{
    private IPLogger logger;

    @Mock
    private IRestartableHolder restartableHolder;

    @Mock
    private DoorTypeManager doorTypeManager;

    @BeforeEach
    public void init()
    {
        MockitoAnnotations.openMocks(this);
        logger = new BasicPLogger();
    }

    @Test
    void test()
        throws IOException
    {
        final String extensionsPath =
            new File(".").getCanonicalPath().replace("bigdoors-integration-test", "bigdoors-doors/DoorTypes");
        final int inputCount = Objects.requireNonNull(new File(extensionsPath).list()).length;

        Assertions.assertEquals(inputCount,
                                new DoorTypeLoader(restartableHolder, logger, doorTypeManager, new File("."))
                                    .loadDoorTypesFromDirectory(extensionsPath).size());
    }
}
