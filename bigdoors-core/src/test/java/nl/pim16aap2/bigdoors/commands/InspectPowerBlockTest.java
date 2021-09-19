package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockInspector;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
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

class InspectPowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

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

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final IPLogger logger = new BasicPLogger();
        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        final PowerBlockInspector.IFactory inspectPowerBlockFactory = Mockito.mock(PowerBlockInspector.IFactory.class);

        Mockito.when(factory.newInspectPowerBlock(Mockito.any(ICommandSender.class)))
               .thenAnswer(invoc -> new InspectPowerBlock(invoc.getArgument(0, ICommandSender.class),
                                                          logger, localizer, toolUserManager,
                                                          inspectPowerBlockFactory));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newInspectPowerBlock(server).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        Assertions.assertTrue(factory.newInspectPowerBlock(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(Mockito.any(), Mockito.anyInt());
    }
}
