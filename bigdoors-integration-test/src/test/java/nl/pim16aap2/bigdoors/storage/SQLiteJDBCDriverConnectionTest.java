package nl.pim16aap2.bigdoors.storage;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.doors.bigdoor.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.doors.clock.DoorTypeClock;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doors.drawbridge.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.doors.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.doors.elevator.DoorTypeElevator;
import nl.pim16aap2.bigdoors.doors.flag.DoorTypeFlag;
import nl.pim16aap2.bigdoors.doors.garagedoor.DoorTypeGarageDoor;
import nl.pim16aap2.bigdoors.doors.portcullis.DoorTypePortcullis;
import nl.pim16aap2.bigdoors.doors.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.doors.revolvingdoor.DoorTypeRevolvingDoor;
import nl.pim16aap2.bigdoors.doors.slidingdoor.DoorTypeSlidingDoor;
import nl.pim16aap2.bigdoors.doors.windmill.DoorTypeWindmill;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@ExtendWith(MockitoExtension.class)
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

    private static final @NonNull PPlayerData playerData4 =
        new PPlayerData(UUID.fromString("b9bb3938-e49e-4ff2-b74f-25df08e2eda3"), "TypeTester", 40, 44, false, false);

    @NotNull
    private static final IPWorld world = new TestPWorld(worldName);

//    @NotNull
//    private static final IPPlayer playerData1 = new TestPPlayer(playerData1);
//    @NotNull
//    private static final IPPlayer playerData2 = new TestPPlayer(playerData2);
//    @NotNull
//    private static final IPPlayer player3 = new TestPPlayer(playerData3);
//    @NotNull
//    private static final IPPlayer player4 = new TestPPlayer(playerData4);

    private static AbstractDoorBase door1;
    private static AbstractDoorBase door2;
    private static AbstractDoorBase door3;

    /**
     * Used to test the insertion of every type.
     */
    private static AbstractDoorBase[] typeTesting;

    private static final File DB_FILE;
    private static final File dbFileBackup;
    private static SQLiteJDBCDriverConnection storage;
    private static ExecutorService threadPool;

    // Initialize files.
    static
    {
        DB_FILE = new File(UnitTestUtil.TEST_DIR + "/test.db");
        dbFileBackup = new File(DB_FILE.toString() + ".BACKUP");
    }

    // Set up basic stuff.
    @BeforeAll
    public static void basicSetup()
        throws NoSuchFieldException, IllegalAccessException
    {
        UnitTestUtil.setupStatic();
        PLogger.get().setConsoleLogLevel(Level.ALL);
        PLogger.get().setFileLogLevel(Level.SEVERE);
        UnitTestUtil.setFakeDoorRegistry();
    }

    // Initialize mocking.
    @BeforeAll
    public static void setupMocking()
    {
        DoorOpeningUtility.init(null, null, null, null);

        try
        {
            AbstractDoorBase.DoorData doorData;
            {
                final int doorUID = 1;
                final int autoOpen = 0;
                final int autoClose = 0;
                final boolean isOpen = false;
                final boolean isLocked = false;
                final @NotNull String name = "massive1";
                final @NotNull Vector3Di min = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di max = new Vector3Di(144, 131, 167);
                final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di powerBlock = new Vector3Di(144, 75, 153);
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData1);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         isLocked, RotateDirection.EAST, doorOwner);
                final @NotNull BigDoor bigDoor = new BigDoor(doorData, autoClose, autoOpen);
                door1 = bigDoor;
            }

            {
                final int doorUID = 2;
                final int autoOpen = -1;
                final int autoClose = -1;
                final boolean modeUp = true;
                final boolean isOpen = false;
                final boolean isLocked = false;
                final @NotNull String name = "massive2";
                final @NotNull PBlockFace currentDirection = PBlockFace.DOWN;
                final @NotNull Vector3Di min = new Vector3Di(144, 75, 168);
                final @NotNull Vector3Di max = new Vector3Di(144, 131, 182);
                final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di powerBlock = new Vector3Di(144, 75, 153);
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData1);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         isLocked, RotateDirection.valueOf(0), doorOwner);
                final @NotNull Drawbridge drawbridge = new Drawbridge(doorData, autoClose, autoOpen, modeUp);
                door2 = drawbridge;
            }

            {
                final int doorUID = 3;
                final int autoOpen = 0;
                final int autoClose = 10;
                final int blocksToMove = 8;
                final boolean isOpen = false;
                final boolean isLocked = false;
                final @NotNull String name = "massive2";
                final @NotNull Vector3Di min = new Vector3Di(144, 70, 168);
                final @NotNull Vector3Di max = new Vector3Di(144, 151, 112);
                final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di powerBlock = new Vector3Di(144, 75, 153);
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData2);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         isLocked, RotateDirection.UP, doorOwner);
                final @NotNull Portcullis portcullis = new Portcullis(doorData, blocksToMove, autoClose, autoOpen);
                door3 = portcullis;
            }
        }
        catch (Exception e)
        {
            PLogger.get().logThrowable(e);
            throw e;
        }
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
    {
        storage = new SQLiteJDBCDriverConnection(DB_FILE);

        try
        {
            UnitTestUtil.setDatabaseStorage(storage);
            threadPool = UnitTestUtil.getDatabaseManagerThreadPool();
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
            Assertions.fail();
        }
        Assertions.assertNotNull(threadPool);
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
            DB_FILE.delete();
        }
        if (dbFileBackup.exists())
        {
            System.out.println("WARNING! FILE \"dbFileBackup\" STILL EXISTS! Attempting deletion now!");
            dbFileBackup.delete();
        }
    }

    /**
     * Runs cleanup after the tests. Remove leftovers from previous runs and store the finished databases of this run
     * (for debugging purposes).
     */
    @AfterAll
    public static void cleanup()
        throws NoSuchFieldException, IllegalAccessException
    {
        // Remove any old database files and append ".FINISHED" to the name of the current one, so it
        // won't interfere with the next run, but can still be used for manual inspection.
        final @NotNull File oldDB = new File(DB_FILE.toString() + ".FINISHED");
        final @NotNull File oldLog = new File(UnitTestUtil.LOG_FILE.toString() + ".FINISHED");

        PLogger.get().setConsoleLogLevel(Level.FINEST);
        if (oldDB.exists())
            oldDB.delete();
        if (dbFileBackup.exists())
            dbFileBackup.delete();

        try
        {
            Files.move(DB_FILE.toPath(), oldDB.toPath());
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e);
        }
        try
        {
            if (oldLog.exists())
                oldLog.delete();
            while (!PLogger.get().isEmpty())
                Thread.sleep(100L);
            Files.move(UnitTestUtil.LOG_FILE.toPath(), oldLog.toPath());
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void registerDoorTypes()
        throws InterruptedException
    {
        DoorTypeManager.get().registerDoorTypes(Arrays.asList(DoorTypeBigDoor.get(), DoorTypeClock.get(),
                                                              DoorTypeDrawbridge.get(), DoorTypeElevator.get(),
                                                              DoorTypeFlag.get(), DoorTypeGarageDoor.get(),
                                                              DoorTypePortcullis.get(), DoorTypeRevolvingDoor.get(),
                                                              DoorTypeSlidingDoor.get(), DoorTypeWindmill.get()));


        threadPool.awaitTermination(600L, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeBigDoor.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeClock.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeDrawbridge.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeElevator.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeFlag.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeGarageDoor.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypePortcullis.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeRevolvingDoor.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeSlidingDoor.get()).isPresent());
        Assertions.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeWindmill.get()).isPresent());

        Assertions.assertEquals(10, DoorTypeManager.get().getRegisteredDoorTypes().size());

    }

    private void initDoorTypeTest()
    {
        AbstractDoorBase.DoorData doorData;

        int doorUID = 15;
        boolean isOpen = false;
        boolean isLocked = true;
        final @NotNull Vector3Di min = new Vector3Di(144, 70, 168);
        final @NotNull Vector3Di max = new Vector3Di(144, 151, 112);
        final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
        final @NotNull Vector3Di powerBlock = new Vector3Di(103, 103, 103);

        final @NotNull Set<DoorType> registeredDoorTypes = DoorTypeManager.get().getRegisteredDoorTypes();
        typeTesting = new AbstractDoorBase[registeredDoorTypes.size()];

        System.out.println("TESTING " + typeTesting.length + " different door types!");
        int idx = 0;
        for (final @NotNull DoorType doorType : registeredDoorTypes)
        {
            isOpen = !isOpen;
            if (isOpen)
                isLocked = !isLocked;

            final @NotNull String name = "DOORTYPETEST_" + doorType.toString();
            final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, playerData4);
            doorData = new AbstractDoorBase.DoorData(doorUID++, name, min, max, engine, powerBlock, world,
                                                     isLocked, isOpen, RotateDirection.NONE, doorOwner);

            final @NotNull Object[] typeData = new Object[doorType.getParameterCount()];
            int parameterIDX = 0;
            // Just populate the type-specific dataset with random-ish data.
            for (DoorType.Parameter parameter : doorType.getParameters())
            {
                Object data;
                switch (parameter.getParameterType())
                {
                    case TEXT:
                        data = "TEST" + idx * parameterIDX;
                        break;
                    case INTEGER:
                        data = Util.getRandomNumber(0, 5);
                        break;
                    case BLOB:
                    default:
                        data = null;
                }
                typeData[parameterIDX++] = data;
            }
            Optional<AbstractDoorBase> door = doorType.constructDoor(doorData, typeData);
            Assertions.assertTrue(door.isPresent());
            typeTesting[idx++] = door.get();
        }
    }

    /**
     * Inserts all doors in {@link #typeTesting}.
     */
    private void insertDoorTypeTestDoors()
    {
        for (int idx = 0; idx < DoorTypeManager.get().getRegisteredDoorTypes().size(); ++idx)
            Assertions.assertTrue(storage.insert(typeTesting[idx]).isPresent());
    }

    /**
     * Verifies all doors in {@link #typeTesting}.
     */
    private void verifyDoorTypeTestDoors()
        throws TooManyDoorsException
    {
        for (int idx = 0; idx < DoorTypeManager.get().getRegisteredDoorTypes().size(); ++idx)
            testRetrieval(typeTesting[idx]);
    }

    /**
     * Verifies all doors in {@link #typeTesting}.
     */
    private void deleteDoorTypeTestDoors()
    {
        // Just make sure it still exists, to make debugging easier.
        Assertions.assertTrue(storage.getDoor(3L).isPresent());
        Assertions.assertTrue(storage.deleteDoorType(DoorTypePortcullis.get()));
        Assertions.assertTrue(storage.getDoor(1L).isPresent());
        Assertions.assertTrue(storage.getDoor(2L).isPresent());
        Assertions.assertFalse(storage.getDoor(3L).isPresent());
    }


    /**
     * Tests all doors in {@link #typeTesting}.
     */
    private void testDoorTypes()
        throws TooManyDoorsException
    {
        initDoorTypeTest();
        insertDoorTypeTestDoors();
        verifyDoorTypeTestDoors();
        deleteDoorTypeTestDoors();
    }

    /**
     * Runs all tests.
     */
    @Test
    public void runTests()
        throws TooManyDoorsException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
               NoSuchFieldException, IOException, ExecutionException, InterruptedException
    {
        initStorage();
        registerDoorTypes();
        insertDoors();
        verifyDoors();
        auxiliaryMethods();
        modifyDoors();
//        insertDoors(); // Insert the doors again to make sure the upgrade went smoothly.

        testDoorTypes();
        // Make sure no errors were logged.
        UnitTestUtil.waitForLogger();
        Assertions.assertEquals(0, UnitTestUtil.LOG_FILE.length());

        PLogger.get().setFileLogLevel(Level.ALL);

        PLogger.get().logMessage(Level.INFO, "================================\nStarting failure testing now:");
        testFailures();
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
    private void testRetrieval(final @NotNull AbstractDoorBase door)
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
        throws TooManyDoorsException
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
        final @NotNull Optional<AbstractDoorBase> testDoor1 = storage.getDoor(1L);
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
        Assertions.assertEquals(1, storage.getDoor(1L).get().getDoorOwners().size());

        // Try adding playerData2 as owner of door 2.
        Assertions.assertTrue(storage.addOwner(2L, playerData2, 1));

        // Try adding player 1 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, playerData1, 0));

        // Try adding player 2 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, playerData2, 0));

        // Try adding a player that is not in the database yet as owner.
        Assertions.assertEquals(1, storage.getDoor(1L).get().getDoorOwners().size());
        Assertions.assertTrue(storage.addOwner(1L, playerData3, 1));
        Assertions.assertEquals(2, storage.getDoor(1L).get().getDoorOwners().size());

        // Verify the permission level of player 2 over door 2.
        Assertions.assertEquals(1, storage.getDoor(2L).get().getDoorOwner(playerData2.getUUID()).get().getPermission());
        // Verify there are only 2 owners of door 2 (player 1 didn't get copied).
        Assertions.assertEquals(2, storage.getDoor(2L).get().getDoorOwners().size());

        // Verify that player 2 is the creator of exactly 1 door.
        Assertions.assertEquals(1, storage.getDoors(playerData2.getUUID().toString(), 0).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors (door 3 (0) and door 2 (1)).
        Assertions.assertEquals(2, storage.getDoors(playerData2.getUUID().toString(), 1).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors, both named "massive2".
        Assertions.assertEquals(2, storage.getDoors(playerData2.getUUID().toString(), "massive2", 1).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 door, named "massive2".
        Assertions.assertEquals(1, storage.getDoors(playerData2.getUUID().toString(), "massive2", 0).size());

        // Verify that adding an existing owner overrides the permission level.
        Assertions.assertTrue(storage.addOwner(2L, playerData2, 2));
        Assertions.assertEquals(2, storage.getDoor(2L).get().getDoorOwner(playerData2.getUUID()).get().getPermission());

        // Remove player 2 as owner of door 2.
        Assertions.assertTrue(storage.removeOwner(2L, playerData2.getUUID().toString()));
        Assertions.assertEquals(1, storage.getDoor(2L).get().getDoorOwners().size());

        // Try to remove player 1 (creator) of door 2. This is not allowed.
        Assertions.assertFalse(storage.removeOwner(2L, playerData1.getUUID().toString()));
        Assertions.assertEquals(1, storage.getDoor(2L).get().getDoorOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 door, named "massive2" (door 3).
        Assertions.assertEquals(1, storage.getDoors(playerData2.getUUID().toString(), "massive2", 1).size());

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
        Assertions.assertFalse(storage.removeOwner(1L, playerData2.getUUID().toString()));
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
        Assertions.assertTrue(storage.removeDoors(playerData2.getUUID().toString(), DELETEDOORNAME));
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
        // Test changing autoCloseTime value.  (i.e. syncing type-specific data).
        {
            ITimerToggleableArchetype doorTimeToggle = (ITimerToggleableArchetype) door3;
            final int door3AutoCloseTime = doorTimeToggle.getAutoCloseTime();
            final int testAutoCloseTime = 20;

            doorTimeToggle.setAutoCloseTime(testAutoCloseTime);
            Assertions.assertTrue(storage.syncTypeData(door3.getDoorUID(), door3.getDoorType(),
                                                       door3.getDoorType().getTypeData(door3).get()));
            Assertions.assertEquals(testAutoCloseTime,
                                    ((ITimerToggleableArchetype) storage.getDoor(3L).get()).getAutoCloseTime());

            doorTimeToggle.setAutoCloseTime(door3AutoCloseTime);
            Assertions.assertTrue(storage.syncTypeData(door3.getDoorUID(), door3.getDoorType(),
                                                       door3.getDoorType().getTypeData(door3).get()));
            Assertions.assertEquals(door3AutoCloseTime,
                                    ((ITimerToggleableArchetype) storage.getDoor(3L).get()).getAutoCloseTime());

            UnitTestUtil.optionalEquals(door3, storage.getDoor(3L));
        }

        // Test (un)locking (i.e. syncing base data).
        {
            door3.setLocked(true);
            Assertions.assertTrue(storage.syncBaseData(door3.getSimpleDoorDataCopy()));
            Assertions.assertTrue(storage.getDoor(3L).get().isLocked());

            door3.setLocked(false);
            Assertions.assertTrue(storage.syncBaseData(door3.getSimpleDoorDataCopy()));
            Assertions.assertFalse(storage.getDoor(3L).get().isLocked());
        }

        // Test syncing all data.
        {
            Portcullis pc = ((Portcullis) door3);

            // Save the current data
            final @NotNull RotateDirection oldDir = door3.getOpenDir();
            final @NotNull RotateDirection newDir = RotateDirection.getOpposite(oldDir);
            Assertions.assertNotSame(oldDir, newDir);

            final @NotNull Vector3DiConst oldPowerBlock = door3.getPowerBlock();
            final @NotNull Vector3Di newPowerBlock = new Vector3Di(oldPowerBlock);
            newPowerBlock.setY((newPowerBlock.getX() + 30) % 256);
            Assertions.assertNotSame(newPowerBlock, oldPowerBlock);

            final @NotNull Vector3Di oldMin = new Vector3Di(door3.getMinimum());
            final @NotNull Vector3Di oldMax = new Vector3Di(door3.getMaximum());
            final @NotNull Vector3Di newMin = oldMin.clone().add(0, 20, 10);
            final @NotNull Vector3Di newMax = oldMax.clone().add(40, 0, 20);
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

            Assertions.assertTrue(storage.syncAllData(door3.getSimpleDoorDataCopy(), door3.getDoorType(),
                                                      door3.getDoorType().getTypeData(door3).get()));

            @NotNull Portcullis retrieved = (Portcullis) storage.getDoor(3L).get();

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

            Assertions.assertTrue(storage.syncAllData(door3.getSimpleDoorDataCopy(), door3.getDoorType(),
                                                      door3.getDoorType().getTypeData(door3).get()));
        }
    }

    /**
     * Verifies that the size of the log file has increased in regards to the previous size.
     *
     * @param previousSize The last known size of the log file to compare the current size against.
     * @return The current size of the log file.
     */
    private long verifyLogSizeIncrease(final long previousSize)
    {
        UnitTestUtil.waitForLogger();
        final long currentLogSize = UnitTestUtil.LOG_FILE.length();
        Assertions.assertTrue(currentLogSize > previousSize);
        return currentLogSize;
    }

    /**
     * Runs tests to verify that exceptions are caught when the should be and properly handled.
     */
    public void testFailures()
        throws NoSuchFieldException, IllegalAccessException
    {
        // Disable console logging of errors as it's the point of this test. This way I won't get scared by errors in the console.
        PLogger.get().setConsoleLogLevel(Level.OFF);

        long previousLogSize = verifyLogSizeIncrease(-1L);
        // Verify database disabling works as intended.
        {
            // Set the enabled status of the database to false.
            final Field databaseLock = SQLiteJDBCDriverConnection.class.getDeclaredField("databaseState");
            databaseLock.setAccessible(true);
            databaseLock.set(storage, IStorage.DatabaseState.ERROR);
            storage.getDoor(playerData1.getUUID(), 1L);

            // Set the database state to enabled again and verify that it's now possible to retrieve doors again.
            databaseLock.set(storage, IStorage.DatabaseState.OK);
            Assertions.assertTrue(storage.getDoor(playerData1.getUUID(), 1L).isPresent());
        }

        // Make sure new errors were added to the log file.
        previousLogSize = verifyLogSizeIncrease(previousLogSize);

        PLogger.get().setConsoleLogLevel(Level.FINE); // Enable console logging again after the test.
    }
}
