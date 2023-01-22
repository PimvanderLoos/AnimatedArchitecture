package nl.pim16aap2.bigdoors.storage;

import com.google.common.flogger.LogSiteStackTrace;
import lombok.extern.slf4j.Slf4j;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.MovableDeletionManager;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableBaseBuilder;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.MovableSerializer;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.movable.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.movable.bigdoor.MovableBigDoor;
import nl.pim16aap2.bigdoors.movable.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.movable.drawbridge.MovableTypeDrawbridge;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.movable.portcullis.MovableTypePortcullis;
import nl.pim16aap2.bigdoors.movable.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorldFactory;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssertionsUtil;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import nl.pim16aap2.testing.logging.LogInspector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SQLiteJDBCDriverConnectionTest
{
    /**
     * Name of movable 1.
     */
    private static final String MOVABLE_1_NAME = "random_door_name";

    /**
     * Name of movables 2 and 3.
     */
    private static final String MOVABLES_2_3_NAME = "popular_door_name";

    private static final String DELETE_MOVABLE_NAME = "delete_meh";

    private static final String WORLD_NAME = "TestWorld";

    private static final String PLAYER_2_NAME_ALT = "TestMan";

    private static final PPlayerData PLAYER_DATA_1 =
        new PPlayerData(UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"), "pim16aap2", 10, 11, true, true);

    private static final PPlayerData PLAYER_DATA_2 =
        new PPlayerData(UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed"), "TestBoiii", 20, 22, true, false);

    private static final PPlayerData PLAYER_DATA_3 =
        new PPlayerData(UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f"), "thirdWheel", 30, 33, false, true);

    private static final IPWorld WORLD = new TestPWorld(WORLD_NAME);

    private static final Path DB_FILE;
    private static final Path DB_FILE_BACKUP;

    private SQLiteJDBCDriverConnection storage;

    private AbstractMovable movable1;
    private AbstractMovable movable2;
    private AbstractMovable movable3;

    static
    {
        LogInspector.get().clearHistory();
        DB_FILE = Path.of(".", "tests", "test.db");
        DB_FILE_BACKUP = DB_FILE.resolveSibling(DB_FILE.getFileName() + ".BACKUP");
    }

    private IPWorldFactory worldFactory;

    private MovableTypeManager movableTypeManager;

    private MovableBaseBuilder movableBaseBuilder;

    private MovableRegistry movableRegistry;

    @Mock
    private RestartableHolder restartableHolder;

    @Mock
    private DebuggableRegistry debuggableRegistry;

    @Mock
    private LocalizationManager localizationManager;

    @BeforeEach
    void beforeEach()
        throws NoSuchMethodException
    {
        MockitoAnnotations.openMocks(this);

        worldFactory = new TestPWorldFactory();
        movableRegistry = MovableRegistry.unCached(
            restartableHolder, debuggableRegistry, Mockito.mock(MovableDeletionManager.class));

        movableTypeManager = new MovableTypeManager(restartableHolder, debuggableRegistry, localizationManager);

        final AssistedFactoryMocker<MovableBase, MovableBase.IFactory> assistedFactoryMocker =
            new AssistedFactoryMocker<>(MovableBase.class, MovableBase.IFactory.class)
                .setMock(MovableRegistry.class, movableRegistry);

        movableBaseBuilder = new MovableBaseBuilder(assistedFactoryMocker.getFactory());

        initMovables();

        initStorage();
    }

    /**
     * Prepares files for a test run.
     */
    @BeforeAll
    public static void prepare()
    {
        try
        {
            Files.deleteIfExists(DB_FILE);
            Files.deleteIfExists(DB_FILE_BACKUP);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs cleanup after the tests. Remove leftovers from previous runs and store the finished databases of this run
     * (for debugging purposes).
     */
    @AfterAll
    public static void cleanup()
    {
        final Path finishedDB = DB_FILE.resolveSibling(DB_FILE.getFileName() + ".FINISHED");
        try
        {
            Files.move(DB_FILE, finishedDB, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception exception)
        {
            log.error("Failed to move database file to finished file.", exception);
        }
        try
        {
            Files.deleteIfExists(DB_FILE_BACKUP);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void deleteMovableTypes()
    {
        // Just make sure it still exists, to make debugging easier.
        Assertions.assertTrue(storage.getMovable(3L).isPresent());
        Assertions.assertTrue(storage.deleteMovableType(MovableTypePortcullis.get()));
        Assertions.assertTrue(storage.getMovable(1L).isPresent());
        Assertions.assertTrue(storage.getMovable(2L).isPresent());
        Assertions.assertFalse(storage.getMovable(3L).isPresent());
    }

    private void testMovableTypes()
    {
        deleteMovableTypes();
    }

    private void registerMovableTypes()
    {
        movableTypeManager.registerMovableType(MovableBigDoor.get());
        movableTypeManager.registerMovableType(MovableTypePortcullis.get());
        movableTypeManager.registerMovableType(MovableTypeDrawbridge.get());
    }

    /**
     * Runs all tests.
     */
    @Test
    void runTests()
        throws IllegalAccessException, NoSuchFieldException
    {
        registerMovableTypes();
        insertMovables();
        verifyMovables();
        partialIdentifiersFromName();
        auxiliaryMethods();
        modifyMovables();

        testMovableTypes();
        failures();

        insertBulkMovables();
        partialIdentifiersFromId();
    }

    private void insertBulkMovables()
    {
        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(movable3).isPresent());
    }

    /**
     * Tests inserting movables in the database.
     */
    public void insertMovables()
    {
        Assertions.assertTrue(storage.insert(movable1).isPresent());
        Assertions.assertTrue(storage.insert(movable2).isPresent());
        Assertions.assertTrue(storage.insert(movable3).isPresent());
    }

    /**
     * Checks if a movable was successfully added to the database and that all data in intact.
     *
     * @param movable
     *     The movable to verify.
     */
    private void testRetrieval(AbstractMovable movable)
    {
        Assertions.assertNotNull(storage);
        Assertions.assertNotNull(movable);
        Assertions.assertNotNull(movable.getPrimeOwner().toString());
        Assertions.assertNotNull(movable.getName());

        List<AbstractMovable> test = storage.getMovables(movable.getPrimeOwner().pPlayerData().getUUID(),
                                                         movable.getName());
        Assertions.assertEquals(1, test.size());

        Assertions.assertEquals(movable.getPrimeOwner(), test.get(0).getPrimeOwner());

        if (!movable.equals(test.get(0)))
            Assertions.fail(
                "Data of retrieved movable is not the same! ID = " + movable.getUid() + ", name = " +
                    movable.getName() +
                    ", found ID = " + test.get(0).getUid() + ", found name = " + test.get(0).getName());
    }

    /**
     * Verifies that the data of all movables that have been added to the database so far is correct.
     */
    public void verifyMovables()
    {
        testRetrieval(movable1);
        testRetrieval(movable2);
        testRetrieval(movable3);
    }

    public void partialIdentifiersFromName()
    {
        Assertions.assertEquals(List.of(new DatabaseManager.MovableIdentifier(2, "popular_door_name"),
                                        new DatabaseManager.MovableIdentifier(3, "popular_door_name")),
                                storage.getPartialIdentifiers("popular_", null, PermissionLevel.NO_PERMISSION));

        final IPPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(List.of(new DatabaseManager.MovableIdentifier(2, "popular_door_name")),
                                storage.getPartialIdentifiers("popular_", player1, PermissionLevel.NO_PERMISSION));
    }

    public void partialIdentifiersFromId()
    {
        Assertions.assertEquals(List.of(new DatabaseManager.MovableIdentifier(1, "random_door_name"),
                                        new DatabaseManager.MovableIdentifier(15, "popular_door_name"),
                                        new DatabaseManager.MovableIdentifier(16, "popular_door_name"),
                                        new DatabaseManager.MovableIdentifier(17, "popular_door_name"),
                                        new DatabaseManager.MovableIdentifier(18, "popular_door_name"),
                                        new DatabaseManager.MovableIdentifier(19, "popular_door_name")),
                                storage.getPartialIdentifiers("1", null, PermissionLevel.NO_PERMISSION));

        final IPPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(List.of(new DatabaseManager.MovableIdentifier(1, "random_door_name")),
                                storage.getPartialIdentifiers("1", player1, PermissionLevel.NO_PERMISSION));
    }

    /**
     * Tests the basic SQL methods.
     */
    public void auxiliaryMethods()
    {
        // Check simple methods.
        Assertions.assertEquals(1, storage.getMovableCountForPlayer(PLAYER_DATA_1.getUUID(), MOVABLE_1_NAME));
        Assertions.assertEquals(2, storage.getMovableCountForPlayer(PLAYER_DATA_1.getUUID()));
        Assertions.assertEquals(1, storage.getMovableCountForPlayer(PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getMovableCountByName(MOVABLE_1_NAME));
        Assertions.assertTrue(storage.getMovable(PLAYER_DATA_1.getUUID(), 1).isPresent());
        Assertions.assertEquals(movable1, storage.getMovable(PLAYER_DATA_1.getUUID(), 1).get());
        Assertions.assertFalse(storage.getMovable(PLAYER_DATA_1.getUUID(), 3).isPresent());
        final Optional<AbstractMovable> testMovable1 = storage.getMovable(1L);
        Assertions.assertTrue(testMovable1.isPresent());
        Assertions.assertEquals(movable1.getPrimeOwner(), testMovable1.get().getPrimeOwner());
        Assertions.assertEquals(movable1, testMovable1.get());
        Assertions.assertFalse(storage.getMovable(9999999).isPresent());
        Assertions.assertTrue(storage.isBigDoorsWorld(WORLD_NAME));
        Assertions.assertFalse(storage.isBigDoorsWorld("fakeWorld"));

        Assertions.assertEquals(1, storage.getOwnerCountOfMovable(1L));

        long chunkId = Util.getChunkId(movable1.getPowerBlock());
        Assertions.assertEquals(3, storage.getMovablesInChunk(chunkId).size());

        // Check if adding owners works correctly.
        UnitTestUtil.optionalEquals(1, storage.getMovable(1L), (movable) -> movable.getOwners().size());

        // Try adding playerData2 as owner of movable 2.
        Assertions.assertTrue(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.ADMIN));

        // Try adding player 1 as owner of movable 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, PLAYER_DATA_1, PermissionLevel.CREATOR));

        // Try adding player 2 as owner of movable 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.CREATOR));

        // Try adding a player that is not in the database yet as owner.
        UnitTestUtil.optionalEquals(1, storage.getMovable(1L), (movable) -> movable.getOwners().size());
        Assertions.assertTrue(storage.addOwner(1L, PLAYER_DATA_3, PermissionLevel.ADMIN));
        UnitTestUtil.optionalEquals(2, storage.getMovable(1L), (movable) -> movable.getOwners().size());

        // Verify the permission level of player 2 over movable 2.
        UnitTestUtil.optionalEquals(PermissionLevel.ADMIN, storage.getMovable(2L),
                                    (movable) -> movable.getOwner(PLAYER_DATA_2.getUUID())
                                                        .map(MovableOwner::permission)
                                                        .orElse(PermissionLevel.NO_PERMISSION));
        // Verify there are only 2 owners of movable 2 (player 1 didn't get copied).
        UnitTestUtil.optionalEquals(2, storage.getMovable(2L), (movable) -> movable.getOwners().size());

        // Verify that player 2 is the creator of exactly 1 movable.
        Assertions.assertEquals(1, storage.getMovables(PLAYER_DATA_2.getUUID(), PermissionLevel.CREATOR).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 movables (movable 3 (0) and movable 2 (1)).
        Assertions.assertEquals(2, storage.getMovables(PLAYER_DATA_2.getUUID(), PermissionLevel.ADMIN).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 movables,
        // with the name shared between movables 2 and 3.
        Assertions.assertEquals(2,
                                storage.getMovables(PLAYER_DATA_2.getUUID(), MOVABLES_2_3_NAME, PermissionLevel.ADMIN)
                                       .size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 movable,
        // with the name shared between movables 2 and 3.
        Assertions.assertEquals(1,
                                storage.getMovables(PLAYER_DATA_2.getUUID(), MOVABLES_2_3_NAME, PermissionLevel.CREATOR)
                                       .size());

        // Verify that adding an existing owner overrides the permission level.
        Assertions.assertTrue(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.USER));
        UnitTestUtil.optionalEquals(PermissionLevel.USER, storage.getMovable(2L),
                                    (movable) -> movable.getOwner(PLAYER_DATA_2.getUUID())
                                                        .map(MovableOwner::permission)
                                                        .orElse(PermissionLevel.NO_PERMISSION));

        // Remove player 2 as owner of movable 2.
        Assertions.assertTrue(storage.removeOwner(2L, PLAYER_DATA_2.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getMovable(2L), (movable) -> movable.getOwners().size());

        // Try to remove player 1 (creator) of movable 2. This is not allowed.
        Assertions.assertFalse(storage.removeOwner(2L, PLAYER_DATA_1.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getMovable(2L), (movable) -> movable.getOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 movable, with the name shared between movables 2 and 3. This will be movable 3.
        Assertions.assertEquals(1,
                                storage.getMovables(PLAYER_DATA_2.getUUID(), MOVABLES_2_3_NAME, PermissionLevel.ADMIN)
                                       .size());

        // Verify that player 1 is owner of exactly 1 movable with the name shared between movables 2 and 3.
        Assertions.assertEquals(1, storage.getMovables(PLAYER_DATA_1.getUUID(), MOVABLES_2_3_NAME).size());

        // Verify that player 1 owns exactly 2 movables.
        Assertions.assertEquals(2, storage.getMovables(PLAYER_DATA_1.getUUID()).size());

        // Verify that there are exactly 2 movables with the name shared between movables 2 and 3 in the database.
        Assertions.assertEquals(2, storage.getMovables(MOVABLES_2_3_NAME).size());

        // Insert a copy of movable 1 in the database (will have movableUID = 4).
        Assertions.assertTrue(storage.insert(movable1).isPresent());

        // Verify there are now exactly 2 movables with the name of movable 1 in the database.
        Assertions.assertEquals(2, storage.getMovables(MOVABLE_1_NAME).size());

        // Remove the just-added copy of movable 1 (movableUID = 4) from the database.
        Assertions.assertTrue(storage.removeMovable(4L));

        // Verify that after removal of the copy of movable 1 (movableUID = 4), there is now exactly 1 movable named
        // MOVABLE_1_NAME in the database again.
        Assertions.assertEquals(1, storage.getMovables(MOVABLE_1_NAME).size());

        // Verify that player 2 cannot delete movables they do not own (movable 1 belongs to player 1).
        Assertions.assertFalse(storage.removeOwner(1L, PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getMovables(MOVABLE_1_NAME).size());

        // Add 10 copies of movable3 with a different name to the database.
        movable3.setName(DELETE_MOVABLE_NAME);
        // Verify there are currently exactly 0 movables with this different name in the database.
        Assertions.assertEquals(0, storage.getMovables(DELETE_MOVABLE_NAME).size());

        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(movable3).isPresent());

        // Verify there are now exactly 10 movables with this different name in the database.
        Assertions.assertEquals(10, storage.getMovables(DELETE_MOVABLE_NAME).size());

        // Remove all 10 movables we just added (owned by player 2) and verify there are exactly 0 entries of the movable with
        // the new name after batch removal. Also revert the name change of movable 3.
        Assertions.assertTrue(storage.removeMovables(PLAYER_DATA_2.getUUID(), DELETE_MOVABLE_NAME));
        Assertions.assertEquals(0, storage.getMovables(DELETE_MOVABLE_NAME).size());
        Assertions.assertTrue(storage.getMovable(3L).isPresent());
        movable3.setName(storage.getMovable(3L).get().getName());


        // Make sure the player name corresponds to the correct UUID.
        Assertions.assertTrue(storage.getPlayerData(PLAYER_DATA_2.getUUID()).isPresent());
        Assertions.assertEquals(PLAYER_DATA_2, storage.getPlayerData(PLAYER_DATA_2.getUUID()).get());
        Assertions.assertEquals(1, storage.getPlayerData(PLAYER_DATA_2.getName()).size());
        Assertions.assertEquals(PLAYER_DATA_2, storage.getPlayerData(PLAYER_DATA_2.getName()).get(0));
        Assertions.assertEquals(0, storage.getPlayerData(PLAYER_2_NAME_ALT).size());
        Assertions.assertEquals(PLAYER_DATA_2, storage.getPlayerData(PLAYER_DATA_2.getUUID()).get());

        // Update player 2's name to their alt name and make sure the old name is gone and the new one is reachable.
        final PPlayerData playerData2ALT =
            new PPlayerData(UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed"), PLAYER_2_NAME_ALT,
                            20, 22, true, false);

        Assertions.assertTrue(storage.updatePlayerData(playerData2ALT));
        UnitTestUtil.optionalEquals(playerData2ALT, storage.getPlayerData(PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(0, storage.getPlayerData(PLAYER_DATA_2.getName()).size());
        Assertions.assertEquals(1, storage.getPlayerData(playerData2ALT.getName()).size());

        // Revert name change of player 2.
        Assertions.assertTrue(storage.updatePlayerData(PLAYER_DATA_2));

        chunkId = Util.getChunkId(movable1.getPowerBlock());
        final ConcurrentHashMap<Integer, List<Long>> powerBlockData = storage.getPowerBlockData(chunkId);
        Assertions.assertNotNull(powerBlockData);
        Assertions.assertEquals(3, powerBlockData.elements().nextElement().size());
    }

    /**
     * Runs tests of the methods that modify movables in the database.
     */
    public void modifyMovables()
    {
        MovableSerializer<?> serializer =
            Assertions.assertDoesNotThrow(() -> new MovableSerializer<>(movable3.getType().getMovableClass()));
        Assertions.assertNotNull(serializer);

        // Test changing autoCloseTime value.  (i.e. syncing type-specific data).
        {
            ITimerToggleable timerToggleable = (ITimerToggleable) movable3;
            final int movable3AutoCloseTime = timerToggleable.getAutoCloseTime();
            final int testAutoCloseTime = 20;

            timerToggleable.setAutoCloseTime(testAutoCloseTime);
            Assertions.assertTrue(storage.syncMovableData(movable3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(movable3))));
            UnitTestUtil.optionalEquals(testAutoCloseTime, storage.getMovable(3L),
                                        (movable) -> ((ITimerToggleable) movable).getAutoCloseTime());

            timerToggleable.setAutoCloseTime(movable3AutoCloseTime);
            Assertions.assertTrue(storage.syncMovableData(movable3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(movable3))));

            UnitTestUtil.optionalEquals(movable3AutoCloseTime, storage.getMovable(3L),
                                        (movable) -> ((ITimerToggleable) movable).getAutoCloseTime());

            UnitTestUtil.optionalEquals(movable3, storage.getMovable(3L));
        }

        // Test (un)locking (i.e. syncing base data).
        {
            movable3.setLocked(true);
            Assertions.assertTrue(storage.syncMovableData(movable3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(movable3))));
            UnitTestUtil.optionalEquals(true, storage.getMovable(3L), AbstractMovable::isLocked);

            movable3.setLocked(false);
            Assertions.assertTrue(storage.syncMovableData(movable3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(movable3))));
            UnitTestUtil.optionalEquals(false, storage.getMovable(3L), AbstractMovable::isLocked);
        }

        // Test syncing all data.
        {
            Portcullis pc = ((Portcullis) movable3);

            // Save the current data
            final RotateDirection oldDir = movable3.getOpenDir();
            final RotateDirection newDir = RotateDirection.getOpposite(oldDir);
            Assertions.assertNotSame(oldDir, newDir);

            final Vector3Di oldPowerBlock = movable3.getPowerBlock();
            final Vector3Di newPowerBlock = new Vector3Di(oldPowerBlock.x(),
                                                          (oldPowerBlock.x() + 30) % 256,
                                                          oldPowerBlock.z());

            final Vector3Di oldMin = movable3.getMinimum();
            final Vector3Di oldMax = movable3.getMaximum();
            final Vector3Di newMin = oldMin.add(0, 20, 10);
            final Vector3Di newMax = oldMax.add(40, 0, 20);
            Assertions.assertNotSame(oldMin, newMin);
            Assertions.assertNotSame(oldMax, newMax);

            final boolean isLocked = movable3.isLocked();
            final boolean isOpen = movable3.isOpen();


            // update some general data.
            movable3.setLocked(!isLocked);
            movable3.setOpen(!isOpen);
            movable3.setPowerBlock(newPowerBlock);
            movable3.setCoordinates(newMin, newMax);
            movable3.setOpenDir(newDir);


            // Update some type-specific data
            final int blocksToMove = pc.getBlocksToMove();
            final int newBlocksToMove = blocksToMove * 2;
            Assertions.assertNotSame(0, blocksToMove);
            pc.setBlocksToMove(newBlocksToMove);

            Assertions.assertTrue(storage.syncMovableData(movable3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(movable3))));

            Optional<AbstractMovable> retrievedOpt = storage.getMovable(3L);
            Assertions.assertTrue(retrievedOpt.isPresent());
            Portcullis retrieved = (Portcullis) retrievedOpt.get();

            // Check base data
            Assertions.assertEquals(!isLocked, retrieved.isLocked());
            Assertions.assertEquals(!isOpen, retrieved.isOpen());
            Assertions.assertEquals(newPowerBlock, retrieved.getPowerBlock());
            Assertions.assertEquals(newMin, retrieved.getMinimum());
            Assertions.assertEquals(newMax, retrieved.getMaximum());
            Assertions.assertEquals(newDir, retrieved.getOpenDir());

            // Check type-specific data
            Assertions.assertEquals(blocksToMove * 2, retrieved.getBlocksToMove());


            // reset base data
            movable3.setLocked(isLocked);
            movable3.setOpen(isOpen);
            movable3.setPowerBlock(oldPowerBlock);
            movable3.setCoordinates(oldMin, oldMax);
            movable3.setOpenDir(oldDir);

            // Reset type-specific data
            pc.setBlocksToMove(blocksToMove);

            Assertions.assertTrue(storage.syncMovableData(movable3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(movable3))));
        }
    }

    /**
     * Runs tests to verify that exceptions are caught when they should be and properly handled.
     */
    public void failures()
        throws NoSuchFieldException, IllegalAccessException
    {
        // Set the enabled status of the database to false.
        final Field databaseLock = SQLiteJDBCDriverConnection.class.getDeclaredField("databaseState");
        databaseLock.setAccessible(true);
        databaseLock.set(storage, IStorage.DatabaseState.ERROR);

        AssertionsUtil.assertThrowablesLogged(() -> storage.getMovable(PLAYER_DATA_1.getUUID(), 1L),
                                              LogSiteStackTrace.class);

        // Set the database state to enabled again and verify that it's now possible to retrieve movables again.
        databaseLock.set(storage, IStorage.DatabaseState.OK);
        Assertions.assertTrue(storage.getMovable(PLAYER_DATA_1.getUUID(), 1L).isPresent());
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
    {
        storage = new SQLiteJDBCDriverConnection(DB_FILE, movableBaseBuilder, movableRegistry, movableTypeManager,
                                                 worldFactory,
                                                 debuggableRegistry);
    }

    private void initMovables()
    {
        Vector3Di min = new Vector3Di(144, 75, 153);
        Vector3Di max = new Vector3Di(144, 131, 167);
        Vector3Di powerBlock = new Vector3Di(144, 75, 153);
        Vector3Di rotationPoint = new Vector3Di(144, 75, 153);
        int autoOpen = 0;
        int autoClose = 0;
        movable1 = new BigDoor(movableBaseBuilder.builder()
                                                 .uid(1).name(MOVABLE_1_NAME).cuboid(min, max)
                                                 .rotationPoint(rotationPoint)
                                                 .powerBlock(powerBlock)
                                                 .world(WORLD).isOpen(false).isLocked(false)
                                                 .openDir(RotateDirection.EAST)
                                                 .primeOwner(
                                                     new MovableOwner(1, PermissionLevel.CREATOR, PLAYER_DATA_1))
                                                 .build(),
                               autoClose, autoOpen);


        min = new Vector3Di(144, 75, 168);
        max = new Vector3Di(144, 131, 182);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);
        autoOpen = 10;
        autoClose = -1;
        boolean modeUp = true;
        movable2 = new Drawbridge(movableBaseBuilder.builder()
                                                    .uid(2).name(MOVABLES_2_3_NAME).cuboid(min, max)
                                                    .rotationPoint(rotationPoint)
                                                    .powerBlock(powerBlock).world(WORLD).isOpen(false)
                                                    .isLocked(false).openDir(RotateDirection.NONE)
                                                    .primeOwner(
                                                        new MovableOwner(2, PermissionLevel.CREATOR, PLAYER_DATA_1))
                                                    .build(),
                                  autoClose, autoOpen, modeUp);


        min = new Vector3Di(144, 70, 168);
        max = new Vector3Di(144, 151, 112);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);
        autoOpen = 0;
        autoClose = 3;
        int blocksToMove = 8;
        movable3 = new Portcullis(movableBaseBuilder.builder()
                                                    .uid(3).name(MOVABLES_2_3_NAME).cuboid(min, max)
                                                    .rotationPoint(rotationPoint)
                                                    .powerBlock(powerBlock).world(WORLD).isOpen(false)
                                                    .isLocked(false).openDir(RotateDirection.UP)
                                                    .primeOwner(
                                                        new MovableOwner(3, PermissionLevel.CREATOR, PLAYER_DATA_2))
                                                    .build(),
                                  blocksToMove, autoClose, autoOpen);
    }

    private static IPPlayer createPlayer(PPlayerData data)
    {
        final IPPlayer player = Mockito.mock(IPPlayer.class);
        Mockito.when(player.getName()).thenReturn(data.getName());
        Mockito.when(player.getUUID()).thenReturn(data.getUUID());
        Mockito.when(player.getPPlayerData()).thenReturn(data);
        Mockito.when(player.getLocation()).thenReturn(Optional.empty());
        return player;
    }
}
