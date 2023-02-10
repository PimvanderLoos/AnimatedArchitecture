package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
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

import static nl.pim16aap2.bigdoors.core.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
class SetNameTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetName.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetName(Mockito.any(ICommandSender.class), Mockito.anyString()))
               .thenAnswer(invoc -> new SetName(invoc.getArgument(0, ICommandSender.class), localizer,
                                                ITextFactory.getSimpleTextFactory(),
                                                invoc.getArgument(1, String.class), toolUserManager));
    }

    @Test
    void testExecution()
    {
        final UUID uuid = UUID.randomUUID();
        final String name = "newDoor";

        final Creator toolUser = Mockito.mock(Creator.class);
        Mockito.when(toolUser.handleInput(name)).thenReturn(true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertDoesNotThrow(() -> factory.newSetName(commandSender, name).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser).handleInput(name);
    }

    @Test
    void testIncorrectToolUser()
    {
        final UUID uuid = UUID.randomUUID();
        final String name = "newDoor";

        final ToolUser toolUser = Mockito.mock(ToolUser.class);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertDoesNotThrow(() -> factory.newSetName(commandSender, name).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser, Mockito.never()).handleInput(name);
    }

    @Test
    void testServer()
    {
        Assertions.assertDoesNotThrow(
            () -> factory.newSetName(Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS), "newDoor")
                         .run().get(1, TimeUnit.SECONDS));
    }
}
