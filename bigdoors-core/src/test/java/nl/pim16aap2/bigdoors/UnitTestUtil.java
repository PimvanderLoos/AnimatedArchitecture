package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import org.mockito.Mockito;

public class UnitTestUtil
{

    /**
     * Initializes and registers a new {@link IBigDoorsPlatform}. A {@link BasicPLogger} is also set up.
     *
     * @return The new {@link IBigDoorsPlatform}.
     */
    public static IBigDoorsPlatform initPlatform()
    {
        IBigDoorsPlatform platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        return platform;
    }
}
