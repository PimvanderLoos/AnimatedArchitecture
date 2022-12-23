package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class MovePowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovePowerBlock.IFactory factory;

    @BeforeEach
    void init()
    {
        final UUID uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = DoorRetrieverFactory.ofDoor(door);
        Mockito.when(door.isDoorOwner(uuid)).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        final PowerBlockRelocator.IFactory powerBlockRelocatorFactory =
            Mockito.mock(PowerBlockRelocator.IFactory.class);
        Mockito.when(powerBlockRelocatorFactory.create(Mockito.any(), Mockito.any()))
               .thenReturn(Mockito.mock(PowerBlockRelocator.class));

        Mockito.when(factory.newMovePowerBlock(Mockito.any(ICommandSender.class),
                                               Mockito.any(DoorRetriever.class)))
               .thenAnswer(invoc -> new MovePowerBlock(invoc.getArgument(0, ICommandSender.class), localizer,
                                                       ITextFactory.getSimpleTextFactory(),
                                                       invoc.getArgument(1, DoorRetriever.class),
                                                       toolUserManager, powerBlockRelocatorFactory));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newMovePowerBlock(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void testExecution()
        throws Exception
    {
        Assertions.assertTrue(factory.newMovePowerBlock(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(Mockito.any(), Mockito.anyInt());
    }
}
