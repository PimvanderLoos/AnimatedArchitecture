package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
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

class CancelTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private DoorSpecificationManager doorSpecificationManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Cancel.IFactory factory;

    @Mock
    private ToolUserManager toolUserManager;

    @BeforeEach
    void init()
    {
        final UUID uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);

        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final IPLogger logger = new BasicPLogger();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newCancel(Mockito.any(ICommandSender.class)))
               .thenAnswer(invoc -> new Cancel(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                               toolUserManager, doorSpecificationManager, handler));
    }

    @Test
    @SneakyThrows
    void test()
    {
        Assertions.assertTrue(factory.newCancel(commandSender).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser).shutdown();
        Mockito.verify(doorSpecificationManager).cancelRequest(commandSender);
    }
}
