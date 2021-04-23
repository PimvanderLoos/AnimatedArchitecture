package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
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
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initPlatform;

class ConfirmTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    private UUID uuid;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);

        Mockito.when(platform.getToolUserManager()).thenReturn(toolUserManager);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        Assertions.assertTrue(Confirm.run(Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS))
                                     .get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).getToolUser(Mockito.any(UUID.class));
    }

    @Test
    @SneakyThrows
    void test()
    {
        Assertions.assertTrue(Confirm.run(commandSender).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any());

        Mockito.when(toolUserManager.getToolUser(Mockito.any(UUID.class))).thenReturn(Optional.empty());
        Assertions.assertTrue(Confirm.run(commandSender).get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.times(2)).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender).sendMessage(Mockito.any());
    }
}
