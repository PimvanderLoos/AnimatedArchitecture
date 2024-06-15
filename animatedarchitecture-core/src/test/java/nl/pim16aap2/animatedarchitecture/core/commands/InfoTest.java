package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InfoTest
{
    @Mock
    private AbstractStructure structure;

    private StructureRetriever structureRetriever;

    @Mock
    private HighlightedBlockSpawner glowingBlockSpawner;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Info.IFactory factory;

    @BeforeEach
    void init()
    {
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);
        Mockito.when(structure.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);

        final StructureSnapshot snapshot = Mockito.mock(StructureSnapshot.class, InvocationOnMock::callRealMethod);
        Mockito.when(structure.getSnapshot()).thenReturn(snapshot);

        Mockito.when(snapshot.getCuboid()).thenReturn(new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6)));
        Mockito.when(snapshot.getPowerBlock()).thenReturn(new Vector3Di(7, 8, 9));
        Mockito.when(snapshot.getNameAndUid()).thenReturn("Structure (0)");
        Mockito.when(snapshot.getOpenDir()).thenReturn(MovementDirection.NORTH);
        Mockito.doReturn(Optional.empty()).when(snapshot).getProperty(Mockito.anyString());

        final StructureType structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");
        Mockito.when(structure.getType()).thenReturn(structureType);
        Mockito.when(snapshot.getType()).thenReturn(structureType);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newInfo(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class)))
            .thenAnswer(invoc -> new Info(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                glowingBlockSpawner)
            );
    }

    @Test
    void testServer()
    {
        final IServer server = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);

        Assertions.assertDoesNotThrow(() -> factory.newInfo(server, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
            .spawnHighlightedBlocks(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testPlayer()
    {
        final IPlayer player = Mockito.mock(IPlayer.class, Answers.CALLS_REAL_METHODS);

        CommandTestingUtil.initCommandSenderPermissions(player, true, false);
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
            .spawnHighlightedBlocks(Mockito.any(StructureSnapshot.class), Mockito.any(IPlayer.class), Mockito.any());

        CommandTestingUtil.initCommandSenderPermissions(player, true, true);
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner)
            .spawnHighlightedBlocks(Mockito.any(StructureSnapshot.class), Mockito.any(IPlayer.class), Mockito.any());

        CommandTestingUtil.initCommandSenderPermissions(player, true, false);
        Mockito.when(structure.getOwner(player)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.times(2))
            .spawnHighlightedBlocks(Mockito.any(StructureSnapshot.class), Mockito.any(IPlayer.class), Mockito.any());
    }

}
