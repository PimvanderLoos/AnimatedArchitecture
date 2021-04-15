package nl.pim16aap2.bigdoors.storage;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorSerializer;
import nl.pim16aap2.bigdoors.doors.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.doors.bigdoor.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doors.drawbridge.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.doors.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.doors.portcullis.DoorTypePortcullis;
import nl.pim16aap2.bigdoors.doors.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorldFactory;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteJDBCDriverConnectionTest
{
    private static final @NonNull String DELETEDOORNAME = "deletemeh";

    private static final @NonNull String worldName = "TestWorld";

    private static final @NonNull String player2NameALT = "TestMan";

    private static final @NonNull PPlayerData playerData1 =
        new PPlayerData(UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"), "pim16aap2", 10, 11, true, true);

    private static final @NonNull PPlayerData playerData2 =
        new PPlayerData(UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed"), "TestBoiii", 20, 22, true, false);

    private static final @NonNull PPlayerData playerData3 =
        new PPlayerData(UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f"), "thirdWheel", 30, 33, false, true);

    @NonNull
    private static final IPWorld world = new TestPWorld(worldName);

    private static AbstractDoorBase door1;
    private static AbstractDoorBase door2;
    private static AbstractDoorBase door3;

    private static final File DB_FILE;
    private static final File dbFileBackup;
    private static SQLiteJDBCDriverConnection storage;

    @Mock
    protected IBigDoorsPlatform platform;

    // Initialize files.
    static
    {
        DB_FILE = new File("./tests/test.db");
        dbFileBackup = new File(DB_FILE.toString() + ".BACKUP");
    }

    @BeforeAll
    public static void baseSetup()
    {
        AbstractDoorBase.DoorData doorData;
        {
            final int doorUID = 1;
            final int autoOpen = 0;
            final int autoClose = 0;
            final boolean isOpen = false;
            final boolean isLocked = false;
            final @NonNull String name = "massive1";
            final @NonNull Vector3Di min = new Vector3Di(144, 75, 153);
            final @NonNull Vector3Di max = new Vector3Di(144, 131, 167);
            final @NonNull Vector3Di engine = new Vector3Di(144, 75, 153);
            final @NonNull Vector3Di powerBlock = new Vector3Di(144, 75, 153);
            final @NonNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData1);

            doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                     isLocked, RotateDirection.EAST, doorOwner);
            final @NonNull BigDoor bigDoor = new BigDoor(doorData, autoClose, autoOpen);
            door1 = bigDoor;
        }

        {
            final int doorUID = 2;
            final int autoOpen = 10;
            final int autoClose = -1;
            final boolean modeUp = true;
            final boolean isOpen = false;
            final boolean isLocked = false;
            final @NonNull String name = "massive2";
            final @NonNull Vector3Di min = new Vector3Di(144, 75, 168);
            final @NonNull Vector3Di max = new Vector3Di(144, 131, 182);
            final @NonNull Vector3Di engine = new Vector3Di(144, 75, 153);
            final @NonNull Vector3Di powerBlock = new Vector3Di(144, 75, 153);
            final @NonNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData1);

            doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                     isLocked, Objects.requireNonNull(RotateDirection.valueOf(0)),
                                                     doorOwner);
            final @NonNull Drawbridge drawbridge = new Drawbridge(doorData, autoClose, autoOpen, modeUp);
            door2 = drawbridge;
        }

        {
            final int doorUID = 3;
            final int autoOpen = 0;
            final int autoClose = 10;
            final int blocksToMove = 8;
            final boolean isOpen = false;
            final boolean isLocked = false;
            final @NonNull String name = "massive2";
            final @NonNull Vector3Di min = new Vector3Di(144, 70, 168);
            final @NonNull Vector3Di max = new Vector3Di(144, 151, 112);
            final @NonNull Vector3Di engine = new Vector3Di(144, 75, 153);
            final @NonNull Vector3Di powerBlock = new Vector3Di(144, 75, 153);
            final @NonNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData2);

            doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                     isLocked, RotateDirection.UP, doorOwner);
            final @NonNull Portcullis portcullis = new Portcullis(doorData, blocksToMove, autoClose, autoOpen);
            door3 = portcullis;
        }
    }

    @BeforeEach
    protected void beforeEach()
    {
        MockitoAnnotations.openMocks(this);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        Mockito.when(platform.getPWorldFactory()).thenReturn(new TestPWorldFactory());
        Mockito.when(platform.getDoorRegistry()).thenReturn(DoorRegistry.uncached());
        Mockito.when(platform.getDoorTypeManager()).thenReturn(new DoorTypeManager());

        initStorage();
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
    {
        storage = new SQLiteJDBCDriverConnection(DB_FILE);

        // Set SQLiteConfig.SynchronousMode to OFF to increase speed.
        // More info:
        // https://www.javadoc.io/static/org.xerial/sqlite-jdbc/3.15.1/org/sqlite/SQLiteConfig.html#enableShortColumnNames-boolean-
        // https://www.sqlite.org/pragma.html#pragma_synchronous
        storage.getConfigRW().setSynchronous(SQLiteConfig.SynchronousMode.OFF);
        storage.getConfigRO().setSynchronous(SQLiteConfig.SynchronousMode.OFF);
    }

    /**
     * Prepares files for a test run.
     */
    @BeforeAll
    public static void prepare()
    {
        if (DB_FILE.exists())
        {
            System.out.println("WARNING! FILE \"dbFile\" STILL EXISTS! Attempting deletion now!");
            Assertions.assertTrue(DB_FILE.delete());
        }
        if (dbFileBackup.exists())
        {
            System.out.println("WARNING! FILE \"dbFileBackup\" STILL EXISTS! Attempting deletion now!");
            Assertions.assertTrue(dbFileBackup.delete());
        }
    }

    /**
     * Runs cleanup after the tests. Remove leftovers from previous runs and store the finished databases of this run
     * (for debugging purposes).
     */
    @AfterAll
    public static void cleanup()
    {
        // Remove any old database files and append ".FINISHED" to the name of the current one, so it
        // won't interfere with the next run, but can still be used for manual inspection.
        final @NonNull File oldDB = new File(DB_FILE.toString() + ".FINISHED");

        if (oldDB.exists())
            Assertions.assertTrue(oldDB.delete());
        if (dbFileBackup.exists())
            Assertions.assertTrue(dbFileBackup.delete());

        try
        {
            Files.move(DB_FILE.toPath(), oldDB.toPath());
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
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
        BigDoors.get().getDoorTypeManager().registerDoorType(DoorTypeBigDoor.get());
        BigDoors.get().getDoorTypeManager().registerDoorType(DoorTypePortcullis.get());
        BigDoors.get().getDoorTypeManager().registerDoorType(DoorTypeDrawbridge.get());
    }

    /**
     * Runs all tests.
     */
    @Test
    public void runTests()
        throws IllegalAccessException, NoSuchFieldException
    {
        registerDoorTypes();
        insertDoors();
        verifyDoors();
        auxiliaryMethods();
        modifyDoors();

        testDoorTypes();
        failures();
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
     * @param door The door to verify.
     */
    private void testRetrieval(final @NonNull AbstractDoorBase door)
    {
        Assertions.assertNotNull(storage);
        Assertions.assertNotNull(door);
        Assertions.assertNotNull(door.getPrimeOwner().toString());
        Assertions.assertNotNull(door.getName());

        List<AbstractDoorBase> test = storage.getDoors(door.getPrimeOwner().getPPlayerData().getUUID(), door.getName());
        Assertions.assertEquals(1, test.size());

        if (!door.getPrimeOwner().equals(test.get(0).getPrimeOwner()))
            Assertions.fail("DOOR OWNERS DO NOT MATCH!");

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

    /**
     * Tests the basic SQL methods.
     */
    public void auxiliaryMethods()
    {
        // Check simple methods.
        Assertions.assertEquals(1, storage.getDoorCountForPlayer(playerData1.getUUID(), "massive1"));
        Assertions.assertEquals(2, storage.getDoorCountForPlayer(playerData1.getUUID()));
        Assertions.assertEquals(1, storage.getDoorCountForPlayer(playerData2.getUUID()));
        Assertions.assertEquals(1, storage.getDoorCountByName("massive1"));
        Assertions.assertTrue(storage.getDoor(playerData1.getUUID(), 1).isPresent());
        Assertions.assertEquals(door1, storage.getDoor(playerData1.getUUID(), 1).get());
        Assertions.assertFalse(storage.getDoor(playerData1.getUUID(), 3).isPresent());
        final @NonNull Optional<AbstractDoorBase> testDoor1 = storage.getDoor(1L);
        Assertions.assertTrue(testDoor1.isPresent());
        Assertions.assertEquals(door1.getPrimeOwner(), testDoor1.get().getPrimeOwner());
        Assertions.assertEquals(door1, testDoor1.get());
        Assertions.assertFalse(storage.getDoor(9999999).isPresent());
        Assertions.assertTrue(storage.isBigDoorsWorld(worldName));
        Assertions.assertFalse(storage.isBigDoorsWorld("fakeWorld"));

        Assertions.assertEquals(1, storage.getOwnerCountOfDoor(1L));

        long chunkHash = Util.simpleChunkHashFromLocation(door1.getPowerBlock().getX(),
                                                          door1.getPowerBlock().getZ());
        Assertions.assertEquals(3, storage.getDoorsInChunk(chunkHash).size());

        // Check if adding owners works correctly.
        UnitTestUtil.optionalEquals(1, storage.getDoor(1L), (door) -> door.getDoorOwners().size());

        // Try adding playerData2 as owner of door 2.
        Assertions.assertTrue(storage.addOwner(2L, playerData2, 1));

        // Try adding player 1 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, playerData1, 0));

        // Try adding player 2 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, playerData2, 0));

        // Try adding a player that is not in the database yet as owner.
        UnitTestUtil.optionalEquals(1, storage.getDoor(1L), (door) -> door.getDoorOwners().size());
        Assertions.assertTrue(storage.addOwner(1L, playerData3, 1));
        UnitTestUtil.optionalEquals(2, storage.getDoor(1L), (door) -> door.getDoorOwners().size());

        // Verify the permission level of player 2 over door 2.
        UnitTestUtil.optionalEquals(1, storage.getDoor(2L),
                                    (door) -> door.getDoorOwner(playerData2.getUUID()).map(DoorOwner::getPermission)
                                                  .orElse(-1));
        // Verify there are only 2 owners of door 2 (player 1 didn't get copied).
        UnitTestUtil.optionalEquals(2, storage.getDoor(2L), (door) -> door.getDoorOwners().size());

        // Verify that player 2 is the creator of exactly 1 door.
        Assertions.assertEquals(1, storage.getDoors(playerData2.getUUID(), 0).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors (door 3 (0) and door 2 (1)).
        Assertions.assertEquals(2, storage.getDoors(playerData2.getUUID(), 1).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors, both named "massive2".
        Assertions.assertEquals(2, storage.getDoors(playerData2.getUUID(), "massive2", 1).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 door, named "massive2".
        Assertions.assertEquals(1, storage.getDoors(playerData2.getUUID(), "massive2", 0).size());

        // Verify that adding an existing owner overrides the permission level.
        Assertions.assertTrue(storage.addOwner(2L, playerData2, 2));
        UnitTestUtil.optionalEquals(2, storage.getDoor(2L),
                                    (door) -> door.getDoorOwner(playerData2.getUUID()).map(DoorOwner::getPermission)
                                                  .orElse(-1));

        // Remove player 2 as owner of door 2.
        Assertions.assertTrue(storage.removeOwner(2L, playerData2.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getDoor(2L), (door) -> door.getDoorOwners().size());

        // Try to remove player 1 (creator) of door 2. This is not allowed.
        Assertions.assertFalse(storage.removeOwner(2L, playerData1.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getDoor(2L), (door) -> door.getDoorOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 door, named "massive2" (door 3).
        Assertions.assertEquals(1, storage.getDoors(playerData2.getUUID(), "massive2", 1).size());

        // Verify that player 1 is owner of exactly 1 door named "massive2".
        Assertions.assertEquals(1, storage.getDoors(playerData1.getUUID(), "massive2").size());

        // Verify that player 1 owns exactly 2 doors.
        Assertions.assertEquals(2, storage.getDoors(playerData1.getUUID()).size());

        // Verify that there are exactly 2 doors named "massive2" in the database.
        Assertions.assertEquals(2, storage.getDoors("massive2").size());

        // Insert a copy of door 1 in the database (will have doorUID = 4).
        Assertions.assertTrue(storage.insert(door1).isPresent());

        // Verify there are now exactly 2 doors named "massive1" in the database.
        Assertions.assertEquals(2, storage.getDoors("massive1").size());

        // Remove the just-added copy of door 1 (doorUID = 4) from the database.
        Assertions.assertTrue(storage.removeDoor(4L));

        // Verify that after removal of the copy of door 1 (doorUID = 4), there is now exactly 1 door named
        // "massive1" in the database again.
        Assertions.assertEquals(1, storage.getDoors("massive1").size());

        // Verify that player 2 cannot delete doors they do not own (door 1 belongs to player 1).
        Assertions.assertFalse(storage.removeOwner(1L, playerData2.getUUID()));
        Assertions.assertEquals(1, storage.getDoors("massive1").size());

        // Add 10 copies of door3 with a different name to the database.
        door3.setName(DELETEDOORNAME);
        // Verify there are currently exactly 0 doors with this different name in the database.
        Assertions.assertEquals(0, storage.getDoors(DELETEDOORNAME).size());

        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(door3).isPresent());

        // Verify there are now exactly 10 doors with this different name in the database.
        Assertions.assertEquals(10, storage.getDoors(DELETEDOORNAME).size());

        // Remove all 10 doors we just added (owned by player 2) and verify there are exactly 0 entries of the door with
        // the new name after batch removal. Also revert the name change of door 3.
        Assertions.assertTrue(storage.removeDoors(playerData2.getUUID(), DELETEDOORNAME));
        Assertions.assertEquals(0, storage.getDoors(DELETEDOORNAME).size());
        Assertions.assertTrue(storage.getDoor(3L).isPresent());
        door3.setName(storage.getDoor(3L).get().getName());


        // Make sure the player name corresponds to the correct UUID.
        Assertions.assertTrue(storage.getPlayerData(playerData2.getUUID()).isPresent());
        Assertions.assertEquals(playerData2, storage.getPlayerData(playerData2.getUUID()).get());
        Assertions.assertEquals(1, storage.getPlayerData(playerData2.getName()).size());
        Assertions.assertEquals(playerData2, storage.getPlayerData(playerData2.getName()).get(0));
        Assertions.assertEquals(0, storage.getPlayerData(player2NameALT).size());
        Assertions.assertEquals(playerData2, storage.getPlayerData(playerData2.getUUID()).get());

        // Update player 2's name to their alt name and make sure the old name is gone and the new one is reachable.
        final @NonNull PPlayerData playerData2ALT =
            new PPlayerData(UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed"), player2NameALT,
                            20, 22, true, false);

        Assertions.assertTrue(storage.updatePlayerData(playerData2ALT));
        UnitTestUtil.optionalEquals(playerData2ALT, storage.getPlayerData(playerData2.getUUID()));
        Assertions.assertEquals(0, storage.getPlayerData(playerData2.getName()).size());
        Assertions.assertEquals(1, storage.getPlayerData(playerData2ALT.getName()).size());

        // Revert name change of player 2.
        Assertions.assertTrue(storage.updatePlayerData(playerData2));

        chunkHash = Util.simpleChunkHashFromLocation(door1.getPowerBlock().getX(),
                                                     door1.getPowerBlock().getZ());
        final ConcurrentHashMap<Integer, List<Long>> powerBlockData = storage.getPowerBlockData(chunkHash);
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
            ITimerToggleableArchetype doorTimeToggle = (ITimerToggleableArchetype) door3;
            final int door3AutoCloseTime = doorTimeToggle.getAutoCloseTime();
            final int testAutoCloseTime = 20;

            doorTimeToggle.setAutoCloseTime(testAutoCloseTime);
            Assertions.assertTrue(storage.syncDoorData(door3.getSimpleDoorDataCopy(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
            UnitTestUtil.optionalEquals(testAutoCloseTime, storage.getDoor(3L),
                                        (door) -> ((ITimerToggleableArchetype) door).getAutoCloseTime());

            doorTimeToggle.setAutoCloseTime(door3AutoCloseTime);
            Assertions.assertTrue(storage.syncDoorData(door3.getSimpleDoorDataCopy(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));

            UnitTestUtil.optionalEquals(door3AutoCloseTime, storage.getDoor(3L),
                                        (door) -> ((ITimerToggleableArchetype) door).getAutoCloseTime());

            UnitTestUtil.optionalEquals(door3, storage.getDoor(3L));
        }

        // Test (un)locking (i.e. syncing base data).
        {
            door3.setLocked(true);
            Assertions.assertTrue(storage.syncDoorData(door3.getSimpleDoorDataCopy(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
            UnitTestUtil.optionalEquals(true, storage.getDoor(3L), AbstractDoorBase::isLocked);

            door3.setLocked(false);
            Assertions.assertTrue(storage.syncDoorData(door3.getSimpleDoorDataCopy(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
            UnitTestUtil.optionalEquals(false, storage.getDoor(3L), AbstractDoorBase::isLocked);
        }

        // Test syncing all data.
        {
            Portcullis pc = ((Portcullis) door3);

            // Save the current data
            final @NonNull RotateDirection oldDir = door3.getOpenDir();
            final @NonNull RotateDirection newDir = RotateDirection.getOpposite(oldDir);
            Assertions.assertNotSame(oldDir, newDir);

            final @NonNull Vector3DiConst oldPowerBlock = door3.getPowerBlock();
            final @NonNull Vector3Di newPowerBlock = new Vector3Di(oldPowerBlock);
            newPowerBlock.setY((newPowerBlock.getX() + 30) % 256);
            Assertions.assertNotSame(newPowerBlock, oldPowerBlock);

            final @NonNull Vector3Di oldMin = new Vector3Di(door3.getMinimum());
            final @NonNull Vector3Di oldMax = new Vector3Di(door3.getMaximum());
            final @NonNull Vector3Di newMin = oldMin.clone().add(0, 20, 10);
            final @NonNull Vector3Di newMax = oldMax.clone().add(40, 0, 20);
            Assertions.assertNotSame(oldMin, newMin);
            Assertions.assertNotSame(oldMax, newMax);

            final boolean isLocked = door3.isLocked();
            final boolean isOpen = door3.isOpen();


            // update some general data.
            door3.setLocked(!isLocked);
            door3.setOpen(!isOpen);
            door3.setPowerBlockPosition(newPowerBlock);
            door3.setCoordinates(newMin, newMax);
            door3.setOpenDir(newDir);


            // Update some type-specific data
            final int blocksToMove = pc.getBlocksToMove();
            final int newBlocksToMove = blocksToMove * 2;
            Assertions.assertNotSame(0, blocksToMove);
            pc.setBlocksToMove(newBlocksToMove);

            Assertions.assertTrue(storage.syncDoorData(door3.getSimpleDoorDataCopy(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));

            Optional<AbstractDoorBase> retrievedOpt = storage.getDoor(3L);
            Assertions.assertTrue(retrievedOpt.isPresent());
            @NonNull Portcullis retrieved = (Portcullis) retrievedOpt.get();

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
            door3.setPowerBlockPosition(oldPowerBlock);
            door3.setCoordinates(oldMin, oldMax);
            door3.setOpenDir(oldDir);

            // Reset type-specific data
            pc.setBlocksToMove(blocksToMove);

            Assertions.assertTrue(storage.syncDoorData(door3.getSimpleDoorDataCopy(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(door3))));
        }
    }

    /**
     * Runs tests to verify that exceptions are caught when the should be and properly handled.
     */
    public void failures()
        throws NoSuchFieldException, IllegalAccessException
    {
        // Set the enabled status of the database to false.
        final Field databaseLock = SQLiteJDBCDriverConnection.class.getDeclaredField("databaseState");
        databaseLock.setAccessible(true);
        databaseLock.set(storage, IStorage.DatabaseState.ERROR);

        UnitTestUtil.assertWrappedThrows(IllegalStateException.class,
                                         () -> storage.getDoor(playerData1.getUUID(), 1L), true);

        // Set the database state to enabled again and verify that it's now possible to retrieve doors again.
        databaseLock.set(storage, IStorage.DatabaseState.OK);
        Assertions.assertTrue(storage.getDoor(playerData1.getUUID(), 1L).isPresent());
    }
}
