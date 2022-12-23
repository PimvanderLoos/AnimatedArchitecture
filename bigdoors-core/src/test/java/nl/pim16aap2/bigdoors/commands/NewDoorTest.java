package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
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

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NewDoor.IFactory factory;

    @Mock
    javax.inject.Provider<Creator.Context> creatorContextProvider;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newNewDoor(Mockito.any(ICommandSender.class), Mockito.any(DoorType.class),
                                        Mockito.any()))
               .thenAnswer(invoc -> new NewDoor(invoc.getArgument(0, ICommandSender.class), localizer,
                                                ITextFactory.getSimpleTextFactory(),
                                                invoc.getArgument(1, DoorType.class),
                                                invoc.getArgument(2, String.class),
                                                toolUserManager, creatorContextProvider));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newNewDoor(server, doorType, null).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void testExecution()
        throws Exception
    {
        final String name = "newDoor";

        final Creator unnamedCreator = Mockito.mock(Creator.class);
        final Creator namedCreator = Mockito.mock(Creator.class);

        Mockito.when(doorType.getCreator(Mockito.any(), Mockito.any(), Mockito.any()))
               .thenAnswer(inv -> name.equals(inv.getArgument(2, String.class)) ? namedCreator : unnamedCreator);

        Assertions.assertTrue(factory.newNewDoor(commandSender, doorType).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(unnamedCreator, Constants.DOOR_CREATOR_TIME_LIMIT);

        Assertions.assertTrue(factory.newNewDoor(commandSender, doorType, name).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(namedCreator, Constants.DOOR_CREATOR_TIME_LIMIT);
    }
}
