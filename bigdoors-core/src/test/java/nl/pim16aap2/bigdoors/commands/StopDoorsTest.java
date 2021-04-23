package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initPlatform;

class StopDoorsTest
{
    @Test
    @SneakyThrows
    void test()
    {
        val platform = initPlatform();
        val activityManager = Mockito.mock(DoorActivityManager.class);
        val commandSender = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);

        Mockito.when(platform.getDoorActivityManager()).thenReturn(activityManager);

        Assertions.assertTrue(StopDoors.run(commandSender).get(1, TimeUnit.SECONDS));
        Mockito.verify(activityManager).stopDoors();
    }
}
