package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
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

@Timeout(1)
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
        MockitoAnnotations.openMocks(this);

        structureRetriever = StructureRetrieverFactory.ofStructure(structure);
        Mockito.when(structure.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPlayer.class))).thenReturn(true);
        Mockito.when(structure.getCuboid()).thenReturn(new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6)));
        Mockito.when(structure.getNameAndUid()).thenReturn("Structure (0)");
        Mockito.when(structure.getOpenDir()).thenReturn(MovementDirection.NORTH);

        final StructureType structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");
        Mockito.when(structure.getType()).thenReturn(structureType);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newInfo(Mockito.any(ICommandSender.class),
                                     Mockito.any(StructureRetriever.class)))
               .thenAnswer(invoc -> new Info(invoc.getArgument(0, ICommandSender.class), localizer,
                                             ITextFactory.getSimpleTextFactory(),
                                             invoc.getArgument(1, StructureRetriever.class),
                                             glowingBlockSpawner));
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
               .spawnHighlightedBlocks(Mockito.any(), Mockito.any(), Mockito.any());

        CommandTestingUtil.initCommandSenderPermissions(player, true, true);
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner).spawnHighlightedBlocks(Mockito.any(), Mockito.any(), Mockito.any());

        CommandTestingUtil.initCommandSenderPermissions(player, true, false);
        Mockito.when(structure.getOwner(player)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.times(2))
               .spawnHighlightedBlocks(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
