package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class InfoTest
{
    @Mock
    private AbstractDoorBase door;

    @Mock
    private DoorRetriever doorRetriever;

    private IBigDoorsPlatform platform;

    @BeforeEach
    void init()
    {
        platform = UnitTestUtil.initPlatform();
        MockitoAnnotations.openMocks(this);

        initDoorRetriever(doorRetriever, door);

        Mockito.when(platform.getGlowingBlockSpawner()).thenReturn(Optional.empty());
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        val server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(Info.run(server, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(platform, Mockito.never()).getGlowingBlockSpawner();
        Mockito.verify(server).sendMessage(door.toString());
    }

    @Test
    @SneakyThrows
    void testPlayer()
    {
        Mockito.when(door.getDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.empty());

        val player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);
        val doorString = door.toString();

        initCommandSenderPermissions(player, true, false);
        Assertions.assertTrue(Info.run(player, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(platform, Mockito.never()).getGlowingBlockSpawner();
        Mockito.verify(player, Mockito.never()).sendMessage(doorString);

        initCommandSenderPermissions(player, true, true);
        Assertions.assertTrue(Info.run(player, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(platform).getGlowingBlockSpawner();
        Mockito.verify(player).sendMessage(doorString);

        initCommandSenderPermissions(player, true, false);
        Mockito.when(door.getDoorOwner(player)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertTrue(Info.run(player, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(platform, Mockito.times(2)).getGlowingBlockSpawner();
        Mockito.verify(player, Mockito.times(2)).sendMessage(doorString);
    }

}
