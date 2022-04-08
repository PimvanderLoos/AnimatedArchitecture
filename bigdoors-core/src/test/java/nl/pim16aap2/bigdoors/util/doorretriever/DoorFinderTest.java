package nl.pim16aap2.bigdoors.util.doorretriever;

import nl.pim16aap2.bigdoors.commands.ICommandSender;
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
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void testBasic()
    {
        final List<Long> uids = List.of(0L, 1L, 2L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge");
        setDatabaseIdentifierResults(uids, names);

        final DoorFinder doorFinder = new DoorFinder(doorRetrieverFactory, databaseManager, commandSender, "My");
        Assertions.assertTrue(doorFinder.getDoorUIDs().isPresent());
        Assertions.assertEquals(names, new ArrayList<>(doorFinder.getDoorIdentifiersIfAvailable().get()));

        doorFinder.processInput("MyD");
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"), doorFinder.getDoorIdentifiersIfAvailable().get());
        Assertions.assertEquals(Set.of(0L, 2L), doorFinder.getDoorUIDs().get());
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
        doorFinder.processInput("T");
        doorFinder.processInput("Th");

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
        doorFinder.processInput("My");
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
        // Somewhat counterintuitively, searching for "T", will result in _all_ names being in the returned set.
        // This happens because we don't filter in the method that retrieves the identifiers from the database;
        // the database can take care of that.
        Assertions.assertEquals(names, new ArrayList<>(doorFinder.getDoorIdentifiersIfAvailable().get()));
        doorFinder.processInput("Th");
        Mockito.verify(databaseManager, Mockito.times(2)).getIdentifiersFromPartial(Mockito.anyString(), Mockito.any());
        Assertions.assertEquals(Set.of("TheirFlag"), doorFinder.getDoorIdentifiersIfAvailable().get());
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
                final boolean useName = Util.parseLong(invocation.getArgument(0, String.class)).isEmpty();
                final List<DoorIdentifier> identifiers = createDoorIdentifiers(uids, names, useName);
                return CompletableFuture.completedFuture(identifiers);
            });
    }
}
