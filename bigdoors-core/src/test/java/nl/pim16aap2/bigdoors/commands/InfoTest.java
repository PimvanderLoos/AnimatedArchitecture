package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.structureOwnerCreator;

@Timeout(1)
class InfoTest
{
    @Mock
    private AbstractStructure structure;

    private StructureRetriever structureRetriever;

    @Mock
    private GlowingBlockSpawner glowingBlockSpawner;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Info.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        structureRetriever = StructureRetrieverFactory.ofStructure(structure);
        Mockito.when(structure.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        Mockito.when(structure.getCuboid()).thenReturn(new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6)));
        Mockito.when(structure.getNameAndUid()).thenReturn("Structure (0)");

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
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);

        Assertions.assertDoesNotThrow(() -> factory.newInfo(server, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testPlayer()
    {
        final IPPlayer player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);

        initCommandSenderPermissions(player, true, false);
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.never())
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());

        initCommandSenderPermissions(player, true, true);
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner).spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());

        initCommandSenderPermissions(player, true, false);
        Mockito.when(structure.getOwner(player)).thenReturn(Optional.of(structureOwnerCreator));
        Assertions.assertDoesNotThrow(() -> factory.newInfo(player, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(glowingBlockSpawner, Mockito.times(2))
               .spawnGlowingBlocks(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
