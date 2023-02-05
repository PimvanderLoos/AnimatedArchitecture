package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.core.text.Text;
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
class SpecifyTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private StructureSpecificationManager structureSpecificationManager;

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
                                                invoc.getArgument(1, String.class), structureSpecificationManager));
    }

    @Test
    void testServer()
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(() -> factory.newSpecify(server, "newDoor").run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureSpecificationManager, Mockito.never()).handleInput(Mockito.any(), Mockito.any());
    }

    @Test
    void testExecution()
    {
        Mockito.when(structureSpecificationManager.handleInput(Mockito.any(), Mockito.any())).thenReturn(true);
        final String input = "newDoor";
        Assertions.assertDoesNotThrow(() -> factory.newSpecify(commandSender, input).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureSpecificationManager).handleInput(commandSender, input);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any(Text.class));

        // Test again, but now the command sender is not an active tool user.
        Mockito.when(structureSpecificationManager.handleInput(Mockito.any(), Mockito.any())).thenReturn(false);
        Assertions.assertDoesNotThrow(() -> factory.newSpecify(commandSender, input).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureSpecificationManager, Mockito.times(2)).handleInput(commandSender, input);
        Mockito.verify(commandSender).sendMessage(Mockito.any(Text.class));
    }
}
