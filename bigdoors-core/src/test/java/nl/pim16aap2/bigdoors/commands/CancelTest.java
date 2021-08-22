package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
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

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class CancelTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private DoorSpecificationManager doorSpecificationManager;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        val uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);

        val toolUserManager = Mockito.mock(ToolUserManager.class);
        Mockito.when(platform.getToolUserManager()).thenReturn(toolUserManager);
        Mockito.when(platform.getDoorSpecificationManager()).thenReturn(doorSpecificationManager);

        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));
    }

    @Test
    @SneakyThrows
    void test()
    {
        Assertions.assertTrue(Cancel.run(commandSender).get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser).shutdown();
        Mockito.verify(doorSpecificationManager).cancelRequest(commandSender);
    }
}
