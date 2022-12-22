package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.text.Text;
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

class ConfirmTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Confirm.IFactory factory;

    private UUID uuid;

    @BeforeEach
    void init()
    {
        uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newConfirm(Mockito.any(ICommandSender.class)))
               .thenAnswer(invoc -> new Confirm(invoc.getArgument(0, ICommandSender.class),
                                                localizer, ITextFactory.getSimpleTextFactory(), toolUserManager));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        // Ensure the server running the method does not result in a ToolUser being started.
        Assertions.assertTrue(factory.newConfirm(Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS)).run()
                                     .get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).getToolUser(Mockito.any(UUID.class));
    }

    @Test
    @SneakyThrows
    void test()
    {
        Assertions.assertTrue(factory.newConfirm(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any(Text.class));

        Mockito.when(toolUserManager.getToolUser(Mockito.any(UUID.class))).thenReturn(Optional.empty());
        Assertions.assertTrue(factory.newConfirm(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.times(2)).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender).sendMessage(Mockito.any(Text.class));
    }
}
