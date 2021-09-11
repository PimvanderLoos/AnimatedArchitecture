package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
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

class SetNameTest
{
    @Mock
    private IPPlayer commandSender;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetName.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
        initCommandSenderPermissions(commandSender, true, true);

        final IPLogger logger = new BasicPLogger();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetName(Mockito.any(ICommandSender.class), Mockito.anyString()))
               .thenAnswer(invoc -> new SetName(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                                invoc.getArgument(1, String.class), toolUserManager, handler));
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        final var uuid = UUID.randomUUID();
        final var name = "newDoor";

        final var toolUser = Mockito.mock(Creator.class);
        Mockito.when(toolUser.handleInput(name)).thenReturn(true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertTrue(factory.newSetName(commandSender, name).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser).handleInput(name);
    }

    @Test
    @SneakyThrows
    void testIncorrectToolUser()
    {
        final var uuid = UUID.randomUUID();
        final var name = "newDoor";

        final var toolUser = Mockito.mock(ToolUser.class);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertTrue(factory.newSetName(commandSender, name).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser, Mockito.never()).handleInput(name);
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        Assertions.assertTrue(factory.newSetName(Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS), "newDoor")
                                     .run().get(1, TimeUnit.SECONDS));
    }
}
