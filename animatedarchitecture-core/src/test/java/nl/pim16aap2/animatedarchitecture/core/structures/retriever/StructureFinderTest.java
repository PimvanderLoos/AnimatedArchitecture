package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StructureFinderTest
{
    @Mock
    IExecutor executor;

    @Mock
    StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    DatabaseManager databaseManager;

    @Mock
    ICommandSender commandSender;

    @Test
    void propagateMaxPermission()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> databaseResult = new CompletableFuture<>();

        when(databaseManager
            .getIdentifiersFromPartial(anyString(), any(), any(), anyCollection()))
            .thenReturn(databaseResult);

        new StructureFinder(
            structureRetrieverFactory,
            executor,
            databaseManager,
            commandSender,
            "M"
        );

        verify(databaseManager, times(1))
            .getIdentifiersFromPartial(
                anyString(),
                any(),
                eq(PermissionLevel.CREATOR),
                anyCollection()
            );

        new StructureFinder(
            structureRetrieverFactory,
            executor,
            databaseManager,
            commandSender,
            "M",
            PermissionLevel.ADMIN,
            Set.of()
        );

        verify(databaseManager, times(1))
            .getIdentifiersFromPartial(
                anyString(),
                any(),
                eq(PermissionLevel.ADMIN),
                anyCollection()
            );

        new StructureFinder(
            structureRetrieverFactory,
            executor,
            databaseManager,
            commandSender,
            "M",
            PermissionLevel.USER,
            Set.of()
        );

        verify(databaseManager, times(1))
            .getIdentifiersFromPartial(anyString(), any(), eq(PermissionLevel.USER), anyCollection());
    }

    @RepeatedTest(value = 30)
    void testDelayedResults()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> databaseResult = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(anyString(), any(), any(), anyCollection()))
            .thenReturn(databaseResult);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, executor, databaseManager, commandSender, "M");

        assertTrue(structureFinder.getStructureIdentifiersIfAvailable().isEmpty());
        final CompletableFuture<Set<String>> returned = structureFinder.getStructureIdentifiers();
        assertFalse(returned.isDone());

        databaseResult.complete(List.of(new DatabaseManager.StructureIdentifier(Mockito.mock(), 0, "MyDoor")));
        Thread.sleep(100); // Give it a slight delay to allow the notification to propagate.
        assertTrue(returned.isDone());

        final List<String> names = new ArrayList<>(returned.get(10, TimeUnit.SECONDS));
        assertEquals(1, names.size());
        assertEquals("MyDoor", names.getFirst());

        // Ensure that trying to retrieve it again will just give us the result immediately.
        final CompletableFuture<Set<String>> again = structureFinder.getStructureIdentifiers();
        assertTrue(again.isDone());

        final List<String> namesAgain = new ArrayList<>(again.get(10, TimeUnit.SECONDS));
        assertEquals(1, namesAgain.size());
        assertEquals("MyDoor", namesAgain.getFirst());
    }

    @Test
    void startsWith()
    {
        assertTrue(StructureFinder.startsWith("a", "ab"));
        assertTrue(StructureFinder.startsWith("A", "ab"));
        assertTrue(StructureFinder.startsWith("a", "Ab"));
        assertTrue(StructureFinder.startsWith("a", "A"));

        assertFalse(StructureFinder.startsWith("ab", "A"));
        assertFalse(StructureFinder.startsWith("ab", "bA"));
        assertFalse(StructureFinder.startsWith("a", ""));
    }

    @Test
    void testBasic()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(0L, 1L, 2L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory,
                executor, databaseManager, commandSender, "My");

        assertTrue(structureFinder.getStructureUIDs().isPresent());
        assertEquals(names, new ArrayList<>(structureFinder.getStructureIdentifiersIfAvailable().get()));

        structureFinder.processInput("Myd", List.of()); // case-insensitive
        structureFinder.processInput("Myd", List.of()); // Repeating shouldn't change anything
        assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
        assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiers().get(10, TimeUnit.SECONDS)
        );
        assertEquals(Set.of(0L, 2L), structureFinder.getStructureUIDs().get());
        verify(databaseManager, times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
    }

    @Test
    void inputBeforeResults()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DatabaseManager.StructureIdentifier> identifiers = createStructureIdentifiers(uids, names, true);
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> output = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(anyString(), any(), any(), anyCollection())).thenReturn(output);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, executor, databaseManager, commandSender, "M");

        structureFinder.processInput("My", List.of());
        structureFinder.processInput("MyD", List.of());
        structureFinder.processInput("MyDr", List.of());
        structureFinder.processInput("MyD", List.of());

        assertTrue(structureFinder.getStructureUIDs().isEmpty());
        output.complete(identifiers);
        assertFalse(structureFinder.getStructureUIDs().isEmpty());
        assertEquals(Set.of("MyDoor", "MyDrawbridge"), structureFinder.getStructureIdentifiersIfAvailable().get());

        verify(databaseManager, times(1)).getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
    }

    @Test
    void changedInputBeforeResults()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DatabaseManager.StructureIdentifier> identifiers = createStructureIdentifiers(uids, names, true);
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> output = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(anyString(), any(), any(), anyCollection())).thenReturn(output);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, executor, databaseManager, commandSender, "M");

        structureFinder.processInput("My", List.of());
        structureFinder.processInput("MyD", List.of());
        structureFinder.processInput("MyDr", List.of());
        assertEquals(List.of("My", "MyD", "MyDr"), new ArrayList<>(structureFinder.getPostponedInputs()));
        structureFinder.processInput("MyPo", List.of());
        assertEquals(List.of("My", "MyPo"), new ArrayList<>(structureFinder.getPostponedInputs()));
        structureFinder.processInput("T", List.of());
        structureFinder.processInput("Th", List.of());
        assertEquals(List.of("Th"), new ArrayList<>(structureFinder.getPostponedInputs()));

        assertTrue(structureFinder.getStructureUIDs().isEmpty());
        output.complete(identifiers);
        assertFalse(structureFinder.getStructureUIDs().isEmpty());
        assertEquals(Set.of("TheirFlag"), structureFinder.getStructureIdentifiersIfAvailable().get());

        verify(databaseManager, times(2)).getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
    }

    @Test
    void rollback()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory,
                executor, databaseManager, commandSender, "M");

        verify(databaseManager, times(1)).getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
        assertTrue(structureFinder.getStructureUIDs().isPresent());
        assertEquals(
            Set.of("MyDoor", "MyPortcullis", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("MyD", List.of());
        verify(databaseManager, times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
        assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("M", List.of());
        verify(databaseManager, times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
        assertEquals(
            Set.of("MyDoor", "MyDrawbridge", "MyPortcullis"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("T", List.of());
        verify(databaseManager, times(2))
            .getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
        assertEquals(
            Set.of("TheirFlag"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
    }

    @Test
    void numericalInput()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(100L, 101L, 120L, 130L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, executor, databaseManager, commandSender, "1");

        structureFinder.processInput("10", List.of());
        assertTrue(structureFinder.getStructureIdentifiersIfAvailable().isPresent());
        assertEquals(
            Set.of("100", "101"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
        verify(databaseManager, times(1)).getIdentifiersFromPartial(Mockito.anyString(), any(), any(), anyCollection());
    }

    @Test
    void exactMatch()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory,
                executor, databaseManager, commandSender, "M");

        assertTrue(structureFinder.getStructureUIDs(true).isPresent());
        assertTrue(structureFinder.getStructureUIDs(true).get().isEmpty());
        assertTrue(structureFinder.getStructureIdentifiers(true).get(10, TimeUnit.SECONDS).isEmpty());

        structureFinder.processInput("MyDoor", List.of());
        assertTrue(structureFinder.getStructureIdentifiersIfAvailable(true).isPresent());
        assertEquals(Set.of("MyDoor"), structureFinder.getStructureIdentifiersIfAvailable(true).get());
        assertEquals(Set.of("MyDoor"), structureFinder.getStructureIdentifiers(true).get(10, TimeUnit.SECONDS));

        verify(databaseManager, times(1)).getIdentifiersFromPartial(anyString(), any(), any(), anyCollection());
    }

    @Test
    void getStructures()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final List<Structure> structures = new ArrayList<>(uids.size());
        for (int idx = 0; idx < names.size(); ++idx)
        {
            final Structure structure = mock(Structure.class);
            when(structure.getUid()).thenReturn(uids.get(idx));
            when(structure.getName()).thenReturn(names.get(idx));
            structures.add(idx, structure);
        }

        when(structureRetrieverFactory.of(Mockito.anyLong()))
            .thenAnswer(invocation ->
            {
                final long uid = invocation.getArgument(0, Long.class);
                if (uid < 0 || uid >= structures.size())
                    throw new IllegalArgumentException("No structure with UID " + uid + " available!");
                return new StructureRetriever.StructureObjectRetriever(
                    structures.get(Math.toIntExact(uid)));
            });

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory,
                executor, databaseManager, commandSender, "M");

        // Only idx=3 is excluded.
        assertEquals(structures.subList(0, 3), structureFinder.getStructures().get(1, TimeUnit.SECONDS));

        assertTrue(structureFinder.getStructures(true).get(1, TimeUnit.SECONDS).isEmpty());
        structureFinder.processInput("MyDrawbridge", List.of());
        assertEquals(List.of(structures.get(2)), structureFinder.getStructures(true).get(1, TimeUnit.SECONDS));
    }

    private List<DatabaseManager.StructureIdentifier> createStructureIdentifiers(
        List<Long> uids,
        List<String> names,
        boolean useNames)
    {
        final List<DatabaseManager.StructureIdentifier> ret = new ArrayList<>(uids.size());
        final List<?> idSource = useNames ? names : uids;
        for (int idx = 0; idx < uids.size(); ++idx)
            ret.add(new DatabaseManager.StructureIdentifier(
                mock(),
                uids.get(idx),
                String.valueOf(idSource.get(idx))
            ));
        return ret;
    }

    private void setDatabaseIdentifierResults(List<Long> uids, List<String> names)
    {
        assertEquals(uids.size(), names.size());

        when(databaseManager.getIdentifiersFromPartial(anyString(), any(), any(), anyCollection()))
            .thenAnswer(invocation ->
            {
                final String input = invocation.getArgument(0, String.class);
                final boolean useNames = MathUtil.parseLong(invocation.getArgument(0, String.class)).isEmpty();
                final ArrayList<DatabaseManager.StructureIdentifier> identifiers = new ArrayList<>(uids.size());
                final List<?> idSource = useNames ? names : uids;
                for (int idx = 0; idx < uids.size(); ++idx)
                {
                    final String identifier = String.valueOf(idSource.get(idx));
                    if (StructureFinder.startsWith(input, identifier))
                        identifiers.add(new DatabaseManager.StructureIdentifier(mock(), uids.get(idx), identifier));
                }
                identifiers.trimToSize();
                return CompletableFuture.completedFuture(identifiers);
            });
    }
}
