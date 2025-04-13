package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
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

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetOpenDirectionTest
{
    @Mock
    private Structure structure;

    @Mock
    private StructureType structureType;

    private StructureRetriever structureRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetOpenDirection.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        when(structure.syncData()).thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        when(structure.getType()).thenReturn(structureType);

        initCommandSenderPermissions(commandSender, true, true);
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        when(factory
            .newSetOpenDirection(
                any(ICommandSender.class),
                any(StructureRetriever.class),
                any(MovementDirection.class),
                anyBoolean()))
            .thenAnswer(invoc -> new SetOpenDirection(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, MovementDirection.class),
                invoc.getArgument(3, Boolean.class),
                executor,
                mock(CommandFactory.class))
            );
    }

    @Test
    void testOpenDirValidity()
    {
        final MovementDirection movementDirection = MovementDirection.CLOCKWISE;
        UnitTestUtil.initMessageable(commandSender);

        when(structureType.isValidOpenDirection(any())).thenReturn(false);
        final var command = factory.newSetOpenDirection(commandSender, structureRetriever, movementDirection);

        assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));
        verify(structure, never()).syncData();
        verify(structure, never()).setOpenDirection(movementDirection);

        when(structureType.isValidOpenDirection(movementDirection)).thenReturn(true);
        assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));
        verify(structure).setOpenDirection(movementDirection);
        verify(structure).syncData();
    }
}
