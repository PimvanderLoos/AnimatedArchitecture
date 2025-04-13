package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    private StructureRetriever structureRetriever;

    @Mock
    private Structure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Delete.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final StructureType doorType = mock(StructureType.class);
        when(doorType.getLocalizationKey()).thenReturn("DoorType");
        when(door.getType()).thenReturn(doorType);

        when(door.isOwner(any(UUID.class), any())).thenReturn(true);
        when(door.isOwner(any(IPlayer.class), any())).thenReturn(true);
        structureRetriever = StructureRetrieverFactory.ofStructure(door);

        when(databaseManager
            .deleteStructure(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        when(factory
            .newDelete(any(ICommandSender.class), any(StructureRetriever.class)))
            .thenAnswer(invoc -> new Delete(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                executor,
                databaseManager)
            );
    }

    @Test
    void testServer()
    {
        final IServer server = mock(IServer.class, Answers.CALLS_REAL_METHODS);
        assertDoesNotThrow(() -> factory.newDelete(server, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(databaseManager).deleteStructure(door, null);
    }

    @Test
    void testExecution()
    {
        // No permissions, so not allowed.
        CommandTestingUtil.initCommandSenderPermissions(commandSender, false, false);
        UnitTestUtil.initMessageable(commandSender);
        assertDoesNotThrow(
            () -> factory.newDelete(commandSender, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(databaseManager, never()).deleteStructure(door, commandSender);

        // Has user permission, but not an owner, so not allowed.
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, false);
        assertDoesNotThrow(
            () -> factory.newDelete(commandSender, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(databaseManager, never()).deleteStructure(door, commandSender);

        // Has user permission, and is owner, so allowed.
        when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        assertDoesNotThrow(
            () -> factory.newDelete(commandSender, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(databaseManager, times(1)).deleteStructure(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        when(door.getOwner(commandSender)).thenReturn(Optional.empty());
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        assertDoesNotThrow(
            () -> factory.newDelete(commandSender, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(databaseManager, times(2)).deleteStructure(door, commandSender);
    }
}
