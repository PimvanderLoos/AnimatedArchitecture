package nl.pim16aap2.bigdoors.storage;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.Drawbridge;
import nl.pim16aap2.bigdoors.doors.Portcullis;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeClock;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeElevator;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeFlag;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeGarageDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorTypePortcullis;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeRevolvingDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeSlidingDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeWindmill;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.testimplementations.TestConfigLoader;
import nl.pim16aap2.bigdoors.testimplementations.TestMessagingInterface;
import nl.pim16aap2.bigdoors.testimplementations.TestPPlayer;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.testimplementations.TestPlatform;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ExtendWith(MockitoExtension.class)
public class SQLiteJDBCDriverConnectionTest implements IRestartableHolder
{
    @NotNull
    private static final IConfigLoader config = new TestConfigLoader();
    @NotNull
    private static final IBigDoorsPlatform platform = new TestPlatform();

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

    private static File dbFile;
    private static File dbFileBackup;
    private static String testDir;
    private static SQLiteJDBCDriverConnection storage;

    // Initialize files.
    static
    {
        try
        {
            testDir = platform.getDataDirectory().getCanonicalPath() + "/tests";
            dbFile = new File(testDir + "/test.db");
            dbFileBackup = new File(dbFile.toString() + ".BACKUP");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @NotNull
    private static final File logFile = new File(testDir, "/log.txt");

    static
    {
        BigDoors.get().setMessagingInterface(new TestMessagingInterface());
    }

    // Set up basic stuff.
    @BeforeAll
    public static void basicSetup()
    {
        BigDoors.get().setBigDoorsPlatform(platform);
        PLogger.init(logFile);
        PLogger.get().setConsoleLogging(true);
        PLogger.get().setOnlyLogExceptions(true); // Only log errors etc.
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
                final @NotNull String name = "massive1";
                final @NotNull Vector3Di min = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di max = new Vector3Di(144, 131, 167);
                final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di powerBlock = new Vector3Di(0, 0, 0);
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player1);
                final @NotNull PBlockFace currentDirection = PBlockFace.DOWN;

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.EAST, doorOwner, false);
                final @NotNull BigDoor bigDoor = new BigDoor(doorData, autoClose, autoOpen, currentDirection);
                door1 = bigDoor;
            }

            {
                final int doorUID = 2;
                final int autoOpen = 0;
                final int autoClose = 0;
                final boolean isOpen = false;
                final @NotNull String name = "massive2";
                final @NotNull PBlockFace currentDirection = PBlockFace.DOWN;
                final @NotNull Vector3Di min = new Vector3Di(144, 75, 168);
                final @NotNull Vector3Di max = new Vector3Di(144, 131, 182);
                final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di powerBlock = new Vector3Di(0, 0, 0);
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player1);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.valueOf(0), doorOwner, false);
                final @NotNull Drawbridge drawbridge = new Drawbridge(doorData, autoClose, autoOpen, currentDirection,
                                                                      true);
                door2 = drawbridge;
            }

            {
                final int doorUID = 3;
                final int autoOpen = 0;
                final int autoClose = 10;
                final int blocksToMove = 8;
                final boolean isOpen = false;
                final @NotNull String name = "massive2";
                final @NotNull Vector3Di min = new Vector3Di(144, 70, 168);
                final @NotNull Vector3Di max = new Vector3Di(144, 151, 112);
                final @NotNull Vector3Di engine = new Vector3Di(144, 75, 153);
                final @NotNull Vector3Di powerBlock = new Vector3Di(0, 0, 0);
                final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player2);

                doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.UP, doorOwner, false);
                final @NotNull Portcullis portcullis = new Portcullis(doorData, blocksToMove, autoClose, autoOpen);
                door3 = portcullis;
            }
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
        throws NoSuchFieldException, IllegalAccessException
    {
        DatabaseManager.init(this, config, dbFile);
        Field dbField = DatabaseManager.class.getDeclaredField("db");
        dbField.setAccessible(true);
        storage = (SQLiteJDBCDriverConnection) dbField.get(DatabaseManager.get());
//        storage.setStatementLogging(true);
    }

    /**
     * Prepares files for a test run.
     */
    @BeforeAll
    public static void prepare()
    {
        if (dbFile.exists())
        {
            System.out.println("WARNING! FILE \"dbFile\" STILL EXISTS! Attempting deletion now!");
            dbFile.delete();
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
    {
        // Remove any old database files and append ".FINISHED" to the name of the current one, so it
        // won't interfere with the next run, but can still be used for manual inspection.
        final @NotNull File oldDB = new File(dbFile.toString() + ".FINISHED");
        final @NotNull File oldLog = new File(logFile.toString() + ".FINISHED");

        PLogger.get().setConsoleLogging(true);
        if (oldDB.exists())
            oldDB.delete();
        if (dbFileBackup.exists())
            dbFileBackup.delete();

        try
        {
            Files.move(dbFile.toPath(), oldDB.toPath());
        }
        catch (IOException e)
        {
            PLogger.get().logException(e);
        }
        try
        {
            if (oldLog.exists())
                oldLog.delete();
            while (!PLogger.get().isEmpty())
                Thread.sleep(100L);
            Files.move(logFile.toPath(), oldLog.toPath());
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void registerDoorTypes()
        throws ExecutionException, InterruptedException
    {
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeBigDoor.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeClock.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeDrawbridge.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeElevator.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeFlag.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeGarageDoor.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypePortcullis.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeRevolvingDoor.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeSlidingDoor.get()).get());
        Assert.assertTrue(DoorTypeManager.get().registerDoorType(DoorTypeWindmill.get()).get());
    }

    private void initDoorTypeTest()
    {
        AbstractDoorBase.DoorData doorData;

        int doorUID = 15;
        final boolean isOpen = false;
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
            final @NotNull String name = "DOORTYPETEST_" + doorType.toString();
            final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, 0, player4);
            doorData = new AbstractDoorBase.DoorData(doorUID++, name, min, max, engine, powerBlock, world,
                                                     isOpen, RotateDirection.EAST, doorOwner, false);

            final @NotNull Object[] typeData = new Object[doorType.getParameterCount()];
            int parameterIDX = 0;
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
            Assert.assertTrue(storage.insert(typeTesting[idx]));
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
     * Tests all doors in {@link #typeTesting}.
     */
    private void testDoorTypes()
        throws TooManyDoorsException
    {
        initDoorTypeTest();
        insertDoorTypeTestDoors();
        verifyDoorTypeTestDoors();
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
        waitForLogger();
        Assert.assertEquals(logFile.length(), 0);

        PLogger.get().setOnlyLogExceptions(false);

        PLogger.get().logMessage("================================\nStarting failure testing now:");
        testFailures();
    }

    /**
     * Tests inserting doors in the database.
     */
    public void insertDoors()
    {
        Assert.assertTrue(storage.insert(door1));
        Assert.assertTrue(storage.insert(door2));
        Assert.assertTrue(storage.insert(door3));
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
        Assert.assertNotNull(door.getPlayerUUID().toString());
        Assert.assertNotNull(door.getName());

        Optional<List<AbstractDoorBase>> test = storage.getDoors(door.getPlayerUUID(), door.getName());
        if (!test.isPresent())
            Assert.fail("COULD NOT RETRIEVE DOOR WITH name \"" + door.getName() + "\"!");

        if (test.get().size() != 1)
            Assert.fail("TOO MANY DOORS FOUND FOR DOOR WITH name \"" + door.getName() + "\"!");

        if (!door.equals(test.get().get(0)))
            Assert.fail(
                "Data of retrieved door is not the same! ID = " + door.getDoorUID() + ", name = " + door.getName() +
                    ", found ID = " + test.get().get(0).getDoorUID() + ", found name = " + test.get().get(0).getName());
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
        Assert.assertTrue(storage.getDoor(1).isPresent());
        Assert.assertEquals(door1, storage.getDoor(1).get());
        Assert.assertFalse(storage.getDoor(9999999).isPresent());
        Assert.assertTrue(storage.getCreatorOfDoor(1L).isPresent());
        Assert.assertEquals(door1.getDoorOwner(), storage.getCreatorOfDoor(1L).get());
        Assert.assertTrue(storage.isBigDoorsWorld(worldUUID));
        Assert.assertFalse(storage.isBigDoorsWorld(UUID.randomUUID()));

        Assert.assertEquals(1, storage.getOwnerCountOfDoor(1L));

        // Check if adding owners works correctly.
        Assert.assertEquals(1, storage.getOwnersOfDoor(1L).size());
        // Try adding player2 as owner of door 2.
        storage.addOwner(2L, player2, 1);
        // Try adding player 1 as owner of door 2, while player 1 is already the creator! This is not allowed.
        storage.addOwner(2L, player1, 0);
        // Try adding player 2 as owner of door 2, while player 1 is already the creator! This is not allowed.
        storage.addOwner(2L, player2, 0);

        // Try adding a player that is not in the database yet as owner.
        Assert.assertEquals(1, storage.getOwnersOfDoor(1L).size());
        storage.addOwner(1L, player3, 1);
        Assert.assertEquals(2, storage.getOwnersOfDoor(1L).size());

        // Verify the permission level of player 2 over door 2.
        Assert.assertEquals(1, storage.getPermission(player2UUID.toString(), 2L));
        // Verify there are only 2 owners of door 2 (player 1 didn't get copied).
        Assert.assertEquals(2, storage.getOwnersOfDoor(2L).size());

        // Verify that player 2 is the creator of exactly 1 door.
        Assert.assertTrue(storage.getDoors(player2UUID.toString(), 0).isPresent());
        Assert.assertEquals(1, storage.getDoors(player2UUID.toString(), 0).get().size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors (door 3 (0) and door 2 (1)).
        Assert.assertTrue(storage.getDoors(player2UUID.toString(), 1).isPresent());
        Assert.assertEquals(2, storage.getDoors(player2UUID.toString(), 1).get().size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 doors, both named "massive2".
        Assert.assertTrue(storage.getDoors(player2UUID.toString(), "massive2", 1).isPresent());
        Assert.assertEquals(2, storage.getDoors(player2UUID.toString(), "massive2", 1).get().size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 door, named "massive2".
        Assert.assertTrue(storage.getDoors(player2UUID.toString(), "massive2", 0).isPresent());
        Assert.assertEquals(1, storage.getDoors(player2UUID.toString(), "massive2", 0).get().size());

        // Verify that adding an existing owner overrides the permission level.
        storage.addOwner(2L, player2, 2);
        Assert.assertEquals(2, storage.getPermission(player2UUID.toString(), 2L));

        // Remove player 2 as owner of door 2.
        storage.removeOwner(2L, player2UUID.toString());
        Assert.assertEquals(1, storage.getOwnersOfDoor(2L).size());

        // Try to remove player 1 (creator) of door 2. This is not allowed.
        storage.removeOwner(2L, player1UUID.toString());
        Assert.assertEquals(1, storage.getOwnersOfDoor(2L).size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 door, named "massive2" (door 3).
        Assert.assertTrue(storage.getDoors(player2UUID.toString(), "massive2", 1).isPresent());
        Assert.assertEquals(1, storage.getDoors(player2UUID.toString(), "massive2", 1).get().size());

        // Verify that player 1 is owner of exactly 1 door named "massive2".
        Assert.assertTrue(storage.getDoors(player1UUID, "massive2").isPresent());
        Assert.assertEquals(1, storage.getDoors(player1UUID, "massive2").get().size());

        // Verify that player 1 owns exactly 2 doors.
        Assert.assertTrue(storage.getDoors(player1UUID).isPresent());
        Assert.assertEquals(2, storage.getDoors(player1UUID).get().size());

        // Verify that there are exactly 2 doors named "massive2" in the database.
        Assert.assertTrue(storage.getDoors("massive2").isPresent());
        Assert.assertEquals(2, storage.getDoors("massive2").get().size());

        // Insert a copy of door 1 in the database (will have doorUID = 4).
        storage.insert(door1);

        // Verify there are now exactly 2 doors named "massive1" in the database.
        Assert.assertTrue(storage.getDoors("massive1").isPresent());
        Assert.assertEquals(2, storage.getDoors("massive1").get().size());

        // Remove the just-added copy of door 1 (doorUID = 4) from the database.
        storage.removeDoor(4L);

        // Verify that after removal of the copy of door 1 (doorUID = 4), there is now exactly 1 door named
        // "massive1" in the database again.
        Assert.assertTrue(storage.getDoors("massive1").isPresent());
        Assert.assertEquals(1, storage.getDoors("massive1").get().size());

        // Verify that player 2 cannot delete doors they do not own (door 1 belongs to player 1).
        storage.removeOwner(1L, player2UUID.toString());
        Assert.assertTrue(storage.getDoors("massive1").isPresent());
        Assert.assertEquals(1, storage.getDoors("massive1").get().size());

        // Add 10 copies of door3 with a different name to the database.
        door3.setName(DELETEDOORNAME);
        // Verify there are currently exactly 0 doors with this different name in the database.
        Assert.assertFalse(storage.getDoors(DELETEDOORNAME).isPresent());

        for (int idx = 0; idx < 10; ++idx)
            storage.insert(door3);

        // Verify there are now exactly 10 doors with this different name in the database.
        Assert.assertTrue(storage.getDoors(DELETEDOORNAME).isPresent());
        Assert.assertEquals(10, storage.getDoors(DELETEDOORNAME).get().size());

        // Remove all 10 doors we just added (owned by player 2) and verify there are exactly 0 entries of the door with
        // the new name after batch removal. Also revert the name change of door 3.
        storage.removeDoors(player2UUID.toString(), DELETEDOORNAME);
        Assert.assertFalse(storage.getDoors(DELETEDOORNAME).isPresent());
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
        storage.updatePlayerName(player2UUID.toString(), player2NameALT);
        Assert.assertFalse(storage.getPlayerUUID(player2Name).isPresent());
        Assert.assertTrue(storage.getPlayerUUID(player2NameALT).isPresent());
        Assert.assertEquals(player2UUID, storage.getPlayerUUID(player2NameALT).get());

        // Revert name change of player 2.
        storage.updatePlayerName(player2UUID.toString(), player2Name);

//        long chunkHash = Util.simpleChunkHashFromLocation(door1.getPowerBlockLoc().getX(),
//                                                          door1.getPowerBlockLoc().getZ());
//        Assert.assertNotNull(storage.getPowerBlockData(chunkHash));
//        Assert.assertEquals(3, storage.getPowerBlockData(chunkHash).size());
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
            final int door3AutoCloseTime = doorTimeToggle.getAutoCloseTimer();
            final int testAutoCloseTime = 20;

            doorTimeToggle.setAutoCloseTimer(testAutoCloseTime);
            // Change the autoCloseTimer of the object of door 3.
            storage.updateTypeData(door3);
            doorTimeToggle.setAutoCloseTimer(door3AutoCloseTime);

            // Verify that door 3 in the database is no longer the same as the door 3 object.
            // This should be the case, because the auto close timer is 0 for the door 3 object.
            assertDoor3NotParity();

            doorTimeToggle.setAutoCloseTimer(testAutoCloseTime);
            Assert.assertEquals(door3, storage.getDoor(player2UUID, 3L).get());

            // Reset the autoclose timer of both the object of door 3 and the database entry of door 3 and
            // verify data parity.
            doorTimeToggle.setAutoCloseTimer(0);
            storage.updateTypeData(door3);
            assertDoor3Parity();
        }

        // Test changing blocksToMove value.
        {
            IBlocksToMoveArchetype doorBTM = (IBlocksToMoveArchetype) door3;
            final int door3BlocksToMove = doorBTM.getBlocksToMove();
            final int testBlocksToMove = 20;
            // Change blocksToMove of the object of door 3.
            doorBTM.setBlocksToMove(testBlocksToMove);
            storage.updateTypeData(door3);
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
            doorBTM.setBlocksToMove(0);
            storage.updateTypeData(door3);
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
            door3.setLock(true);
            assertDoor3Parity();

            // Reset the lock status of both the database entry and the object of door 3 and verify they are
            // the same again.
            storage.setLock(3L, false);
            door3.setLock(false);
            assertDoor3Parity();
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

//        // Test power block relocation.
//        {
//            // Create a new location that is not the same as the current power block location of door 3.
//            final @NotNull Vector3Di oldLoc = door3.getPowerBlockLoc();
//            final @NotNull Vector3Di newLoc = oldLoc.clone();
//            newLoc.setY((newLoc.getX() + 30) % 256);
//            Assert.assertNotSame(newLoc, oldLoc);
//
//            // Set the power block location of the database entry of door 3 to the new location.
//            storage.updateDoorPowerBlockLoc(3L, newLoc.getX(), newLoc.getY(), newLoc.getZ());
//            // Verify that the database entry of door 3 and the object of door 3 are no longer the same.
//            // This should be the case because the power block locations should differ between them.
//            assertDoor3NotParity();
//            // Move the powerBlock location of the object of door 3 so that it matches the database entry of door 3.
//            // Then make sure both the object and the database entry of door 3 match.
//            door3.setPowerBlockLocation(newLoc);
//            assertDoor3Parity();
//
//            // Reset the powerBlock location of both the database entry and the object of door 3 and verify they are the
//            // same again.
//            storage.updateDoorPowerBlockLoc(3L, oldLoc.getX(), oldLoc.getY(), oldLoc.getZ());
//            door3.setPowerBlockLocation(oldLoc);
//            assertDoor3Parity();
//        }

        // Test updating doors.
        {
            // Create some new locations and verify they're different from the old min/max values.
            final @NotNull Vector3Di oldMin = door3.getMinimum();
            final @NotNull Vector3Di oldMax = door3.getMaximum();
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
     * Makes this thread wait for the logger to finish writing everything to the log file.
     */
    private void waitForLogger()
    {
        while (!PLogger.get().isEmpty())
        {
            try
            {
                Thread.sleep(10L);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        // Wait a bit longer to make sure it's finished writing the file as well.
        try
        {
            Thread.sleep(20L);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
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
        waitForLogger();
        final long currentLogSize = logFile.length();
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
        PLogger.get().setConsoleLogging(false);

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

        PLogger.get().setConsoleLogging(true); // Enable console logging again after the test.
    }

    @Override
    public void registerRestartable(@NotNull IRestartable restartable)
    {
        // Don't do anything; it's not needed.
    }

    @Override
    public boolean isRestartableRegistered(@NotNull IRestartable restartable)
    {
        // Don't do anything; it's not needed.
        return false;
    }
}
