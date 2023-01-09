package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.doorOwnerCreator;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class InfoTest
{
    @Mock
    private AbstractDoor door;

    private DoorRetriever doorRetriever;

    @Mock
    private GlowingBlockSpawner glowingBlockSpawner;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Info.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        doorRetriever = DoorRetrieverFactory.ofDoor(door);
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        final DoorType doorType = Mockito.mock(DoorType.class);
        Mockito.when(doorType.getLocalizationKey()).thenReturn("DoorType");
        Mockito.when(door.getDoorType()).thenReturn(doorType);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newInfo(Mockito.any(ICommandSender.class),
                                     Mockito.any(DoorRetriever.class)))
               .thenAnswer(invoc -> new Info(invoc.getArgument(0, ICommandSender.class), localizer,
                                             ITextFactory.getSimpleTextFactory(),
                                             invoc.getArgument(1, DoorRetriever.class),
                                             glowingBlockSpawner));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);

        Assertions.assertTrue(factory.newInfo(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(server).sendMessage(UnitTestUtil.toText(door.toString()));
    }

    @Test
    void testPlayer()
        throws Exception
    {
        final IPPlayer player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);

        final String doorString = door.toString();

        initCommandSenderPermissions(player, true, false);
        Assertions.assertTrue(factory.newInfo(player, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.never()).sendMessage(UnitTestUtil.toText(doorString));

        initCommandSenderPermissions(player, true, true);
        Assertions.assertTrue(factory.newInfo(player, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner).spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(player).sendMessage(UnitTestUtil.toText(doorString));

        initCommandSenderPermissions(player, true, false);
        Mockito.when(door.getDoorOwner(player)).thenReturn(Optional.of(doorOwnerCreator));
        Assertions.assertTrue(factory.newInfo(player, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.times(2))
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.times(2)).sendMessage(UnitTestUtil.toText(doorString));
    }

}
