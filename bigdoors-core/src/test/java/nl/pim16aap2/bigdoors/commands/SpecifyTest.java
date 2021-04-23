package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;

class SpecifyTest
{
    @Mock
    private IPPlayer commandSender;

    @Mock
    private DoorSpecificationManager doorSpecificationManager;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(platform.getDoorSpecificationManager()).thenReturn(doorSpecificationManager);
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        val server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(Specify.run(server, "newDoor").get(1, TimeUnit.SECONDS));
        Mockito.verify(doorSpecificationManager, Mockito.never()).handleInput(Mockito.any(), Mockito.any());
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        Mockito.when(doorSpecificationManager.handleInput(Mockito.any(), Mockito.any())).thenReturn(true);
        val input = "newDoor";
        Assertions.assertTrue(Specify.run(commandSender, input).get(1, TimeUnit.SECONDS));
        Mockito.verify(doorSpecificationManager).handleInput(commandSender, input);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any());

        // Test again, but now the command sender is not an active tool user.
        Mockito.when(doorSpecificationManager.handleInput(Mockito.any(), Mockito.any())).thenReturn(false);
        Assertions.assertTrue(Specify.run(commandSender, input).get(1, TimeUnit.SECONDS));
        Mockito.verify(doorSpecificationManager, Mockito.times(2)).handleInput(commandSender, input);
        Mockito.verify(commandSender).sendMessage(Mockito.any());
    }
}
