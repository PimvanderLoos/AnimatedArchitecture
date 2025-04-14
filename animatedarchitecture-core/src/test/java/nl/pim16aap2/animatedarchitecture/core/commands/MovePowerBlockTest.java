package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.tooluser.PowerBlockRelocator;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyInt;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovePowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    private StructureRetriever structureRetriever;

    @Mock
    private Structure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovePowerBlock.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final UUID uuid = UUID.randomUUID();

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        structureRetriever = StructureRetrieverFactory.ofStructure(door);
        when(door.isOwner(uuid, StructureAttribute.RELOCATE_POWERBLOCK.getPermissionLevel())).thenReturn(true);
        when(door.isOwner(any(IPlayer.class), any())).thenReturn(true);
        when(commandSender.getUUID()).thenReturn(uuid);
        when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final PowerBlockRelocator.IFactory powerBlockRelocatorFactory = mock(PowerBlockRelocator.IFactory.class);
        when(powerBlockRelocatorFactory.create(Mockito.any(), any())).thenReturn(mock(PowerBlockRelocator.class));

        when(factory
            .newMovePowerBlock(any(ICommandSender.class), any(StructureRetriever.class)))
            .thenAnswer(invoc -> new MovePowerBlock(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                executor,
                toolUserManager,
                powerBlockRelocatorFactory)
            );
    }

    @Test
    void testServer()
    {
        final IServer server = mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(
            () -> factory.newMovePowerBlock(server, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager, never()).startToolUser(Mockito.any(), anyInt());
    }

    @Test
    void testExecution()
    {
        Assertions.assertDoesNotThrow(
            () -> factory.newMovePowerBlock(commandSender, structureRetriever).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager).startToolUser(Mockito.any(), anyInt());
    }
}
