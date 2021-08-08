package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
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

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class SetNameTest
{
    @Mock
    private IPPlayer commandSender;

    @Mock
    private ToolUserManager toolUserManager;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        MockitoAnnotations.openMocks(this);
        initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(platform.getToolUserManager()).thenReturn(toolUserManager);
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        val uuid = UUID.randomUUID();
        val name = "newDoor";

        val toolUser = Mockito.mock(Creator.class);
        Mockito.when(toolUser.handleInput(name)).thenReturn(true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertTrue(SetName.run(commandSender, name).get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser).handleInput(name);
    }

    @Test
    @SneakyThrows
    void testIncorrectToolUser()
    {
        val uuid = UUID.randomUUID();
        val name = "newDoor";

        val toolUser = Mockito.mock(ToolUser.class);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertTrue(SetName.run(commandSender, name).get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser, Mockito.never()).handleInput(name);
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        Assertions.assertTrue(SetName.run(Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS), "newDoor")
                                     .get(1, TimeUnit.SECONDS));
    }
}
