package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
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
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final var server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(new NewDoor(server, doorType, null).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        final var name = "newDoor";

        final var unnamedCreator = Mockito.mock(Creator.class);
        final var namedCreator = Mockito.mock(Creator.class);

        Mockito.when(doorType.getCreator(Mockito.any(), Mockito.any()))
               .thenAnswer(inv -> name.equals(inv.getArgument(1, String.class)) ? namedCreator : unnamedCreator);

        Assertions.assertTrue(new NewDoor(commandSender, doorType).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(unnamedCreator, Constants.DOOR_CREATOR_TIME_LIMIT);

        Assertions.assertTrue(new NewDoor(commandSender, doorType, name).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(namedCreator, Constants.DOOR_CREATOR_TIME_LIMIT);
    }
}
