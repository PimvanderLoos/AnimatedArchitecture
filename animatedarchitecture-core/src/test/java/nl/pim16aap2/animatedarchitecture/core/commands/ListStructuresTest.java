package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.MockInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class ListStructuresTest
{
    private List<Structure> structures;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer playerCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer serverCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ListStructures.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final int size = 3;
        structures = new ArrayList<>(size);
        for (int idx = 0; idx < size; ++idx)
            structures.add(Mockito.mock(Structure.class));

        when(factory
            .newListStructures(any(ICommandSender.class), any(StructureRetriever.class)))
            .thenAnswer(invoc -> new ListStructures(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                executor)
            );
    }

    @Test
    void run_shouldNotFindUnownedStructuresWithoutAdminPermission()
    {
        // Setup
        final var retriever = MockInjector.injectInto(StructureRetrieverFactory.class).ofStructures(structures);
        UnitTestUtil.initMessageable(playerCommandSender);
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, false);

        // Execute
        assertDoesNotThrow(
            () -> factory.newListStructures(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));

        // Verify
        assertThatMessageable(playerCommandSender)
            .sentErrorMessage("commands.list_structures.error.no_structures_found");
    }

    @Test
    void run_shouldFindUnownedStructuresWithAdminPermission()
    {
        // Setup
        final var retriever = MockInjector.injectInto(StructureRetrieverFactory.class).ofStructures(structures);
        UnitTestUtil.initMessageable(playerCommandSender);
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, true);

        // Execute
        assertDoesNotThrow(
            () -> factory.newListStructures(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));

        // Verify
        assertThatMessageable(playerCommandSender)
            .extractSentTextMessages()
            .anyMatch(msg -> msg.contains("commands.list_structures.structure_list_header"));
    }

    @Test
    void run_shouldFindUnownedStructuresAsServer()
    {
        // Setup
        final var retriever = MockInjector.injectInto(StructureRetrieverFactory.class).ofStructures(structures);
        UnitTestUtil.initMessageable(serverCommandSender);

        // Execute
        assertDoesNotThrow(
            () -> factory.newListStructures(serverCommandSender, retriever).run().get(1, TimeUnit.SECONDS));

        // Verify
        assertThatMessageable(serverCommandSender)
            .extractSentTextMessages()
            .anyMatch(msg -> msg.contains("commands.list_structures.structure_list_header"));
    }
}
