package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
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
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
            .thenReturn(databaseResult);

        new StructureFinder(structureRetrieverFactory,
            databaseManager,
            commandSender,
            "M"
        );
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), eq(PermissionLevel.CREATOR));

        new StructureFinder(structureRetrieverFactory,
            databaseManager,
            commandSender,
            "M",
            PermissionLevel.ADMIN
        );
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), eq(PermissionLevel.ADMIN));

        new StructureFinder(structureRetrieverFactory,
            databaseManager,
            commandSender,
            "M",
            PermissionLevel.USER
        );
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), eq(PermissionLevel.USER));
    }

    @RepeatedTest(value = 30)
    void testDelayedResults()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> databaseResult = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
            .thenReturn(databaseResult);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        Assertions.assertTrue(structureFinder.getStructureIdentifiersIfAvailable().isEmpty());
        final CompletableFuture<Set<String>> returned = structureFinder.getStructureIdentifiers();
        Assertions.assertFalse(returned.isDone());

        databaseResult.complete(List.of(new DatabaseManager.StructureIdentifier(0, "MyDoor")));
        Thread.sleep(100); // Give it a slight delay to allow the notification to propagate.
        Assertions.assertTrue(returned.isDone());

        final List<String> names = new ArrayList<>(returned.get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(1, names.size());
        Assertions.assertEquals("MyDoor", names.get(0));

        // Ensure that trying to retrieve it again will just give us the result immediately.
        final CompletableFuture<Set<String>> again = structureFinder.getStructureIdentifiers();
        Assertions.assertTrue(again.isDone());

        final List<String> namesAgain = new ArrayList<>(again.get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(1, namesAgain.size());
        Assertions.assertEquals("MyDoor", namesAgain.get(0));
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

        structureFinder.processInput("Myd"); // case-insensitive
        structureFinder.processInput("Myd"); // Repeating shouldn't change anything
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiers().get(10, TimeUnit.SECONDS)
        );
        Assertions.assertEquals(Set.of(0L, 2L), structureFinder.getStructureUIDs().get());
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void inputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DatabaseManager.StructureIdentifier> identifiers = createStructureIdentifiers(uids, names, true);
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> output = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
            .thenReturn(output);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        structureFinder.processInput("My");
        structureFinder.processInput("MyD");
        structureFinder.processInput("MyDr");
        structureFinder.processInput("MyD");

        Assertions.assertTrue(structureFinder.getStructureUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(structureFinder.getStructureUIDs().isEmpty());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void changedInputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DatabaseManager.StructureIdentifier> identifiers = createStructureIdentifiers(uids, names, true);
        final CompletableFuture<List<DatabaseManager.StructureIdentifier>> output = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
            .thenReturn(output);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        structureFinder.processInput("My");
        structureFinder.processInput("MyD");
        structureFinder.processInput("MyDr");
        Assertions.assertEquals(List.of("My", "MyD", "MyDr"), new ArrayList<>(structureFinder.getPostponedInputs()));
        structureFinder.processInput("MyPo");
        Assertions.assertEquals(List.of("My", "MyPo"), new ArrayList<>(structureFinder.getPostponedInputs()));
        structureFinder.processInput("T");
        structureFinder.processInput("Th");
        Assertions.assertEquals(List.of("Th"), new ArrayList<>(structureFinder.getPostponedInputs()));

        Assertions.assertTrue(structureFinder.getStructureUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(structureFinder.getStructureUIDs().isEmpty());
        Assertions.assertEquals(Set.of("TheirFlag"), structureFinder.getStructureIdentifiersIfAvailable().get());

        Mockito.verify(databaseManager, Mockito.times(2))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void rollback()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final StructureFinder structureFinder =
            new StructureFinder(structureRetrieverFactory, databaseManager, commandSender, "M");

        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertTrue(structureFinder.getStructureUIDs().isPresent());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyPortcullis", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("MyD");
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("M");
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertEquals(
            Set.of("MyDoor", "MyDrawbridge", "MyPortcullis"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );

        structureFinder.processInput("T");
        Mockito.verify(databaseManager, Mockito.times(2))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
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

        structureFinder.processInput("10");
        Assertions.assertTrue(structureFinder.getStructureIdentifiersIfAvailable().isPresent());
        Assertions.assertEquals(
            Set.of("100", "101"),
            structureFinder.getStructureIdentifiersIfAvailable().get()
        );
        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
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

        structureFinder.processInput("MyDoor");
        Assertions.assertTrue(structureFinder.getStructureIdentifiersIfAvailable(true).isPresent());
        Assertions.assertEquals(
            Set.of("MyDoor"),
            structureFinder.getStructureIdentifiersIfAvailable(true).get()
        );
        Assertions.assertEquals(
            Set.of("MyDoor"),
            structureFinder.getStructureIdentifiers(true).get(10, TimeUnit.SECONDS)
        );

        Mockito.verify(databaseManager, Mockito.times(1))
            .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
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
            Mockito.when(structure.getUid()).thenReturn(uids.get(idx));
            Mockito.when(structure.getName()).thenReturn(names.get(idx));
            structures.add(idx, structure);
        }

        Mockito.when(structureRetrieverFactory.of(Mockito.anyLong()))
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
        structureFinder.processInput("MyDrawbridge");
        Assertions.assertEquals(
            List.of(structures.get(2)),
            structureFinder.getStructures(true).get(1, TimeUnit.SECONDS)
        );
    }

    private List<DatabaseManager.StructureIdentifier> createStructureIdentifiers(
        List<Long> uids, List<String> names, boolean useNames)
    {
        final List<DatabaseManager.StructureIdentifier> ret = new ArrayList<>(uids.size());
        final List<?> idSource = useNames ? names : uids;
        for (int idx = 0; idx < uids.size(); ++idx)
            ret.add(new DatabaseManager.StructureIdentifier(uids.get(idx), String.valueOf(idSource.get(idx))));
        return ret;
    }

    private void setDatabaseIdentifierResults(List<Long> uids, List<String> names)
    {
        Assertions.assertEquals(uids.size(), names.size());
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
            .thenAnswer(invocation ->
            {
                final String input = invocation.getArgument(0, String.class);
                final boolean useNames = Util.parseLong(invocation.getArgument(0, String.class)).isEmpty();
                final ArrayList<DatabaseManager.StructureIdentifier> identifiers = new ArrayList<>(uids.size());
                final List<?> idSource = useNames ? names : uids;
                for (int idx = 0; idx < uids.size(); ++idx)
                {
                    final String identifier = String.valueOf(idSource.get(idx));
                    if (StructureFinder.startsWith(input, identifier))
                        identifiers.add(new DatabaseManager.StructureIdentifier(uids.get(idx), identifier));
                }
                identifiers.trimToSize();
                return CompletableFuture.completedFuture(identifiers);
            });
    }
}
