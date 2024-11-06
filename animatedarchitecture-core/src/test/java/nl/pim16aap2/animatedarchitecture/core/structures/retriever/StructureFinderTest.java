package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.junit.jupiter.api.Assertions;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "DefaultAnnotationParam"})
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StructureFinderTest
{
    @Mock
    StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    DatabaseManager databaseManager;

    @Mock
    ICommandSender commandSender;

    @Test
    void propagateMaxPermission()
    {
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> databaseResult = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anyCollection())
        ).thenReturn(databaseResult);

        new StructureFinder(
            structureRetrieverFactory,
            databaseManager,
            commandSender,
            "M"
        );

        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(
                Mockito.anyString(),
                Mockito.any(),
                eq(PermissionLevel.CREATOR),
                Mockito.anyCollection()
            );

        new StructureFinder(
            structureRetrieverFactory,
            databaseManager,
            commandSender,
            "M",
            PermissionLevel.ADMIN,
            Set.of()
        );

        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(
                Mockito.anyString(),
                Mockito.any(),
                eq(PermissionLevel.ADMIN),
                Mockito.anyCollection()
            );

        new StructureFinder(
            structureRetrieverFactory,
            databaseManager,
            commandSender,
            "M",
            PermissionLevel.USER,
            Set.of()
        );

        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(
                Mockito.anyString(),
                Mockito.any(),
                eq(PermissionLevel.USER),
                Mockito.anyCollection()
            );
    }

    @RepeatedTest(value = 30)
    void testDelayedResults()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> databaseResult = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anyCollection())
        ).thenReturn(databaseResult);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        Assertions.assertTrue(structureFinder.getStructureIdentifiersIfAvailable().isEmpty());
        final CompletableFuture<Set<String>> returned = structureFinder.getStructureIdentifiers();
        Assertions.assertFalse(returned.isDone());

        databaseResult.complete(List.of(new DatabaseManager.StructureIdentifier(Mockito.mock(), 0, "MyDoor")));
        Thread.sleep(100); // Give it a slight delay to allow the notification to propagate.
        Assertions.assertTrue(returned.isDone());

        final List<String> names = new ArrayList<>(returned.get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(1, names.size());
        Assertions.assertEquals("MyDoor", names.getFirst());

        // Ensure that trying to retrieve it again will just give us the result immediately.
        final CompletableFuture<Set<String>> again = structureFinder.getStructureIdentifiers();
        Assertions.assertTrue(again.isDone());

        final List<String> namesAgain = new ArrayList<>(again.get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(1, namesAgain.size());
        Assertions.assertEquals("MyDoor", namesAgain.getFirst());
    }

    @Test
    void startsWith()
    {
        Assertions.assertTrue(StructureFinder.startsWith("a", "ab"));
        Assertions.assertTrue(StructureFinder.startsWith("A", "ab"));
        Assertions.assertTrue(StructureFinder.startsWith("a", "Ab"));
        Assertions.assertTrue(StructureFinder.startsWith("a", "A"));

        Assertions.assertFalse(StructureFinder.startsWith("ab", "A"));
        Assertions.assertFalse(StructureFinder.startsWith("ab", "bA"));
        Assertions.assertFalse(StructureFinder.startsWith("a", ""));
    }

    @Test
    void testBasic()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final List<Long> uids = List.of(0L, 1L, 2L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "My");

        Assertions.assertTrue(structureFinder.getStructureUIDs().isPresent());
        Assertions.assertEquals(names, new ArrayList<>(structureFinder.getStructureIdentifiersIfAvailable().get()));

        structureFinder.processInput("Myd", List.of()); // case-insensitive
        structureFinder.processInput("Myd", List.of()); // Repeating shouldn't change anything
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiers().get(10, TimeUnit.SECONDS)
        );
        Assertions.assertEquals(Set.of(0L, 2L), structureFinder.getStructureUIDs().get());
        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
    }

    @Test
    void inputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DatabaseManager.StructureIdentifier> identifiers = createStructureIdentifiers(uids, names, true);
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> output = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anyCollection())
        ).thenReturn(output);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        structureFinder.processInput("My", List.of());
        structureFinder.processInput("MyD", List.of());
        structureFinder.processInput("MyDr", List.of());
        structureFinder.processInput("MyD", List.of());

        Assertions.assertTrue(structureFinder.getStructureUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(structureFinder.getStructureUIDs().isEmpty());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
    }

    @Test
    void changedInputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DatabaseManager.StructureIdentifier> identifiers = createStructureIdentifiers(uids, names, true);
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> output = new CompletableFuture<>();
        when(databaseManager.getIdentifiersFromPartial(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anyCollection())
        ).thenReturn(output);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        structureFinder.processInput("My", List.of());
        structureFinder.processInput("MyD", List.of());
        structureFinder.processInput("MyDr", List.of());
        Assertions.assertEquals(List.of("My", "MyD", "MyDr"), new ArrayList<>(structureFinder.getPostponedInputs()));
        structureFinder.processInput("MyPo", List.of());
        Assertions.assertEquals(List.of("My", "MyPo"), new ArrayList<>(structureFinder.getPostponedInputs()));
        structureFinder.processInput("T", List.of());
        structureFinder.processInput("Th", List.of());
        Assertions.assertEquals(List.of("Th"), new ArrayList<>(structureFinder.getPostponedInputs()));

        Assertions.assertTrue(structureFinder.getStructureUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(structureFinder.getStructureUIDs().isEmpty());
        Assertions.assertEquals(Set.of("TheirFlag"), structureFinder.getStructureIdentifiersIfAvailable().get());

        verify(databaseManager, Mockito.times(2))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
    }

    @Test
    void rollback()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
        Assertions.assertTrue(structureFinder.getStructureUIDs().isPresent());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyPortcullis", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("MyD", List.of());
        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("M", List.of());
        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge", "MyPortcullis"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("T", List.of());
        verify(databaseManager, Mockito.times(2))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
        Assertions.assertEquals(
            Set.of("TheirFlag"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
    }

    @Test
    void numericalInput()
    {
        final List<Long> uids = List.of(100L, 101L, 120L, 130L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory,
                databaseManager, commandSender, "1");

        structureFinder.processInput("10", List.of());
        Assertions.assertTrue(structureFinder.getStructureIdentifiersIfAvailable().isPresent());
        Assertions.assertEquals(
            Set.of("100", "101"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
    }

    @Test
    void exactMatch()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        Assertions.assertTrue(structureFinder.getStructureUIDs(true).isPresent());
        Assertions.assertTrue(structureFinder.getStructureUIDs(true).get().isEmpty());
        Assertions.assertTrue(structureFinder.getStructureIdentifiers(true).get(10, TimeUnit.SECONDS).isEmpty());

        structureFinder.processInput("MyDoor", List.of());
        Assertions.assertTrue(structureFinder.getStructureIdentifiersIfAvailable(true).isPresent());
        Assertions.assertEquals(
            Set.of("MyDoor"),
            structureFinder.getStructureIdentifiersIfAvailable(true).get()
        );
        Assertions.assertEquals(
            Set.of("MyDoor"),
            structureFinder.getStructureIdentifiers(true).get(10, TimeUnit.SECONDS)
        );

        verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyCollection());
    }

    @Test
    void getStructures()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final List<AbstractStructure> structures = new ArrayList<>(uids.size());
        for (int idx = 0; idx < names.size(); ++idx)
        {
            final AbstractStructure structure = Mockito.mock(AbstractStructure.class);
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
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        // Only idx=3 is excluded.
        Assertions.assertEquals(structures.subList(0, 3), structureFinder.getStructures().get(1, TimeUnit.SECONDS));

        Assertions.assertTrue(structureFinder.getStructures(true).get(1, TimeUnit.SECONDS).isEmpty());
        structureFinder.processInput("MyDrawbridge", List.of());
        Assertions.assertEquals(
            List.of(structures.get(2)),
            structureFinder.getStructures(true).get(1, TimeUnit.SECONDS)
        );
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
                Mockito.mock(),
                uids.get(idx),
                String.valueOf(idSource.get(idx))
            ));
        return ret;
    }

    private void setDatabaseIdentifierResults(List<Long> uids, List<String> names)
    {
        Assertions.assertEquals(uids.size(), names.size());

        when(databaseManager.getIdentifiersFromPartial(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anyCollection())
        ).thenAnswer(invocation ->
        {
            final String input = invocation.getArgument(0, String.class);
            final boolean useNames = MathUtil.parseLong(invocation.getArgument(0, String.class)).isEmpty();
            final ArrayList<DatabaseManager.StructureIdentifier> identifiers = new ArrayList<>(uids.size());
            final List<?> idSource = useNames ? names : uids;
            for (int idx = 0; idx < uids.size(); ++idx)
            {
                final String identifier = String.valueOf(idSource.get(idx));
                if (StructureFinder.startsWith(input, identifier))
                    identifiers.add(new DatabaseManager.StructureIdentifier(
                        Mockito.mock(),
                        uids.get(idx),
                        identifier
                    ));
            }
            identifiers.trimToSize();
            return CompletableFuture.completedFuture(identifiers);
        });
    }
}
