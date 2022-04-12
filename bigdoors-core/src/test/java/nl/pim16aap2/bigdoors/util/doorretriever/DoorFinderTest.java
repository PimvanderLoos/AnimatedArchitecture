package nl.pim16aap2.bigdoors.util.doorretriever;

import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static nl.pim16aap2.bigdoors.managers.DatabaseManager.DoorIdentifier;

@Timeout(value = 2)
class DoorFinderTest
{
    @Mock
    DoorRetrieverFactory doorRetrieverFactory;

    @Mock
    DatabaseManager databaseManager;

    @Mock
    ICommandSender commandSender;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDelayedResults()
        throws InterruptedException
    {
        final CompletableFuture<List<DoorIdentifier>> databaseResult = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any()))
               .thenReturn(databaseResult);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "M");

        Assertions.assertTrue(doorFinder.getDoorIdentifiersIfAvailable().isEmpty());
        final CompletableFuture<Set<String>> returned = doorFinder.getDoorIdentifiers();
        Assertions.assertFalse(returned.isDone());

        databaseResult.complete(List.of(new DoorIdentifier(0, "MyDoor")));
        Thread.sleep(100); // Give it a slight delay to allow the notification to propagate.
        Assertions.assertTrue(returned.isDone());

        final List<String> names = new ArrayList<>(returned.join());
        Assertions.assertEquals(1, names.size());
        Assertions.assertEquals("MyDoor", names.get(0));

        // Ensure that trying to retrieve it again will just give us the result immediately.
        final CompletableFuture<Set<String>> again = doorFinder.getDoorIdentifiers();
        Assertions.assertTrue(again.isDone());

        final List<String> namesAgain = new ArrayList<>(again.join());
        Assertions.assertEquals(1, namesAgain.size());
        Assertions.assertEquals("MyDoor", namesAgain.get(0));
    }

    @Test
    void startsWith()
    {
        Assertions.assertTrue(DoorFinder.startsWith("a", "ab"));
        Assertions.assertTrue(DoorFinder.startsWith("A", "ab"));
        Assertions.assertTrue(DoorFinder.startsWith("a", "Ab"));
        Assertions.assertTrue(DoorFinder.startsWith("a", "A"));

        Assertions.assertFalse(DoorFinder.startsWith("ab", "A"));
        Assertions.assertFalse(DoorFinder.startsWith("ab", "bA"));
        Assertions.assertFalse(DoorFinder.startsWith("a", ""));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void testBasic()
        throws ExecutionException, InterruptedException
    {
        final List<Long> uids = List.of(0L, 1L, 2L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge");
        setDatabaseIdentifierResults(uids, names);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "My");
        Assertions.assertTrue(doorFinder.getDoorUIDs().isPresent());
        Assertions.assertEquals(names, new ArrayList<>(doorFinder.getDoorIdentifiersIfAvailable().get()));

        doorFinder.processInput("Myd"); // case-insensitive
        doorFinder.processInput("Myd"); // Repeating shouldn't change anything
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"), doorFinder.getDoorIdentifiersIfAvailable().get());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"), doorFinder.getDoorIdentifiers().get());
        Assertions.assertEquals(Set.of(0L, 2L), doorFinder.getDoorUIDs().get());
        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
    }

    @Test
    void inputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DoorIdentifier> identifiers = createDoorIdentifiers(uids, names, true);
        final CompletableFuture<List<DoorIdentifier>> output = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any())).thenReturn(output);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "M");
        doorFinder.processInput("My");
        doorFinder.processInput("MyD");
        doorFinder.processInput("MyDr");
        doorFinder.processInput("MyD");

        Assertions.assertTrue(doorFinder.getDoorUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(doorFinder.getDoorUIDs().isEmpty());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"), doorFinder.getDoorIdentifiersIfAvailable().get());

        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
    }

    @Test
    void changedInputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<DoorIdentifier> identifiers = createDoorIdentifiers(uids, names, true);
        final CompletableFuture<List<DoorIdentifier>> output = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any())).thenReturn(output);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "M");
        doorFinder.processInput("My");
        doorFinder.processInput("MyD");
        doorFinder.processInput("MyDr");
        Assertions.assertEquals(List.of("My", "MyD", "MyDr"), new ArrayList<>(doorFinder.getPostponedInputs()));
        doorFinder.processInput("MyPo");
        Assertions.assertEquals(List.of("My", "MyPo"), new ArrayList<>(doorFinder.getPostponedInputs()));
        doorFinder.processInput("T");
        doorFinder.processInput("Th");
        Assertions.assertEquals(List.of("Th"), new ArrayList<>(doorFinder.getPostponedInputs()));

        Assertions.assertTrue(doorFinder.getDoorUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(doorFinder.getDoorUIDs().isEmpty());
        Assertions.assertEquals(Set.of("TheirFlag"), doorFinder.getDoorIdentifiersIfAvailable().get());

        Mockito.verify(databaseManager, Mockito.times(2)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
    }

    @Test
    void rollback()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "M");
        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
        Assertions.assertTrue(doorFinder.getDoorUIDs().isPresent());
        Assertions.assertEquals(Set.of("MyDoor", "MyPortcullis", "MyDrawbridge"),
                                doorFinder.getDoorIdentifiersIfAvailable().get());

        doorFinder.processInput("MyD");
        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"), doorFinder.getDoorIdentifiersIfAvailable().get());

        doorFinder.processInput("M");
        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge", "MyPortcullis"),
                                doorFinder.getDoorIdentifiersIfAvailable().get());

        doorFinder.processInput("T");
        Mockito.verify(databaseManager, Mockito.times(2)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
        Assertions.assertEquals(Set.of("TheirFlag"), doorFinder.getDoorIdentifiersIfAvailable().get());
    }

    @Test
    void numericalInput()
    {
        final List<Long> uids = List.of(100L, 101L, 120L, 130L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "1");
        doorFinder.processInput("10");
        Assertions.assertTrue(doorFinder.getDoorIdentifiersIfAvailable().isPresent());
        Assertions.assertEquals(Set.of("100", "101"), doorFinder.getDoorIdentifiersIfAvailable().get());
        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
    }

    @Test
    void exactMatch()
        throws ExecutionException, InterruptedException
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "M");

        Assertions.assertTrue(doorFinder.getDoorUIDs(true).isPresent());
        Assertions.assertTrue(doorFinder.getDoorUIDs(true).get().isEmpty());
        Assertions.assertTrue(doorFinder.getDoorIdentifiers(true).get().isEmpty());

        doorFinder.processInput("MyDoor");
        Assertions.assertTrue(doorFinder.getDoorIdentifiersIfAvailable(true).isPresent());
        Assertions.assertEquals(Set.of("MyDoor"), doorFinder.getDoorIdentifiersIfAvailable(true).get());
        Assertions.assertEquals(Set.of("MyDoor"), doorFinder.getDoorIdentifiers(true).get());

        Mockito.verify(databaseManager, Mockito.times(1)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
    }

    @Test
    void getDoors()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final List<AbstractDoor> doors = new ArrayList<>(uids.size());
        for (int idx = 0; idx < names.size(); ++idx)
        {
            final AbstractDoor door = Mockito.mock(AbstractDoor.class);
            Mockito.when(door.getDoorUID()).thenReturn(uids.get(idx));
            Mockito.when(door.getName()).thenReturn(names.get(idx));
            doors.add(idx, door);
        }

        Mockito.when(doorRetrieverFactory.of(Mockito.anyLong())).thenAnswer(
            invocation ->
            {
                final long uid = invocation.getArgument(0, Long.class);
                if (uid < 0 || uid >= doors.size())
                    throw new IllegalArgumentException("No door with UID " + uid + " available!");
                return new DoorRetriever.DoorObjectRetriever(doors.get((int) uid));
            });

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "M");

        // Only idx=3 is excluded.
        Assertions.assertEquals(doors.subList(0, 3), doorFinder.getDoors().get(1, TimeUnit.SECONDS));

        Assertions.assertTrue(doorFinder.getDoors(true).get(1, TimeUnit.SECONDS).isEmpty());
        doorFinder.processInput("MyDrawbridge");
        Assertions.assertEquals(List.of(doors.get(2)), doorFinder.getDoors(true).get(1, TimeUnit.SECONDS));
    }

    private List<DoorIdentifier> createDoorIdentifiers(List<Long> uids, List<String> names, boolean useNames)
    {
        final List<DoorIdentifier> ret = new ArrayList<>(uids.size());
        final List<?> idSource = useNames ? names : uids;
        for (int idx = 0; idx < uids.size(); ++idx)
            ret.add(new DoorIdentifier(uids.get(idx), String.valueOf(idSource.get(idx))));
        return ret;
    }

    private void setDatabaseIdentifierResults(List<Long> uids, List<String> names)
    {
        Assertions.assertEquals(uids.size(), names.size());
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any())).thenAnswer(
            invocation ->
            {
                final String input = invocation.getArgument(0, String.class);
                final boolean useNames = Util.parseLong(invocation.getArgument(0, String.class)).isEmpty();
                final ArrayList<DoorIdentifier> identifiers = new ArrayList<>(uids.size());
                final List<?> idSource = useNames ? names : uids;
                for (int idx = 0; idx < uids.size(); ++idx)
                {
                    final String identifier = String.valueOf(idSource.get(idx));
                    System.out.printf("Identifier: %s, input: %s, startsWith: %s\n",
                                      identifier, input, DoorFinder.startsWith(input, identifier));
                    if (DoorFinder.startsWith(input, identifier))
                        identifiers.add(new DoorIdentifier(uids.get(idx), identifier));
                }
                identifiers.trimToSize();
                return CompletableFuture.completedFuture(identifiers);
            });
    }
}
