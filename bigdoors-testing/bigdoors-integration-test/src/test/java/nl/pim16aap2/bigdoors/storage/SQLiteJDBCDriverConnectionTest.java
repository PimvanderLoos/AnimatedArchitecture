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
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorBaseBuilder;
import nl.pim16aap2.bigdoors.doors.DoorOwner;
import nl.pim16aap2.bigdoors.doors.DoorSerializer;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.doors.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.doors.bigdoor.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doors.drawbridge.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.doors.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.doors.portcullis.DoorTypePortcullis;
import nl.pim16aap2.bigdoors.doors.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
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
     * Name of door 1.
     */
    private static final String DOOR_1_NAME = "random_door_name";

    /**
     * Name of doors 2 and 3.
     */
    private static final String DOORS_2_3_NAME = "popular_door_name";

    private static final String DELETE_DOOR_NAME = "delete_meh";

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

    private AbstractDoor door1;
    private AbstractDoor door2;
    private AbstractDoor door3;

    static
    {
        LogInspector.get().clearHistory();
        DB_FILE = Path.of(".", "tests", "test.db");
        DB_FILE_BACKUP = DB_FILE.resolveSibling(DB_FILE.getFileName() + ".BACKUP");
    }

    private IPWorldFactory worldFactory;

    private DoorTypeManager doorTypeManager;

    private DoorBaseBuilder doorBaseBuilder;

    private DoorRegistry doorRegistry;

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
        doorRegistry = DoorRegistry.unCached(restartableHolder, debuggableRegistry);
        doorTypeManager = new DoorTypeManager(restartableHolder, debuggableRegistry, localizationManager);

        final AssistedFactoryMocker<DoorBase, DoorBase.IFactory> assistedFactoryMocker =
            new AssistedFactoryMocker<>(DoorBase.class, DoorBase.IFactory.class)
                .setMock(DoorRegistry.class, doorRegistry);

        doorBaseBuilder = new DoorBaseBuilder(assistedFactoryMocker.getFactory());

        initDoors();

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

    private void deleteDoorTypeTestDoors()
    {
        // Just make sure it still exists, to make debugging easier.
        Assertions.assertTrue(storage.getDoor(3L).isPresent());
        Assertions.assertTrue(storage.deleteDoorType(DoorTypePortcullis.get()));
        Assertions.assertTrue(storage.getDoor(1L).isPresent());
        Assertions.assertTrue(storage.getDoor(2L).isPresent());
        Assertions.assertFalse(storage.getDoor(3L).isPresent());
    }

    private void testDoorTypes()
    {
        deleteDoorTypeTestDoors();
    }

    private void registerDoorTypes()
    {
        doorTypeManager.registerDoorType(DoorTypeBigDoor.get());
        doorTypeManager.registerDoorType(DoorTypePortcullis.get());
        doorTypeManager.registerDoorType(DoorTypeDrawbridge.get());
    }

    /**
     * Runs all tests.
     */
    @Test
    void runTests()
        throws IllegalAccessException, NoSuchFieldException
    {
        registerDoorTypes();
        insertDoors();
        verifyDoors();
        partialIdentifiersFromName();
        auxiliaryMethods();
        modifyDoors();

        testDoorTypes();
        failures();

        insertBulkDoors();
        partialIdentifiersFromId();
    }

    private void insertBulkDoors()
    {
        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(door3).isPresent());
    }

    /**
     * Tests inserting doors in the database.
     */
    public void insertDoors()
    {
        Assertions.assertTrue(storage.insert(door1).isPresent());
        Assertions.assertTrue(storage.insert(door2).isPresent());
        Assertions.assertTrue(storage.insert(door3).isPresent());
    }

    /**
     * Checks if a door was successfully added to the database and that all data in intact.
     *
     * @param door
     *     The door to verify.
     */
    private void testRetrieval(AbstractDoor door)
    {
        Assertions.assertNotNull(storage);
        Assertions.assertNotNull(door);
        Assertions.assertNotNull(door.getPrimeOwner().toString());
        Assertions.assertNotNull(door.getName());

        List<AbstractDoor> test = storage.getDoors(door.getPrimeOwner().pPlayerData().getUUID(), door.getName());
        Assertions.assertEquals(1, test.size());

        Assertions.assertEquals(door.getPrimeOwner(), test.get(0).getPrimeOwner());

        if (!door.equals(test.get(0)))
            Assertions.fail(
                "Data of retrieved door is not the same! ID = " + door.getDoorUID() + ", name = " + door.getName() +
                    ", found ID = " + test.get(0).getDoorUID() + ", found name = " + test.get(0).getName());
    }

    /**
     * Verifies that the data of all doors that have been added to the database so far is correct.
     */
    public void verifyDoors()
    {
        testRetrieval(door1);
        testRetrieval(door2);
        testRetrieval(door3);
    }

    public void partialIdentifiersFromName()
    {
        Assertions.assertEquals(List.of(new DatabaseManager.DoorIdentifier(2, "popular_door_name"),
                                        new DatabaseManager.DoorIdentifier(3, "popular_door_name")),
                                storage.getPartialIdentifiers("popular_", null, PermissionLevel.NO_PERMISSION));

        final IPPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(List.of(new DatabaseManager.DoorIdentifier(2, "popular_door_name")),
                                storage.getPartialIdentifiers("popular_", player1, PermissionLevel.NO_PERMISSION));
    }

    public void partialIdentifiersFromId()
    {
        Assertions.assertEquals(List.of(new DatabaseManager.DoorIdentifier(1, "random_door_name"),
                                        new DatabaseManager.DoorIdentifier(15, "popular_door_name"),
                                        new DatabaseManager.DoorIdentifier(16, "popular_door_name"),
                                        new DatabaseManager.DoorIdentifier(17, "popular_door_name"),
                                        new DatabaseManager.DoorIdentifier(18, "popular_door_name"),
                                        new DatabaseManager.DoorIdentifier(19, "popular_door_name")),
                                storage.getPartialIdentifiers("1", null, PermissionLevel.NO_PERMISSION));

        final IPPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(List.of(new DatabaseManager.DoorIdentifier(1, "random_door_name")),
                                storage.getPartialIdentifiers("1", player1, PermissionLevel.NO_PERMISSION));
    }

    /**
     * Tests the basic SQL methods.
     */
    public void auxiliaryMethods()
    {
        // Check simple methods.
        Assertions.assertEquals(1, storage.getDoorCountForPlayer(PLAYER_DATA_1.getUUID(), DOOR_1_NAME));
        Assertions.assertEquals(2, storage.getDoorCountForPlayer(PLAYER_DATA_1.getUUID()));
        Assertions.assertEquals(1, storage.getDoorCountForPlayer(PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getDoorCountByName(DOOR_1_NAME));
        Assertions.assertTrue(storage.getDoor(PLAYER_DATA_1.getUUID(), 1).isPresent());
        Assertions.assertEquals(door1, storage.getDoor(PLAYER_DATA_1.getUUID(), 1).get());
        Assertions.assertFalse(storage.getDoor(PLAYER_DATA_1.getUUID(), 3).isPresent());
        final Optional<AbstractDoor> testDoor1 = storage.getDoor(1L);
        Assertions.assertTrue(testDoor1.isPresent());
        Assertions.assertEquals(door1.getPrimeOwner(), testDoor1.get().getPrimeOwner());
        Assertions.assertEquals(door1, testDoor1.get());
        Assertions.assertFalse(storage.getDoor(9999999).isPresent());
        Assertions.assertTrue(storage.isBigDoorsWorld(WORLD_NAME));
        Assertions.assertFalse(storage.isBigDoorsWorld("fakeWorld"));

        Assertions.assertEquals(1, storage.getOwnerCountOfDoor(1L));

        long chunkId = Util.getChunkId(door1.getPowerBlock());
        Assertions.assertEquals(3, storage.getDoorsInChunk(chunkId).size());

        // Check if adding owners works correctly.
        UnitTestUtil.optionalEquals(1, storage.getDoor(1L), (door) -> door.getDoorOwners().size());

        // Try adding playerData2 as owner of door 2.
        Assertions.assertTrue(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.ADMIN));

        // Try adding player 1 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, PLAYER_DATA_1, PermissionLevel.CREATOR));

        // Try adding player 2 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.CREATOR));

        // Try adding a player that is not in the database yet as owner.
        UnitTestUtil.optionalEquals(1, storage.getDoor(1L), (door) -> door.getDoorOwners().size());
        Assertions.assertTrue(storage.addOwner(1L, PLAYER_DATA_3, PermissionLevel.ADMIN));
        UnitTestUtil.optionalEquals(2, storage.getDoor(1L), (door) -> door.getDoorOwners().size());

        // Verify the permission level of player 2 over door 2.
        UnitTestUtil.optionalEquals(PermissionLevel.ADMIN, storage.getDoor(2L),
                                    (door) -> door.getDoorOwner(PLAYER_DATA_2.getUUID()).map(DoorOwner::permission)
                                                  .orElse(PermissionLevel.NO_PERMISSION));
        // Verify there are only 2 owners of door 2 (player 1 didn't get copied).
        UnitTestUtil.optionalEquals(2, storage.getDoor(2L), (door) -> door.getDoorOwners().size());

        // Verify that player 2 is the creator of exactly 1 door.
        Assertions.assertEquals(1, storage.getDoors(PLAYER_DATA_2.getUUID(), PermissionLevel.CREATOR).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors (door 3 (0) and door 2 (1)).
        Assertions.assertEquals(2, storage.getDoors(PLAYER_DATA_2.getUUID(), PermissionLevel.ADMIN).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors,
        // with the name shared between doors 2 and 3.
        Assertions.assertEquals(2, storage.getDoors(PLAYER_DATA_2.getUUID(), DOORS_2_3_NAME, PermissionLevel.ADMIN)
                                          .size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 door,
        // with the name shared between doors 2 and 3.
        Assertions.assertEquals(1, storage.getDoors(PLAYER_DATA_2.getUUID(), DOORS_2_3_NAME, PermissionLevel.CREATOR)
                                          .size());

        // Verify that adding an existing owner overrides the permission level.
        Assertions.assertTrue(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.USER));
        UnitTestUtil.optionalEquals(PermissionLevel.USER, storage.getDoor(2L),
                                    (door) -> door.getDoorOwner(PLAYER_DATA_2.getUUID()).map(DoorOwner::permission)
                                                  .orElse(PermissionLevel.NO_PERMISSION));

        // Remove player 2 as owner of door 2.
        Assertions.assertTrue(storage.removeOwner(2L, PLAYER_DATA_2.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getDoor(2L), (door) -> door.getDoorOwners().size());

        // Try to remove player 1 (creator) of door 2. This is not allowed.
        Assertions.assertFalse(storage.removeOwner(2L, PLAYER_DATA_1.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getDoor(2L), (door) -> door.getDoorOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 door, with the name shared between doors 2 and 3. This will be door 3.
        Assertions.assertEquals(1, storage.getDoors(PLAYER_DATA_2.getUUID(), DOORS_2_3_NAME, PermissionLevel.ADMIN)
                                          .size());

        // Verify that player 1 is owner of exactly 1 door with the name shared between doors 2 and 3.
        Assertions.assertEquals(1, storage.getDoors(PLAYER_DATA_1.getUUID(), DOORS_2_3_NAME).size());

        // Verify that player 1 owns exactly 2 doors.
        Assertions.assertEquals(2, storage.getDoors(PLAYER_DATA_1.getUUID()).size());

        // Verify that there are exactly 2 doors with the name shared between doors 2 and 3 in the database.
        Assertions.assertEquals(2, storage.getDoors(DOORS_2_3_NAME).size());

        // Insert a copy of door 1 in the database (will have doorUID = 4).
        Assertions.assertTrue(storage.insert(door1).isPresent());

        // Verify there are now exactly 2 doors with the name of door 1 in the database.
        Assertions.assertEquals(2, storage.getDoors(DOOR_1_NAME).size());

        // Remove the just-added copy of door 1 (doorUID = 4) from the database.
        Assertions.assertTrue(storage.removeDoor(4L));

        // Verify that after removal of the copy of door 1 (doorUID = 4), there is now exactly 1 door named
        // DOOR_1_NAME in the database again.
        Assertions.assertEquals(1, storage.getDoors(DOOR_1_NAME).size());

        // Verify that player 2 cannot delete doors they do not own (door 1 belongs to player 1).
        Assertions.assertFalse(storage.removeOwner(1L, PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getDoors(DOOR_1_NAME).size());

        // Add 10 copies of door3 with a different name to the database.
        door3.setName(DELETE_DOOR_NAME);
        // Verify there are currently exactly 0 doors with this different name in the database.
        Assertions.assertEquals(0, storage.getDoors(DELETE_DOOR_NAME).size());

        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(door3).isPresent());

        // Verify there are now exactly 10 doors with this different name in the database.
        Assertions.assertEquals(10, storage.getDoors(DELETE_DOOR_NAME).size());

        // Remove all 10 doors we just added (owned by player 2) and verify there are exactly 0 entries of the door with
        // the new name after batch removal. Also revert the name change of door 3.
        Assertions.assertTrue(storage.removeDoors(PLAYER_DATA_2.getUUID(), DELETE_DOOR_NAME));
        Assertions.assertEquals(0, storage.getDoors(DELETE_DOOR_NAME).size());
        Assertions.assertTrue(storage.getDoor(3L).isPresent());
        door3.setName(storage.getDoor(3L).get().getName());


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

        chunkId = Util.getChunkId(door1.getPowerBlock());
        final ConcurrentHashMap<Integer, List<Long>> powerBlockData = storage.getPowerBlockData(chunkId);
        Assertions.assertNotNull(powerBlockData);
        Assertions.assertEquals(3, powerBlockData.elements().nextElement().size());
    }

    /**
     * Runs tests of the methods that modify doors in the database.
     */
    public void modifyDoors()
    {
        DoorSerializer<?> serializer =
            Assertions.assertDoesNotThrow(() -> new DoorSerializer<>(door3.getDoorType().getDoorClass()));
        Assertions.assertNotNull(serializer);

        // Test changing autoCloseTime value.  (i.e. syncing type-specific data).
        {
            ITimerToggleable doorTimeToggle = (ITimerToggleable) door3;
            final int door3AutoCloseTime = doorTimeToggle.getAutoCloseTime();
            final int testAutoCloseTime = 20;

            doorTimeToggle.setAutoCloseTime(testAutoCloseTime);
            Assertions.assertTrue(storage.syncDoorData(door3.getDoorBase().getPartialSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
            UnitTestUtil.optionalEquals(testAutoCloseTime, storage.getDoor(3L),
                                        (door) -> ((ITimerToggleable) door).getAutoCloseTime());

            doorTimeToggle.setAutoCloseTime(door3AutoCloseTime);
            Assertions.assertTrue(storage.syncDoorData(door3.getDoorBase().getPartialSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));

            UnitTestUtil.optionalEquals(door3AutoCloseTime, storage.getDoor(3L),
                                        (door) -> ((ITimerToggleable) door).getAutoCloseTime());

            UnitTestUtil.optionalEquals(door3, storage.getDoor(3L));
        }

        // Test (un)locking (i.e. syncing base data).
        {
            door3.setLocked(true);
            Assertions.assertTrue(storage.syncDoorData(door3.getDoorBase().getPartialSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
            UnitTestUtil.optionalEquals(true, storage.getDoor(3L), AbstractDoor::isLocked);

            door3.setLocked(false);
            Assertions.assertTrue(storage.syncDoorData(door3.getDoorBase().getPartialSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
            UnitTestUtil.optionalEquals(false, storage.getDoor(3L), AbstractDoor::isLocked);
        }

        // Test syncing all data.
        {
            Portcullis pc = ((Portcullis) door3);

            // Save the current data
            final RotateDirection oldDir = door3.getOpenDir();
            final RotateDirection newDir = RotateDirection.getOpposite(oldDir);
            Assertions.assertNotSame(oldDir, newDir);

            final Vector3Di oldPowerBlock = door3.getPowerBlock();
            final Vector3Di newPowerBlock = new Vector3Di(oldPowerBlock.x(),
                                                          (oldPowerBlock.x() + 30) % 256,
                                                          oldPowerBlock.z());

            final Vector3Di oldMin = door3.getMinimum();
            final Vector3Di oldMax = door3.getMaximum();
            final Vector3Di newMin = oldMin.add(0, 20, 10);
            final Vector3Di newMax = oldMax.add(40, 0, 20);
            Assertions.assertNotSame(oldMin, newMin);
            Assertions.assertNotSame(oldMax, newMax);

            final boolean isLocked = door3.isLocked();
            final boolean isOpen = door3.isOpen();


            // update some general data.
            door3.setLocked(!isLocked);
            door3.setOpen(!isOpen);
            door3.setPowerBlock(newPowerBlock);
            door3.setCoordinates(newMin, newMax);
            door3.setOpenDir(newDir);


            // Update some type-specific data
            final int blocksToMove = pc.getBlocksToMove();
            final int newBlocksToMove = blocksToMove * 2;
            Assertions.assertNotSame(0, blocksToMove);
            pc.setBlocksToMove(newBlocksToMove);

            Assertions.assertTrue(storage.syncDoorData(door3.getDoorBase().getPartialSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));

            Optional<AbstractDoor> retrievedOpt = storage.getDoor(3L);
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
            door3.setLocked(isLocked);
            door3.setOpen(isOpen);
            door3.setPowerBlock(oldPowerBlock);
            door3.setCoordinates(oldMin, oldMax);
            door3.setOpenDir(oldDir);

            // Reset type-specific data
            pc.setBlocksToMove(blocksToMove);

            Assertions.assertTrue(storage.syncDoorData(door3.getDoorBase().getPartialSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
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

        AssertionsUtil.assertThrowablesLogged(() -> storage.getDoor(PLAYER_DATA_1.getUUID(), 1L),
                                              LogSiteStackTrace.class);

        // Set the database state to enabled again and verify that it's now possible to retrieve doors again.
        databaseLock.set(storage, IStorage.DatabaseState.OK);
        Assertions.assertTrue(storage.getDoor(PLAYER_DATA_1.getUUID(), 1L).isPresent());
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
    {
        storage = new SQLiteJDBCDriverConnection(DB_FILE, doorBaseBuilder, doorRegistry, doorTypeManager, worldFactory,
                                                 debuggableRegistry);
    }

    private void initDoors()
    {
        Vector3Di min = new Vector3Di(144, 75, 153);
        Vector3Di max = new Vector3Di(144, 131, 167);
        Vector3Di powerBlock = new Vector3Di(144, 75, 153);
        Vector3Di rotationPoint = new Vector3Di(144, 75, 153);
        int autoOpen = 0;
        int autoClose = 0;
        door1 = new BigDoor(doorBaseBuilder.builder()
                                           .uid(1).name(DOOR_1_NAME).cuboid(min, max).rotationPoint(rotationPoint)
                                           .powerBlock(powerBlock)
                                           .world(WORLD).isOpen(false).isLocked(false).openDir(RotateDirection.EAST)
                                           .primeOwner(new DoorOwner(1, PermissionLevel.CREATOR, PLAYER_DATA_1))
                                           .build(),
                            autoClose, autoOpen);


        min = new Vector3Di(144, 75, 168);
        max = new Vector3Di(144, 131, 182);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);
        autoOpen = 10;
        autoClose = -1;
        boolean modeUp = true;
        door2 = new Drawbridge(doorBaseBuilder.builder()
                                              .uid(2).name(DOORS_2_3_NAME).cuboid(min, max).rotationPoint(rotationPoint)
                                              .powerBlock(powerBlock).world(WORLD).isOpen(false)
                                              .isLocked(false).openDir(RotateDirection.NONE)
                                              .primeOwner(new DoorOwner(2, PermissionLevel.CREATOR, PLAYER_DATA_1))
                                              .build(),
                               autoClose, autoOpen, modeUp);


        min = new Vector3Di(144, 70, 168);
        max = new Vector3Di(144, 151, 112);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);
        autoOpen = 0;
        autoClose = 3;
        int blocksToMove = 8;
        door3 = new Portcullis(doorBaseBuilder.builder()
                                              .uid(3).name(DOORS_2_3_NAME).cuboid(min, max).rotationPoint(rotationPoint)
                                              .powerBlock(powerBlock).world(WORLD).isOpen(false)
                                              .isLocked(false).openDir(RotateDirection.UP)
                                              .primeOwner(new DoorOwner(3, PermissionLevel.CREATOR, PLAYER_DATA_2))
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
