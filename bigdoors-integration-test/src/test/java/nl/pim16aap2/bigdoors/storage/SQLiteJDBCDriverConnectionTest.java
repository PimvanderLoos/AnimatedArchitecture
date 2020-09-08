package nl.pim16aap2.bigdoors.storage;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.doors.bigdoor.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.doors.clock.DoorTypeClock;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
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
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.testimplementations.TestPPlayer;
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
    @NotNull
    private static final String DELETEDOORNAME = "deletemeh";

    @NotNull
    private static final UUID worldUUID = UUID.fromString("ea163ae7-de27-4b3e-b642-d459d56bb360");
    @NotNull
    private static final UUID player1UUID = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935");
    @NotNull
    private static final UUID player2UUID = UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed");
    @NotNull
    private static final UUID player3UUID = UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f");
    @NotNull
    private static final UUID player4UUID = UUID.fromString("b9bb3938-e49e-4ff2-b74f-25df08e2eda3");
    @NotNull
    private static final String player1Name = "pim16aap2";
    @NotNull
    private static final String player2Name = "TestBoy";
    @NotNull
    private static final String player2NameALT = "TestMan";
    @NotNull
    private static final String player3Name = "thirdwheel";
    @NotNull
    private static final String player4Name = "TypeTester";

    @NotNull
    private static final IPWorld world = new TestPWorld(worldUUID);

    @NotNull
    private static final IPPlayer player1 = new TestPPlayer(player1UUID, player1Name);
    @NotNull
    private static final IPPlayer player2 = new TestPPlayer(player2UUID, player2Name);
    @NotNull
    private static final IPPlayer player3 = new TestPPlayer(player3UUID, player3Name);
    @NotNull
    private static final IPPlayer player4 = new TestPPlayer(player4UUID, player4Name);

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
        PLogger.get().setConsoleLogLevel(Level.FINEST);
        PLogger.get().setFileLogLevel(Level.SEVERE);
        DoorRegistry.get().restart();
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
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player1);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.EAST, doorOwner, isLocked);
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
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player1);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.valueOf(0), doorOwner, isLocked);
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
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player2);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.UP, doorOwner, isLocked);
                final @NotNull Portcullis portcullis = new Portcullis(doorData, blocksToMove, autoClose, autoOpen);
                door3 = portcullis;
            }
        }
        catch (Exception e)
        {
            PLogger.get().logThrowable(e);
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
            Assert.fail();
        }
        Assert.assertNotNull(threadPool);
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

        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeBigDoor.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeClock.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeDrawbridge.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeElevator.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeFlag.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeGarageDoor.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypePortcullis.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeRevolvingDoor.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeSlidingDoor.get()).isPresent());
        Assert.assertTrue(DoorTypeManager.get().getDoorTypeID(DoorTypeWindmill.get()).isPresent());

        Assert.assertEquals(10, DoorTypeManager.get().getRegisteredDoorTypes().size());

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
            final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player4);
            doorData = new AbstractDoorBase.DoorData(doorUID++, name, min, max, engine, powerBlock, world,
                                                     isOpen, RotateDirection.NONE, doorOwner, isLocked);

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
            Assert.assertTrue(door.isPresent());
            typeTesting[idx++] = door.get();
        }
    }

    /**
     * Inserts all doors in {@link #typeTesting}.
     */
    private void insertDoorTypeTestDoors()
    {
        for (int idx = 0; idx < DoorTypeManager.get().getRegisteredDoorTypes().size(); ++idx)
            Assert.assertTrue(storage.insert(typeTesting[idx]).isPresent());
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
        Assert.assertTrue(storage.getDoor(3L).isPresent());
        Assert.assertTrue(storage.deleteDoorType(DoorTypePortcullis.get()));
        Assert.assertTrue(storage.getDoor(1L).isPresent());
        Assert.assertTrue(storage.getDoor(2L).isPresent());
        Assert.assertFalse(storage.getDoor(3L).isPresent());
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
        Assert.assertEquals(UnitTestUtil.LOG_FILE.length(), 0);

        PLogger.get().setFileLogLevel(Level.ALL);

        PLogger.get().logMessage(Level.INFO, "================================\nStarting failure testing now:");
        testFailures();
    }

    /**
     * Tests inserting doors in the database.
     */
    public void insertDoors()
    {
        Assert.assertTrue(storage.insert(door1).isPresent());
        Assert.assertTrue(storage.insert(door2).isPresent());
        Assert.assertTrue(storage.insert(door3).isPresent());
    }

    /**
     * Checks if a door was successfully added to the database and that all data in intact.
     *
     * @param door The door to verify.
     */
    private void testRetrieval(final @NotNull AbstractDoorBase door)
        throws TooManyDoorsException
    {
        Assert.assertNotNull(storage);
        Assert.assertNotNull(door);
        Assert.assertNotNull(door.getPrimeOwner().toString());
        Assert.assertNotNull(door.getName());

        List<AbstractDoorBase> test = storage.getDoors(door.getPrimeOwner().getPlayer().getUUID(), door.getName());
        if (test.size() != 1)
            Assert.fail("TOO MANY DOORS FOUND FOR DOOR WITH name \"" + door.getName() + "\"!");

        if (!door.getPrimeOwner().equals(test.get(0).getPrimeOwner()))
            Assert.fail("DOOR OWNERS DO NOT MATCH!");

        if (!door.equals(test.get(0)))
            Assert.fail(
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
        Assert.assertEquals(0, storage.getPermission(player1UUID.toString(), 1));
        Assert.assertEquals(1, storage.getDoorCountForPlayer(player1UUID, "massive1"));
        Assert.assertEquals(2, storage.getDoorCountForPlayer(player1UUID));
        Assert.assertEquals(1, storage.getDoorCountForPlayer(player2UUID));
        Assert.assertEquals(1, storage.getDoorCountByName("massive1"));
        Assert.assertTrue(storage.getDoor(player1UUID, 1).isPresent());
        Assert.assertEquals(door1, storage.getDoor(player1UUID, 1).get());
        Assert.assertFalse(storage.getDoor(player1UUID, 3).isPresent());
        final @NotNull Optional<AbstractDoorBase> testDoor1 = storage.getDoor(1L);
        Assert.assertTrue(testDoor1.isPresent());
        Assert.assertEquals(door1.getPrimeOwner(), testDoor1.get().getPrimeOwner());
        Assert.assertEquals(door1, testDoor1.get());
        Assert.assertFalse(storage.getDoor(9999999).isPresent());
        Assert.assertTrue(storage.isBigDoorsWorld(worldUUID));
        Assert.assertFalse(storage.isBigDoorsWorld(UUID.randomUUID()));

        Assert.assertEquals(1, storage.getOwnerCountOfDoor(1L));

        long chunkHash = Util.simpleChunkHashFromLocation(door1.getPowerBlock().getX(),
                                                          door1.getPowerBlock().getZ());
        Assert.assertEquals(3, storage.getDoorsInChunk(chunkHash).size());

        // Check if adding owners works correctly.
        Assert.assertEquals(1, storage.getDoor(1L).get().getDoorOwners().size());

        // Try adding player2 as owner of door 2.
        Assert.assertTrue(storage.addOwner(2L, player2, 1));

        // Try adding player 1 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assert.assertFalse(storage.addOwner(2L, player1, 0));

        // Try adding player 2 as owner of door 2, while player 1 is already the creator! This is not allowed.
        Assert.assertFalse(storage.addOwner(2L, player2, 0));

        // Try adding a player that is not in the database yet as owner.
        Assert.assertEquals(1, storage.getDoor(1L).get().getDoorOwners().size());
        Assert.assertTrue(storage.addOwner(1L, player3, 1));
        Assert.assertEquals(2, storage.getDoor(1L).get().getDoorOwners().size());

        // Verify the permission level of player 2 over door 2.
        Assert.assertEquals(1, storage.getPermission(player2UUID.toString(), 2L));
        // Verify there are only 2 owners of door 2 (player 1 didn't get copied).
        Assert.assertEquals(2, storage.getDoor(2L).get().getDoorOwners().size());

        // Verify that player 2 is the creator of exactly 1 door.
        Assert.assertEquals(1, storage.getDoors(player2UUID.toString(), 0).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors (door 3 (0) and door 2 (1)).
        Assert.assertEquals(2, storage.getDoors(player2UUID.toString(), 1).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors, both named "massive2".
        Assert.assertEquals(2, storage.getDoors(player2UUID.toString(), "massive2", 1).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 door, named "massive2".
        Assert.assertEquals(1, storage.getDoors(player2UUID.toString(), "massive2", 0).size());

        // Verify that adding an existing owner overrides the permission level.
        Assert.assertTrue(storage.addOwner(2L, player2, 2));
        Assert.assertEquals(2, storage.getPermission(player2UUID.toString(), 2L));

        // Remove player 2 as owner of door 2.
        Assert.assertTrue(storage.removeOwner(2L, player2UUID.toString()));
        Assert.assertEquals(1, storage.getDoor(2L).get().getDoorOwners().size());

        // Try to remove player 1 (creator) of door 2. This is not allowed.
        Assert.assertFalse(storage.removeOwner(2L, player1UUID.toString()));
        Assert.assertEquals(1, storage.getDoor(2L).get().getDoorOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 door, named "massive2" (door 3).
        Assert.assertEquals(1, storage.getDoors(player2UUID.toString(), "massive2", 1).size());

        // Verify that player 1 is owner of exactly 1 door named "massive2".
        Assert.assertEquals(1, storage.getDoors(player1UUID, "massive2").size());

        // Verify that player 1 owns exactly 2 doors.
        Assert.assertEquals(2, storage.getDoors(player1UUID).size());

        // Verify that there are exactly 2 doors named "massive2" in the database.
        Assert.assertEquals(2, storage.getDoors("massive2").size());

        // Insert a copy of door 1 in the database (will have doorUID = 4).
        Assert.assertTrue(storage.insert(door1).isPresent());

        // Verify there are now exactly 2 doors named "massive1" in the database.
        Assert.assertEquals(2, storage.getDoors("massive1").size());

        // Remove the just-added copy of door 1 (doorUID = 4) from the database.
        Assert.assertTrue(storage.removeDoor(4L));

        // Verify that after removal of the copy of door 1 (doorUID = 4), there is now exactly 1 door named
        // "massive1" in the database again.
        Assert.assertEquals(1, storage.getDoors("massive1").size());

        // Verify that player 2 cannot delete doors they do not own (door 1 belongs to player 1).
        Assert.assertFalse(storage.removeOwner(1L, player2UUID.toString()));
        Assert.assertEquals(1, storage.getDoors("massive1").size());

        // Add 10 copies of door3 with a different name to the database.
        door3.setName(DELETEDOORNAME);
        // Verify there are currently exactly 0 doors with this different name in the database.
        Assert.assertEquals(storage.getDoors(DELETEDOORNAME).size(), 0);

        for (int idx = 0; idx < 10; ++idx)
            Assert.assertTrue(storage.insert(door3).isPresent());

        // Verify there are now exactly 10 doors with this different name in the database.
        Assert.assertEquals(10, storage.getDoors(DELETEDOORNAME).size());

        // Remove all 10 doors we just added (owned by player 2) and verify there are exactly 0 entries of the door with
        // the new name after batch removal. Also revert the name change of door 3.
        Assert.assertTrue(storage.removeDoors(player2UUID.toString(), DELETEDOORNAME));
        Assert.assertEquals(storage.getDoors(DELETEDOORNAME).size(), 0);
        Assert.assertTrue(storage.getDoor(3L).isPresent());
        door3.setName(storage.getDoor(3L).get().getName());

        // Make sure the player name corresponds to the correct UUID.
        Assert.assertTrue(storage.getPlayerName(player2UUID.toString()).isPresent());
        Assert.assertEquals(player2Name, storage.getPlayerName(player2UUID.toString()).get());
        Assert.assertFalse(storage.getPlayerUUID(player2NameALT).isPresent());
        Assert.assertTrue(storage.getPlayerUUID(player2Name).isPresent());
        Assert.assertEquals(player2UUID, storage.getPlayerUUID(player2Name).get());
        Assert.assertNotSame(player1UUID, storage.getPlayerUUID(player2Name).get());

        // Update player 2's name to their alt name and make sure the old name is gone and the new one is reachable.
        Assert.assertTrue(storage.updatePlayerName(player2UUID.toString(), player2NameALT));
        Assert.assertFalse(storage.getPlayerUUID(player2Name).isPresent());
        Assert.assertTrue(storage.getPlayerUUID(player2NameALT).isPresent());
        Assert.assertEquals(player2UUID, storage.getPlayerUUID(player2NameALT).get());

        // Revert name change of player 2.
        Assert.assertTrue(storage.updatePlayerName(player2UUID.toString(), player2Name));

        chunkHash = Util.simpleChunkHashFromLocation(door1.getPowerBlock().getX(),
                                                     door1.getPowerBlock().getZ());
        final ConcurrentHashMap<Integer, List<Long>> powerBlockData = storage.getPowerBlockData(chunkHash);
        Assert.assertNotNull(powerBlockData);
        Assert.assertEquals(3, powerBlockData.elements().nextElement().size());
    }

    /**
     * Verifies that door 3 exists in the database, and that the database entry of door 3 does equals the object of door
     * 3.
     */
    private void assertDoor3Parity()
    {
        // Check if door 3 exists in the database.
        Assert.assertTrue(storage.getDoor(player2UUID, 3L).isPresent());
        // Check if the object of door 3 and the database entry of door 3 are the same.
        Assert.assertEquals(door3, storage.getDoor(player2UUID, 3L).get());
    }

    /**
     * Verifies that door 3 exists in the database, and that the database entry of door 3 does not equal the object of
     * door 3.
     */
    private void assertDoor3NotParity()
    {
        // Check if door 3 exists in the database.
        Assert.assertTrue(storage.getDoor(player2UUID, 3L).isPresent());
        // Check if the object of door 3 and the database entry of door 3 are NOT the same.
        Assert.assertNotSame(door3, storage.getDoor(player2UUID, 3L).get());
    }

    /**
     * Runs tests of the methods that modify doors in the database.
     */
    public void modifyDoors()
    {
        // Test changing autoCloseTime value.
        {
            ITimerToggleableArchetype doorTimeToggle = (ITimerToggleableArchetype) door3;
            final int door3AutoCloseTime = doorTimeToggle.getAutoCloseTime();
            final int testAutoCloseTime = 20;

            doorTimeToggle.setAutoCloseTime(testAutoCloseTime);
            // Change the autoCloseTimer of the object of door 3.
            storage.syncTypeData(door3);
            doorTimeToggle.setAutoCloseTime(door3AutoCloseTime);

            // Verify that door 3 in the database is no longer the same as the door 3 object.
            // This should be the case, because the auto close timer is 0 for the door 3 object.
            assertDoor3NotParity();

            doorTimeToggle.setAutoCloseTime(testAutoCloseTime);
            Assert.assertEquals(door3, storage.getDoor(player2UUID, 3L).get());

            // Reset the autoclose timer of both the object of door 3 and the database entry of door 3 and
            // verify data parity.
            doorTimeToggle.setAutoCloseTime(0);
            storage.syncTypeData(door3);
            assertDoor3Parity();
        }

        // Test changing blocksToMove value.
        {
            IBlocksToMoveArchetype doorBTM = (IBlocksToMoveArchetype) door3;
            final int door3BlocksToMove = doorBTM.getBlocksToMove();
            final int testBlocksToMove = 20;
            // Change blocksToMove of the object of door 3.
            doorBTM.setBlocksToMove(testBlocksToMove);
            storage.syncTypeData(door3);
            doorBTM.setBlocksToMove(door3BlocksToMove);

            // Verify that door 3 in the database is no longer the same as the door 3 object.
            // This should be the case, because the blocksToMove value is 0 for the door 3 object.
            assertDoor3NotParity();
            // Update the door 3 object to have the same blocksToMove value as the door 3 in the database
            // And verify that the door 3 in the database and the door 3 object are the same again.
            doorBTM.setBlocksToMove(testBlocksToMove);
            assertDoor3Parity();

            // Reset the blocksToMove value of both the object of door 3 and the database entry of door 3 and
            // verify data parity.
            doorBTM.setBlocksToMove(8);
            storage.syncTypeData(door3);
            assertDoor3Parity();
        }

        // Test (un)locking.
        {
            // Set the lock status of the database entry of door 3 to true.
            storage.setLock(3L, true);
            // Verify that the database entry of door 3 and the object of door 3 are no longer the same.
            // This should be the case because the database entry of door 3 is now locked,
            // while the object of door 3 is not.
            assertDoor3NotParity();
            // Set the object of door 3 to locked so it matches the database entry of door 3. Then make sure
            // Both the object and the database entry of door 3 match.
            door3.setLocked(true);
            assertDoor3Parity();

            // Reset the lock status of both the database entry and the object of door 3 and verify they are
            // the same again.
            storage.setLock(3L, false);
            door3.setLocked(false);
            assertDoor3Parity();
        }

        // Test syncing all data.
        {
            Portcullis pc = ((Portcullis) door3);
            // update some general data.
            door3.setLocked(true);

            // Update some type-specific data
            final int blocksToMove = pc.getBlocksToMove();
            final int newBlocksToMove = blocksToMove * 2;
            Assert.assertNotSame(0, blocksToMove);
            pc.setBlocksToMove(newBlocksToMove);

            Assert.assertTrue(storage.syncAllData(door3));

            @NotNull Portcullis retrieved = (Portcullis) storage.getDoor(3L).get();
            Assert.assertEquals(blocksToMove * 2, retrieved.getBlocksToMove());
            Assert.assertTrue(retrieved.isLocked());

            door3.setLocked(false);
            pc.setBlocksToMove(blocksToMove);

            storage.syncAllData(door3);

            retrieved = (Portcullis) storage.getDoor(3L).get();
            Assert.assertEquals(blocksToMove, retrieved.getBlocksToMove());
            Assert.assertFalse(retrieved.isLocked());
        }

        // Test rotate direction change
        {
            final @NotNull RotateDirection oldDir = door3.getOpenDir();
            final @NotNull RotateDirection newDir = RotateDirection.getOpposite(oldDir);

            // Set the rotation direction of the database entry of door 3 to true.
            storage.updateDoorOpenDirection(3L, newDir);
            // Verify that the database entry of door 3 and the object of door 3 are no longer the same.
            // This should be the case because the rotate directions should differ.
            assertDoor3NotParity();
            // Change the rotation direction of the object of door 3 so that it matches the rotation direction
            // of the database entry of door 3.
            door3.setOpenDir(newDir);
            assertDoor3Parity();

            // Reset the rotation direction of both the database entry and the object of door 3 and verify they are
            // the same again.
            storage.updateDoorOpenDirection(3L, oldDir);
            door3.setOpenDir(oldDir);
            assertDoor3Parity();
        }

        // Test power block relocation.
        {
            // Create a new location that is not the same as the current power block location of door 3.
            final @NotNull Vector3DiConst oldLoc = door3.getPowerBlock();
            final @NotNull Vector3Di newLoc = new Vector3Di(oldLoc);
            newLoc.setY((newLoc.getX() + 30) % 256);
            Assert.assertNotSame(newLoc, oldLoc);

            // Set the power block location of the database entry of door 3 to the new location.
            storage.updateDoorPowerBlockLoc(3L, newLoc.getX(), newLoc.getY(), newLoc.getZ());
            // Verify that the database entry of door 3 and the object of door 3 are no longer the same.
            // This should be the case because the power block locations should differ between them.
            assertDoor3NotParity();
            // Move the powerBlock location of the object of door 3 so that it matches the database entry of door 3.
            // Then make sure both the object and the database entry of door 3 match.
            door3.setPowerBlockPosition(newLoc);
            assertDoor3Parity();

            // Reset the powerBlock location of both the database entry and the object of door 3 and verify they are the
            // same again.
            storage.updateDoorPowerBlockLoc(3L, oldLoc.getX(), oldLoc.getY(), oldLoc.getZ());
            door3.setPowerBlockPosition(oldLoc);
            assertDoor3Parity();
        }

        // Test updating doors.
        {
            // Create some new locations and verify they're different from the old min/max values.
            final @NotNull Vector3Di oldMin = new Vector3Di(door3.getMinimum());
            final @NotNull Vector3Di oldMax = new Vector3Di(door3.getMaximum());
            final @NotNull Vector3Di newMin = oldMin.clone().add(0, 20, 10);
            final @NotNull Vector3Di newMax = oldMax.clone().add(40, 0, 20);
            Assert.assertNotSame(oldMin, newMin);
            Assert.assertNotSame(oldMax, newMax);

            // Set the coordinates of the database entry of door 3 to the new location.
            storage.updateDoorCoords(3L, false, newMin.getX(), newMin.getY(), newMin.getZ(),
                                     newMax.getX(), newMax.getY(), newMax.getZ());
            // Verify that the database entry of door 3 and the object of door 3 are no longer the same.
            // This should be the case because the coordinates should differ between them.
            assertDoor3NotParity();
            // Move the coordinates of the object of door 3 so that it matches the database entry of door 3.
            // Then make sure both the object and the database entry of door 3 match.
            door3.setMinimum(newMin);
            door3.setMaximum(newMax);
            assertDoor3Parity();

            // Reset the coordinates of both the database entry and the object of door 3 and verify they are the
            // same again.
            storage.updateDoorCoords(3L, false, oldMin.getX(), oldMin.getY(), oldMin.getZ(),
                                     oldMax.getX(), oldMax.getY(), oldMax.getZ());
            door3.setMinimum(oldMin);
            door3.setMaximum(oldMax);
            assertDoor3Parity();
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
        Assert.assertTrue(currentLogSize > previousSize);
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
            storage.getDoor(player1UUID, 1L);

            // Set the database state to enabled again and verify that it's now possible to retrieve doors again.
            databaseLock.set(storage, IStorage.DatabaseState.OK);
            Assert.assertTrue(storage.getDoor(player1UUID, 1L).isPresent());
        }

        // Make sure new errors were added to the log file.
        previousLogSize = verifyLogSizeIncrease(previousLogSize);

        PLogger.get().setConsoleLogLevel(Level.FINE); // Enable console logging again after the test.
    }
}
