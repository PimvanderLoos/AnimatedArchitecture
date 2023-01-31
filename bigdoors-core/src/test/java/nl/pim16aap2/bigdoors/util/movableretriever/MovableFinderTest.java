package nl.pim16aap2.bigdoors.util.movableretriever;

import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
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

import static nl.pim16aap2.bigdoors.managers.DatabaseManager.MovableIdentifier;
import static org.mockito.ArgumentMatchers.eq;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Timeout(value = 2)
class MovableFinderTest
{
    @Mock
    MovableRetrieverFactory movableRetrieverFactory;

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
    void propagateMaxPermission()
    {
        final CompletableFuture<List<MovableIdentifier>> databaseResult = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
               .thenReturn(databaseResult);

        new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M");
        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), eq(PermissionLevel.CREATOR));

        new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M", PermissionLevel.ADMIN);
        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), eq(PermissionLevel.ADMIN));

        new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M", PermissionLevel.USER);
        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), eq(PermissionLevel.USER));
    }

    @Test
    void testDelayedResults()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        final CompletableFuture<List<MovableIdentifier>> databaseResult = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
               .thenReturn(databaseResult);

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M");

        Assertions.assertTrue(movableFinder.getMovableIdentifiersIfAvailable().isEmpty());
        final CompletableFuture<Set<String>> returned = movableFinder.getMovableIdentifiers();
        Assertions.assertFalse(returned.isDone());

        databaseResult.complete(List.of(new MovableIdentifier(0, "MyDoor")));
        Thread.sleep(100); // Give it a slight delay to allow the notification to propagate.
        Assertions.assertTrue(returned.isDone());

        final List<String> names = new ArrayList<>(returned.get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(1, names.size());
        Assertions.assertEquals("MyDoor", names.get(0));

        // Ensure that trying to retrieve it again will just give us the result immediately.
        final CompletableFuture<Set<String>> again = movableFinder.getMovableIdentifiers();
        Assertions.assertTrue(again.isDone());

        final List<String> namesAgain = new ArrayList<>(again.get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(1, namesAgain.size());
        Assertions.assertEquals("MyDoor", namesAgain.get(0));
    }

    @Test
    void startsWith()
    {
        Assertions.assertTrue(MovableFinder.startsWith("a", "ab"));
        Assertions.assertTrue(MovableFinder.startsWith("A", "ab"));
        Assertions.assertTrue(MovableFinder.startsWith("a", "Ab"));
        Assertions.assertTrue(MovableFinder.startsWith("a", "A"));

        Assertions.assertFalse(MovableFinder.startsWith("ab", "A"));
        Assertions.assertFalse(MovableFinder.startsWith("ab", "bA"));
        Assertions.assertFalse(MovableFinder.startsWith("a", ""));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void testBasic()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final List<Long> uids = List.of(0L, 1L, 2L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge");
        setDatabaseIdentifierResults(uids, names);

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "My");

        Assertions.assertTrue(movableFinder.getMovableUIDs().isPresent());
        Assertions.assertEquals(names, new ArrayList<>(movableFinder.getMovableIdentifiersIfAvailable().get()));

        movableFinder.processInput("Myd"); // case-insensitive
        movableFinder.processInput("Myd"); // Repeating shouldn't change anything
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"),
                                movableFinder.getMovableIdentifiers().get(10, TimeUnit.SECONDS));
        Assertions.assertEquals(Set.of(0L, 2L), movableFinder.getMovableUIDs().get());
        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void inputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<MovableIdentifier> identifiers = createMovableIdentifiers(uids, names, true);
        final CompletableFuture<List<MovableIdentifier>> output = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
               .thenReturn(output);

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M");

        movableFinder.processInput("My");
        movableFinder.processInput("MyD");
        movableFinder.processInput("MyDr");
        movableFinder.processInput("MyD");

        Assertions.assertTrue(movableFinder.getMovableUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(movableFinder.getMovableUIDs().isEmpty());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());

        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void changedInputBeforeResults()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        final List<MovableIdentifier> identifiers = createMovableIdentifiers(uids, names, true);
        final CompletableFuture<List<MovableIdentifier>> output = new CompletableFuture<>();
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any()))
               .thenReturn(output);

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M");

        movableFinder.processInput("My");
        movableFinder.processInput("MyD");
        movableFinder.processInput("MyDr");
        Assertions.assertEquals(List.of("My", "MyD", "MyDr"), new ArrayList<>(movableFinder.getPostponedInputs()));
        movableFinder.processInput("MyPo");
        Assertions.assertEquals(List.of("My", "MyPo"), new ArrayList<>(movableFinder.getPostponedInputs()));
        movableFinder.processInput("T");
        movableFinder.processInput("Th");
        Assertions.assertEquals(List.of("Th"), new ArrayList<>(movableFinder.getPostponedInputs()));

        Assertions.assertTrue(movableFinder.getMovableUIDs().isEmpty());
        output.complete(identifiers);
        Assertions.assertFalse(movableFinder.getMovableUIDs().isEmpty());
        Assertions.assertEquals(Set.of("TheirFlag"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());

        Mockito.verify(databaseManager, Mockito.times(2))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void rollback()
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M");

        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertTrue(movableFinder.getMovableUIDs().isPresent());
        Assertions.assertEquals(Set.of("MyDoor", "MyPortcullis", "MyDrawbridge"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());

        movableFinder.processInput("MyD");
        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());

        movableFinder.processInput("M");
        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertEquals(Set.of("MyDoor", "MyDrawbridge", "MyPortcullis"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());

        movableFinder.processInput("T");
        Mockito.verify(databaseManager, Mockito.times(2))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assertions.assertEquals(Set.of("TheirFlag"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());
    }

    @Test
    void numericalInput()
    {
        final List<Long> uids = List.of(100L, 101L, 120L, 130L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "1");

        movableFinder.processInput("10");
        Assertions.assertTrue(movableFinder.getMovableIdentifiersIfAvailable().isPresent());
        Assertions.assertEquals(Set.of("100", "101"),
                                movableFinder.getMovableIdentifiersIfAvailable().get());
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

        final MovableFinder movableFinder = new MovableFinder(movableRetrieverFactory, databaseManager, commandSender,
                                                              "M");


        Assertions.assertTrue(movableFinder.getMovableUIDs(true).isPresent());
        Assertions.assertTrue(movableFinder.getMovableUIDs(true).get().isEmpty());
        Assertions.assertTrue(movableFinder.getMovableIdentifiers(true).get(10, TimeUnit.SECONDS).isEmpty());

        movableFinder.processInput("MyDoor");
        Assertions.assertTrue(movableFinder.getMovableIdentifiersIfAvailable(true).isPresent());
        Assertions.assertEquals(Set.of("MyDoor"),
                                movableFinder.getMovableIdentifiersIfAvailable(true).get());
        Assertions.assertEquals(Set.of("MyDoor"), movableFinder.getMovableIdentifiers(true).get(10, TimeUnit.SECONDS));

        Mockito.verify(databaseManager, Mockito.times(1))
               .getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void getMovables()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final List<Long> uids = List.of(0L, 1L, 2L, 3L);
        final List<String> names = List.of("MyDoor", "MyPortcullis", "MyDrawbridge", "TheirFlag");
        setDatabaseIdentifierResults(uids, names);

        final List<AbstractMovable> movables = new ArrayList<>(uids.size());
        for (int idx = 0; idx < names.size(); ++idx)
        {
            final AbstractMovable movable = Mockito.mock(AbstractMovable.class);
            Mockito.when(movable.getUid()).thenReturn(uids.get(idx));
            Mockito.when(movable.getName()).thenReturn(names.get(idx));
            movables.add(idx, movable);
        }

        Mockito.when(movableRetrieverFactory.of(Mockito.anyLong())).thenAnswer(
            invocation ->
            {
                final long uid = invocation.getArgument(0, Long.class);
                if (uid < 0 || uid >= movables.size())
                    throw new IllegalArgumentException("No movable with UID " + uid + " available!");
                return new MovableRetriever.MovableObjectRetriever(movables.get((int) uid));
            });

        final MovableFinder movableFinder =
            new MovableFinder(movableRetrieverFactory, databaseManager, commandSender, "M");

        // Only idx=3 is excluded.
        Assertions.assertEquals(movables.subList(0, 3), movableFinder.getMovables().get(1, TimeUnit.SECONDS));

        Assertions.assertTrue(movableFinder.getMovables(true).get(1, TimeUnit.SECONDS).isEmpty());
        movableFinder.processInput("MyDrawbridge");
        Assertions.assertEquals(List.of(movables.get(2)), movableFinder.getMovables(true).get(1, TimeUnit.SECONDS));
    }

    private List<MovableIdentifier> createMovableIdentifiers(List<Long> uids, List<String> names, boolean useNames)
    {
        final List<MovableIdentifier> ret = new ArrayList<>(uids.size());
        final List<?> idSource = useNames ? names : uids;
        for (int idx = 0; idx < uids.size(); ++idx)
            ret.add(new MovableIdentifier(uids.get(idx), String.valueOf(idSource.get(idx))));
        return ret;
    }

    private void setDatabaseIdentifierResults(List<Long> uids, List<String> names)
    {
        Assertions.assertEquals(uids.size(), names.size());
        Mockito.when(databaseManager.getIdentifiersFromPartial(Mockito.anyString(), Mockito.any(),
                                                               Mockito.any())).thenAnswer(
            invocation ->
            {
                final String input = invocation.getArgument(0, String.class);
                final boolean useNames = Util.parseLong(invocation.getArgument(0, String.class)).isEmpty();
                final ArrayList<MovableIdentifier> identifiers = new ArrayList<>(uids.size());
                final List<?> idSource = useNames ? names : uids;
                for (int idx = 0; idx < uids.size(); ++idx)
                {
                    final String identifier = String.valueOf(idSource.get(idx));
                    if (MovableFinder.startsWith(input, identifier))
                        identifiers.add(new MovableIdentifier(uids.get(idx), identifier));
                }
                identifiers.trimToSize();
                return CompletableFuture.completedFuture(identifiers);
            });
    }
}
