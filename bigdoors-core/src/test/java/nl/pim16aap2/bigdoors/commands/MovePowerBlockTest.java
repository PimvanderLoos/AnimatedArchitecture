package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initDoorRetriever;

class MovePowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @BeforeEach
    void init()
    {
        final var platform = initPlatform();
        final var uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(platform.getBigDoorsToolUtil()).thenReturn(Mockito.mock(IBigDoorsToolUtil.class));
        Mockito.when(platform.getToolUserManager()).thenReturn(toolUserManager);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final var server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(MovePowerBlock.run(server, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        Assertions.assertTrue(MovePowerBlock.run(commandSender, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(Mockito.any(), Mockito.anyInt());
    }
}
