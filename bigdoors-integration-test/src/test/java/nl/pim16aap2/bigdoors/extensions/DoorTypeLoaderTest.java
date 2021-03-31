package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

class DoorTypeLoaderTest
{
    private IBigDoorsPlatform platform;

    @BeforeEach
    public void init()
    {
        platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        Mockito.when(platform.getDoorTypeManager()).thenReturn(Mockito.mock(DoorTypeManager.class));
    }

    @Test
    public void test()
        throws IOException
    {
        final String extensionsPath =
            new File(".").getCanonicalPath().replace("bigdoors-integration-test", "bigdoors-doors/DoorTypes");
        final int inputCount = new File(extensionsPath).list().length;

//        BigDoors.get().getPLogger().setConsoleLogLevel(Level.OFF);
        Assertions.assertEquals(inputCount, DoorTypeLoader.get().loadDoorTypesFromDirectory(extensionsPath).size());
    }
}
