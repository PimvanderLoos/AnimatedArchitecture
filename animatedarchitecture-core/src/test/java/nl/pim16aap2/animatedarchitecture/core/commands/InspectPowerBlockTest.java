package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.PowerBlockInspector;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Timeout(1)
class InspectPowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private InspectPowerBlock.IFactory factory;

    @BeforeEach
    void init()
    {
        final UUID uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        final PowerBlockInspector.IFactory inspectPowerBlockFactory = Mockito.mock(PowerBlockInspector.IFactory.class);

        Mockito.when(factory.newInspectPowerBlock(Mockito.any(ICommandSender.class)))
               .thenAnswer(invoc -> new InspectPowerBlock(invoc.getArgument(0, ICommandSender.class),
                                                          localizer, ITextFactory.getSimpleTextFactory(),
                                                          toolUserManager, inspectPowerBlockFactory));
    }

    @Test
    void testServer()
    {
        final IServer server = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(() -> factory.newInspectPowerBlock(server).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void testExecution()
    {
        Assertions.assertDoesNotThrow(() -> factory.newInspectPowerBlock(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(Mockito.any(), Mockito.anyInt());
    }
}
