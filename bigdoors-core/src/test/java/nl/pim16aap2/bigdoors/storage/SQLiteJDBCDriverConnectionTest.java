package nl.pim16aap2.bigdoors.storage;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.testimplementations.TestConfigLoader;
import nl.pim16aap2.bigdoors.testimplementations.TestMessagingInterface;
import nl.pim16aap2.bigdoors.testimplementations.TestPPlayer;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.testimplementations.TestPlatform;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class SQLiteJDBCDriverConnectionTest
{
    private static final IConfigLoader config = new TestConfigLoader();
    private static final IBigDoorsPlatform platform = new TestPlatform();

    private static final String DELETEDOORNAME = "deletemeh";

    private static final UUID player1UUID = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935");
    private static final UUID player2UUID = UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed");
    private static final UUID player3UUID = UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f");
    private static final String player1Name = "pim16aap2";
    private static final String player2Name = "TestBoy";
    private static final String player2NameALT = "TestMan";
    private static final String player3Name = "thirdwheel";
    private static final UUID worldUUID = UUID.fromString("ea163ae7-de27-4b3e-b642-d459d56bb360");

    private static IPWorld world = new TestPWorld(worldUUID);

    private static IPPlayer player1 = new TestPPlayer(player1UUID, player1Name);
    private static IPPlayer player2 = new TestPPlayer(player2UUID, player2Name);
    private static IPPlayer player3 = new TestPPlayer(player3UUID, player3Name);

    private static AbstractDoorBase door1;
    private static AbstractDoorBase door2;
    private static AbstractDoorBase door3;

    private static File dbFile;
    private static File dbFileBackup;
    private static String testDir;
    private static IStorage storage;

    // Initialize files.
    static
    {
        try
        {
            testDir = new File(".").getCanonicalPath() + "/tests";
            dbFile = new File(testDir + "/test.db");
            dbFileBackup = new File(dbFile.toString() + ".BACKUP");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static final File logFile = new File(testDir, "log.txt");
    private static final PLogger plogger = PLogger.get();

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
        plogger.setConsoleLogging(true);
        plogger.setOnlyLogExceptions(true); // Only log errors etc.
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
                Vector3Di min = new Vector3Di(144, 75, 153);
                Vector3Di max = new Vector3Di(144, 131, 167);
                Vector3Di engine = new Vector3Di(144, 75, 153);
                Vector3Di powerBlock = new Vector3Di(101, 101, 101);
                boolean isOpen = false;
                doorData = new AbstractDoorBase.DoorData(min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.valueOf(0));
            }
            door1 = DoorType.BIGDOOR.getNewDoor(plogger, 1, doorData);

            door1.setName("massive1");
            door1.setLock(false);
            door1.setAutoClose(0);
            door1.setBlocksToMove(0);
            door1.setDoorOwner(new DoorOwner(door1.getDoorUID(), player1UUID, player1Name, 0));


            {
                Vector3Di min = new Vector3Di(144, 75, 168);
                Vector3Di max = new Vector3Di(144, 131, 182);
                Vector3Di engine = new Vector3Di(144, 75, 153);
                Vector3Di powerBlock = new Vector3Di(102, 102, 102);
                boolean isOpen = false;
                doorData = new AbstractDoorBase.DoorData(min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.valueOf(0));
            }
            door2 = DoorType.DRAWBRIDGE.getNewDoor(plogger, 2, doorData);
            door2.setName("massive2");
            door2.setLock(false);
            door2.setAutoClose(0);
            door2.setBlocksToMove(0);
            door2.setDoorOwner(new DoorOwner(door2.getDoorUID(), player1UUID, player1Name, 0));


            {
                Vector3Di min = new Vector3Di(144, 70, 168);
                Vector3Di max = new Vector3Di(144, 151, 112);
                Vector3Di engine = new Vector3Di(144, 75, 153);
                Vector3Di powerBlock = new Vector3Di(103, 103, 103);
                boolean isOpen = false;
                doorData = new AbstractDoorBase.DoorData(min, max, engine, powerBlock, world, isOpen,
                                                         RotateDirection.NORTH);
            }
            door3 = DoorType.BIGDOOR.getNewDoor(plogger, 3, doorData);
            door3.setName("massive2");
            door3.setLock(false);
            door3.setAutoClose(0);
            door3.setBlocksToMove(0);
            door3.setDoorOwner(new DoorOwner(door3.getDoorUID(), player2UUID, player2Name, 0));
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
    {
        storage = new SQLiteJDBCDriverConnection(dbFile, plogger, config);
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
        File oldDB = new File(dbFile.toString() + ".FINISHED");
        File oldLog = new File(logFile.toString() + ".FINISHED");

        plogger.setConsoleLogging(true);
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
            plogger.logException(e);
        }
        try
        {
            if (oldLog.exists())
                oldLog.delete();
            while (!plogger.isEmpty())
                Thread.sleep(100L);
            Files.move(logFile.toPath(), oldLog.toPath());
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Runs all tests.
     */
    @Test
    public void runTests()
        throws TooManyDoorsException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
               NoSuchFieldException, IOException
    {
        initStorage();
        insertDoors();
        verifyDoors();
        auxiliaryMethods();
        modifyDoors();
        insertDoors(); // Insert the doors again to make sure the upgrade went smoothly.
        // Make sure no errors were logged.
        waitForLogger();
        Assert.assertEquals(logFile.length(), 0);

        plogger.setOnlyLogExceptions(false);

        plogger.logMessage("================================\nStarting failure testing now:");
        testFailures();
    }

    /**
     * Tests inserting doors in the database.
     */
    public void insertDoors()
    {
        storage.insert(door1);
        storage.insert(door2);
        storage.insert(door3);
    }

    /**
     * Checks if a door was successfully added to the database and that all data in intact.
     *
     * @param door The door to verify.
     */
    private void testRetrieval(AbstractDoorBase door)
        throws TooManyDoorsException
    {
        Assert.assertNotNull(storage);
        Assert.assertNotNull(door);
        Assert.assertNotNull(door.getPlayerUUID().toString());
        Assert.assertNotNull(door.getName());

        Optional<List<AbstractDoorBase>> test = storage.getDoors(door.getPlayerUUID(), door.getName());
        if (!test.isPresent())
            Assert.fail("COULD NOT RETRIEVE DOOR WITH ID \"" + door.getDoorUID() + "\"!");

        if (test.get().size() != 1)
            Assert.fail("TOO MANY DOORS FOUND FOR DOOR WITH ID \"" + door.getDoorUID() + "\"!");

        if (!door.equals(test.get().get(0)))
            Assert.fail("Data of retrieved door is not the same!");
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
        Assert.assertTrue(storage.getOwnerOfDoor(1L).isPresent());
        Assert.assertEquals(door1.getDoorOwner(), storage.getOwnerOfDoor(1L).get());
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

        long chunkHash = Util.simpleChunkHashFromLocation(door1.getPowerBlockLoc().getX(),
                                                          door1.getPowerBlockLoc().getZ());
        Assert.assertNotNull(storage.getPowerBlockData(chunkHash));
        Assert.assertEquals(3, storage.getPowerBlockData(chunkHash).size());
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
            final int testAutoCloseTime = 20;
            // Change the autoCloseTimer of the object of door 3.
            storage.updateDoorAutoClose(3, testAutoCloseTime);
            // Verify that door 3 in the database is no longer the same as the door 3 object.
            // This should be the case, because the auto close timer is 0 for the door 3 object.
            assertDoor3NotParity();
            door3.setAutoClose(testAutoCloseTime);
            Assert.assertEquals(door3, storage.getDoor(player2UUID, 3L).get());

            // Reset the autoclose timer of both the object of door 3 and the database entry of door 3 and
            // verify data parity.
            door3.setAutoClose(0);
            storage.updateDoorAutoClose(3, 0);
            assertDoor3Parity();
        }

        // Test changing blocksToMove value.
        {
            final int testBlocksToMove = 20;
            // Change blocksToMove of the object of door 3.
            storage.updateDoorBlocksToMove(3, testBlocksToMove);
            // Verify that door 3 in the database is no longer the same as the door 3 object.
            // This should be the case, because the blocksToMove value is 0 for the door 3 object.
            assertDoor3NotParity();
            // Update the door 3 object to have the same blocksToMove value as the door 3 in the database
            // And verify that the door 3 in the database and the door 3 object are the same again.
            door3.setBlocksToMove(testBlocksToMove);
            assertDoor3Parity();

            // Reset the blocksToMove value of both the object of door 3 and the database entry of door 3 and
            // verify data parity.
            door3.setBlocksToMove(0);
            storage.updateDoorBlocksToMove(3, 0);
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
            RotateDirection oldDir = door3.getOpenDir();
            RotateDirection newDir = RotateDirection.getOpposite(oldDir);

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
            Vector3Di oldLoc = door3.getPowerBlockLoc();
            Vector3Di newLoc = oldLoc.clone();
            newLoc.setY((newLoc.getX() + 30) % 256);
            Assert.assertNotSame(newLoc, oldLoc);

            // Set the power block location of the database entry of door 3 to the new location.
            storage.updateDoorPowerBlockLoc(3L, newLoc.getX(), newLoc.getY(), newLoc.getZ());
            // Verify that the database entry of door 3 and the object of door 3 are no longer the same.
            // This should be the case because the power block locations should differ between them.
            assertDoor3NotParity();
            // Move the powerBlock location of the object of door 3 so that it matches the database entry of door 3.
            // Then make sure both the object and the database entry of door 3 match.
            door3.setPowerBlockLocation(newLoc);
            assertDoor3Parity();

            // Reset the powerBlock location of both the database entry and the object of door 3 and verify they are the
            // same again.
            storage.updateDoorPowerBlockLoc(3L, oldLoc.getX(), oldLoc.getY(), oldLoc.getZ());
            door3.setPowerBlockLocation(oldLoc);
            assertDoor3Parity();
        }

        // Test updating doors.
        {
            // Create some new locations and verify they're different from the old min/max values.
            Vector3Di oldMin = door3.getMinimum();
            Vector3Di oldMax = door3.getMaximum();
            Vector3Di newMin = oldMin.clone().add(0, 20, 10);
            Vector3Di newMax = oldMax.clone().add(40, 0, 20);
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
        while (!plogger.isEmpty())
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
     * Runs tests to verify that exceptions are caught when the should be and properly handled.
     */
    public void testFailures()
        throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException
    {
        // Disable console logging of errors as it's the point of this test. This way I won't get scared by errors in the console.
        plogger.setConsoleLogging(false);

//        waitForLogger();
//        long logSize = logFile.length();
//
//        // Make sure new errors were added to the log file.
//        waitForLogger();
//        long newLogSize = logFile.length();
//        Assert.assertTrue(newLogSize > logSize);
//        logSize = newLogSize;

        plogger.setConsoleLogging(true); // Enable console logging again after the test.
    }
}
