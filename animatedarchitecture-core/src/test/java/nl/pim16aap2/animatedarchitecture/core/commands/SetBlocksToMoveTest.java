package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetBlocksToMoveTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetBlocksToMove.IFactory factory;

    @Mock
    private StructureType structureType;

    @BeforeEach
    void init()
    {
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetBlocksToMove(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class),
                Mockito.anyInt()))
            .thenAnswer(invoc -> new SetBlocksToMove(
                invoc.getArgument(0, ICommandSender.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, Integer.class))
            );
    }

    @Test
    void testSetBlocksToMove()
    {
        final Structure structure = newStructure(Property.BLOCKS_TO_MOVE);

        final int blocksToMove = 42;

        final SetBlocksToMove command = factory.newSetBlocksToMove(
            commandSender,
            StructureRetrieverFactory.ofStructure(structure),
            blocksToMove
        );
        UnitTestUtil.setField(StructureTargetCommand.class, command, "retrieverResult", structure);

        Assertions.assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));

        Mockito.verify(structure).setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove);
        Mockito.verify(structure).syncData();
    }

    @Test
    void testNoUpdate()
    {
        final Structure structure = newStructure(Property.BLOCKS_TO_MOVE);

        final int blocksToMove = 84;
        structure.setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove);

        final SetBlocksToMove command = factory.newSetBlocksToMove(
            commandSender,
            StructureRetrieverFactory.ofStructure(structure),
            blocksToMove
        );

        Assertions.assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));

        Mockito.verify(structure, Mockito.times(2)).setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove);
        Mockito.verify(structure, Mockito.never()).syncData();
    }

    private Structure newStructure(Property<?>... properties)
    {
        final Structure structure = Mockito.mock(Structure.class);
        UnitTestUtil.setPropertyContainerInMockedStructure(structure, properties);

        Mockito.when(structure.syncData())
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(structure.getType()).thenReturn(structureType);

        UnitTestUtil.setPropertyContainerInMockedStructure(structure, properties);

        return structure;
    }
}
