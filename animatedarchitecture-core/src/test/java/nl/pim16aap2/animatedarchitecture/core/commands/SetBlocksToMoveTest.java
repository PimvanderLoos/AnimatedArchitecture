package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        when(structureType.getLocalizationKey()).thenReturn("StructureType");

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        when(factory
            .newSetBlocksToMove(any(ICommandSender.class), any(StructureRetriever.class), anyInt()))
            .thenAnswer(invoc -> new SetBlocksToMove(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, Integer.class),
                executor)
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

        assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));

        verify(structure).setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove);
        verify(structure).syncData();
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

        assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));

        verify(structure, times(2)).setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove);
        verify(structure, never()).syncData();
    }

    private Structure newStructure(Property<?>... properties)
    {
        final Structure structure = mock(Structure.class);
        UnitTestUtil.setPropertyContainerInMockedStructure(structure, properties);

        when(structure.syncData()).thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        when(structure.getType()).thenReturn(structureType);

        UnitTestUtil.setPropertyContainerInMockedStructure(structure, properties);

        return structure;
    }
}
