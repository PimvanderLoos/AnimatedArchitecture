package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.doorOwner0;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class InfoTest
{
    @Mock
    private AbstractDoor door;

    private DoorRetriever.AbstractRetriever doorRetriever;

    @Mock
    private IGlowingBlockSpawner glowingBlockSpawner;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Info.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        doorRetriever = DoorRetriever.ofDoor(door);
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        final IPLogger logger = new BasicPLogger();
        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);

        Mockito.when(factory.newInfo(Mockito.any(ICommandSender.class),
                                     Mockito.any(DoorRetriever.AbstractRetriever.class)))
               .thenAnswer(invoc -> new Info(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                             invoc.getArgument(1, DoorRetriever.AbstractRetriever.class),
                                             glowingBlockSpawner, handler));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final var server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newInfo(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never()).spawnGlowingBlocks(Mockito.any(), Mockito.any());
        Mockito.verify(server).sendMessage(door.toString());
    }

    @Test
    @SneakyThrows
    void testPlayer()
    {
        final IPPlayer player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);
        final String doorString = door.toString();

        initCommandSenderPermissions(player, true, false);
        Assertions.assertTrue(factory.newInfo(player, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never()).spawnGlowingBlocks(Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.never()).sendMessage(doorString);

        initCommandSenderPermissions(player, true, true);
        Assertions.assertTrue(factory.newInfo(player, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner).spawnGlowingBlocks(Mockito.any(), Mockito.any());
        Mockito.verify(player).sendMessage(doorString);

        initCommandSenderPermissions(player, true, false);
        Mockito.when(door.getDoorOwner(player)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertTrue(factory.newInfo(player, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.times(2)).spawnGlowingBlocks(Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.times(2)).sendMessage(doorString);
    }

}
