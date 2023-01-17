package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
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
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.movableOwnerCreator;

class InfoTest
{
    @Mock
    private AbstractMovable movable;

    private MovableRetriever movableRetriever;

    @Mock
    private GlowingBlockSpawner glowingBlockSpawner;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Info.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        movableRetriever = MovableRetrieverFactory.ofMovable(movable);
        Mockito.when(movable.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(movable.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        final MovableType movableType = Mockito.mock(MovableType.class);
        Mockito.when(movableType.getLocalizationKey()).thenReturn("MovableType");
        Mockito.when(movable.getMovableType()).thenReturn(movableType);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newInfo(Mockito.any(ICommandSender.class),
                                     Mockito.any(MovableRetriever.class)))
               .thenAnswer(invoc -> new Info(invoc.getArgument(0, ICommandSender.class), localizer,
                                             ITextFactory.getSimpleTextFactory(),
                                             invoc.getArgument(1, MovableRetriever.class),
                                             glowingBlockSpawner));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);

        Assertions.assertTrue(factory.newInfo(server, movableRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(server).sendMessage(UnitTestUtil.toText(movable.toString()));
    }

    @Test
    void testPlayer()
        throws Exception
    {
        final IPPlayer player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);

        final String movableString = movable.toString();

        initCommandSenderPermissions(player, true, false);
        Assertions.assertTrue(factory.newInfo(player, movableRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.never()).sendMessage(UnitTestUtil.toText(movableString));

        initCommandSenderPermissions(player, true, true);
        Assertions.assertTrue(factory.newInfo(player, movableRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner).spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(player).sendMessage(UnitTestUtil.toText(movableString));

        initCommandSenderPermissions(player, true, false);
        Mockito.when(movable.getOwner(player)).thenReturn(Optional.of(movableOwnerCreator));
        Assertions.assertTrue(factory.newInfo(player, movableRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.times(2))
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.times(2)).sendMessage(UnitTestUtil.toText(movableString));
    }

}
