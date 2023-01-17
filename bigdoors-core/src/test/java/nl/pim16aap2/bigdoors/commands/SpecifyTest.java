package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.text.Text;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;


class SpecifyTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private MovableSpecificationManager movableSpecificationManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Specify.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSpecify(Mockito.any(ICommandSender.class), Mockito.anyString()))
               .thenAnswer(invoc -> new Specify(invoc.getArgument(0, ICommandSender.class), localizer,
                                                ITextFactory.getSimpleTextFactory(),
                                                invoc.getArgument(1, String.class), movableSpecificationManager));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newSpecify(server, "newDoor").run().get(1, TimeUnit.SECONDS));
        Mockito.verify(movableSpecificationManager, Mockito.never()).handleInput(Mockito.any(), Mockito.any());
    }

    @Test
    void testExecution()
        throws Exception
    {
        Mockito.when(movableSpecificationManager.handleInput(Mockito.any(), Mockito.any())).thenReturn(true);
        final String input = "newDoor";
        Assertions.assertTrue(factory.newSpecify(commandSender, input).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(movableSpecificationManager).handleInput(commandSender, input);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any(Text.class));

        // Test again, but now the command sender is not an active tool user.
        Mockito.when(movableSpecificationManager.handleInput(Mockito.any(), Mockito.any())).thenReturn(false);
        Assertions.assertTrue(factory.newSpecify(commandSender, input).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(movableSpecificationManager, Mockito.times(2)).handleInput(commandSender, input);
        Mockito.verify(commandSender).sendMessage(Mockito.any(Text.class));
    }
}
