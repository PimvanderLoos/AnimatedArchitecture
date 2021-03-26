package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.util.PLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

class DoorTypeLoaderTest
{
    @Test
    public void test()
        throws IOException
    {
        final String extensionsPath =
            new File(".").getCanonicalPath().replace("bigdoors-integration-test", "bigdoors-doors/DoorTypes");
        final int inputCount = new File(extensionsPath).list().length;

        PLogger.get().setConsoleLogLevel(Level.OFF);
        Assertions.assertEquals(inputCount, DoorTypeLoader.get().loadDoorTypesFromDirectory(extensionsPath).size());
    }
}
