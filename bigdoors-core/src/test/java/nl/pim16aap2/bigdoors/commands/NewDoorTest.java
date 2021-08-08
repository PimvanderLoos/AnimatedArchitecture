package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class NewDoorTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private DoorType doorType;

    @Mock
    private ToolUserManager toolUserManager;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        Mockito.when(platform.getToolUserManager()).thenReturn(toolUserManager);

        initCommandSenderPermissions(commandSender, true, true);
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        val server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(NewDoor.run(server, doorType, null).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        val name = "newDoor";

        val unnamedCreator = Mockito.mock(Creator.class);
        val namedCreator = Mockito.mock(Creator.class);

        Mockito.when(doorType.getCreator(Mockito.any(), Mockito.any()))
               .thenAnswer(inv -> name.equals(inv.getArgument(1, String.class)) ? namedCreator : unnamedCreator);

        Assertions.assertTrue(NewDoor.run(commandSender, doorType).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(unnamedCreator, Constants.DOOR_CREATOR_TIME_LIMIT);

        Assertions.assertTrue(NewDoor.run(commandSender, doorType, name).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(namedCreator, Constants.DOOR_CREATOR_TIME_LIMIT);
    }
}
