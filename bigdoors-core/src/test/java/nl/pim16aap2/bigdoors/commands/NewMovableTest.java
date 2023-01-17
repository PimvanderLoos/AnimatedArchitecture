package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
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

class NewMovableTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private MovableType doorType;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NewMovable.IFactory factory;

    @Mock
    javax.inject.Provider<Creator.Context> creatorContextProvider;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newNewMovable(Mockito.any(ICommandSender.class), Mockito.any(MovableType.class),
                                           Mockito.any()))
               .thenAnswer(invoc -> new NewMovable(invoc.getArgument(0, ICommandSender.class), localizer,
                                                   ITextFactory.getSimpleTextFactory(),
                                                   invoc.getArgument(1, MovableType.class),
                                                   invoc.getArgument(2, String.class),
                                                   toolUserManager, creatorContextProvider));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newNewMovable(server, doorType, null).run().get(1, TimeUnit.SECONDS));
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

        Assertions.assertTrue(factory.newNewMovable(commandSender, doorType).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(unnamedCreator, Constants.MOVABLE_CREATOR_TIME_LIMIT);

        Assertions.assertTrue(factory.newNewMovable(commandSender, doorType, name).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(namedCreator, Constants.MOVABLE_CREATOR_TIME_LIMIT);
    }
}
