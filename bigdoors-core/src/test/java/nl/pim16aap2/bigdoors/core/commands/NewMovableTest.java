package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.util.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

@Timeout(1)
class NewStructureTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private StructureType doorType;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NewStructure.IFactory factory;

    @Mock
    javax.inject.Provider<Creator.Context> creatorContextProvider;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newNewStructure(Mockito.any(ICommandSender.class), Mockito.any(StructureType.class),
                                             Mockito.any()))
               .thenAnswer(invoc -> new NewStructure(invoc.getArgument(0, ICommandSender.class), localizer,
                                                     ITextFactory.getSimpleTextFactory(),
                                                     invoc.getArgument(1, StructureType.class),
                                                     invoc.getArgument(2, String.class),
                                                     toolUserManager, creatorContextProvider));
    }

    @Test
    void testServer()
    {
        final IServer server = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(
            () -> factory.newNewStructure(server, doorType, null).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void testExecution()
    {
        final String name = "newDoor";

        final Creator unnamedCreator = Mockito.mock(Creator.class);
        final Creator namedCreator = Mockito.mock(Creator.class);

        Mockito.when(doorType.getCreator(Mockito.any(), Mockito.any(), Mockito.any()))
               .thenAnswer(inv -> name.equals(inv.getArgument(2, String.class)) ? namedCreator : unnamedCreator);

        Assertions.assertDoesNotThrow(
            () -> factory.newNewStructure(commandSender, doorType).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(unnamedCreator, Constants.STRUCTURE_CREATOR_TIME_LIMIT);

        Assertions.assertDoesNotThrow(
            () -> factory.newNewStructure(commandSender, doorType, name).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(namedCreator, Constants.STRUCTURE_CREATOR_TIME_LIMIT);
    }
}
