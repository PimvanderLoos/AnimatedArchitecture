package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.config.ConfigLoader;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.OfflinePlayerRetriever;
import nl.pim16aap2.bigdoors.spigotutil.WorldRetriever;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class SQLiteJDBCDriverConnectionTest
{
    private String testDir;
    @Mock
    private PLogger plogger;
    @Mock
    private ConfigLoader config;
    @Mock
    private World world;

    private UUID player1UUID = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935");
    private UUID player2UUID = UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed");
    String player1Name = "pim16aap2";
    private String player2Name = "TestMan";
    private UUID worldUUID = UUID.fromString("ea163ae7-de27-4b3e-b642-d459d56bb360");

    @Mock
    private Player player1;
    @Mock
    private Player player2;

    @Mock
    private WorldRetriever worldRetriever;
    @Mock
    private OfflinePlayerRetriever offlinePlayerRetriever;

    private DoorBase door1;
    private DoorBase door2;
    private DoorBase door3;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private File dbFile;
    private IStorage storage;

    private void init(boolean delete)
    {
        when(config.dbBackup()).thenReturn(false);
        when(world.getUID()).thenReturn(worldUUID);
        when(player1.getName()).thenReturn(player1Name);
        when(player2.getName()).thenReturn(player2Name);
        when(player1.getUniqueId()).thenReturn(player1UUID);
        when(player2.getUniqueId()).thenReturn(player2UUID);
        when(worldRetriever.worldFromString(worldUUID)).thenReturn(world);
        when(offlinePlayerRetriever.getPlayer(player1UUID)).thenReturn(player1);
        when(offlinePlayerRetriever.getPlayer(player2UUID)).thenReturn(player2);
        doAnswer(invocation ->
                 {
                     Object[] args = invocation.getArguments();
                     ((Exception) args[0]).printStackTrace();
                     System.out.println(args[1]);
                     return null;
                 }).when(plogger).logException(any(Exception.class), any(String.class));

        doAnswer(invocation ->
                 {
                     Object[] args = invocation.getArguments();
                     ((Exception) args[0]).printStackTrace();
                     return null;
                 }).when(plogger).logException(any(Exception.class), any(String.class));
        doAnswer(invocation ->
                 {
                     Object[] args = invocation.getArguments();
                     ((Error) args[0]).printStackTrace();
                     System.out.println(args[1]);
                     return null;
                 }).when(plogger).logError(any(Error.class), any(String.class));

        doAnswer(invocation ->
                 {
                     Object[] args = invocation.getArguments();
                     ((Error) args[0]).printStackTrace();
                     return null;
                 }).when(plogger).logError(any(Error.class), any(String.class));

        try
        {
            testDir = new File(".").getCanonicalPath() + "/tests";
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
//        plogger = new PLogger(new File(testDir + "/test.log"), null, "BigDoors");

        dbFile = new File(testDir + "/test.db");
        if (delete && dbFile.exists())
            dbFile.delete();
        storage = new SQLiteJDBCDriverConnection(dbFile, plogger, config, worldRetriever,
                                                 offlinePlayerRetriever);
    }

    private void init()
    {
        init(false);
    }

    @Test
    public void createDB()
    {
        init(true);
    }

    private void initDoors()
    {
        door1 = DoorType.BIGDOOR.getNewDoor(plogger, 1);
        door1.setWorld(world);
        door1.setMinimum(new Location(world, 144, 75, 153));
        door1.setMaximum(new Location(world, 144, 131, 167));
        door1.setEngineLocation(new Location(world, 144, 75, 153));
        door1.setEngineSide(PBlockFace.valueOf(-1));
        door1.setPowerBlockLocation(new Location(world, 139, 74, 166));
        door1.setName("massive1");
        door1.setOpenStatus(false);
        door1.setLock(false);
        door1.setOpenDir(RotateDirection.valueOf(0));
        door1.setAutoClose(0);
        door1.setBlocksToMove(0);
        door1.setDoorOwner(new DoorOwner(door1.getDoorUID(), player1UUID, player1Name, 0));

        door2 = DoorType.BIGDOOR.getNewDoor(plogger, 2);
        door2.setWorld(world);
        door2.setMinimum(new Location(world, 144, 75, 168));
        door2.setMaximum(new Location(world, 144, 131, 182));
        door2.setEngineLocation(new Location(world, 144, 75, 153));
        door2.setEngineSide(PBlockFace.valueOf(-1));
        door2.setPowerBlockLocation(new Location(world, 139, 74, 169));
        door2.setName("massive2");
        door2.setOpenStatus(false);
        door2.setLock(false);
        door2.setOpenDir(RotateDirection.valueOf(0));
        door2.setAutoClose(0);
        door2.setBlocksToMove(0);
        door2.setDoorOwner(new DoorOwner(door2.getDoorUID(), player1UUID, player1Name, 0));

        door3 = DoorType.BIGDOOR.getNewDoor(plogger, 3);
        door3.setWorld(world);
        door3.setMinimum(new Location(world, 144, 70, 168));
        door3.setMaximum(new Location(world, 144, 151, 112));
        door3.setEngineLocation(new Location(world, 144, 75, 153));
        door3.setEngineSide(PBlockFace.valueOf(1));
        door3.setPowerBlockLocation(new Location(world, 139, 74, 169));
        door3.setName("massive3");
        door3.setOpenStatus(false);
        door3.setLock(false);
        door3.setOpenDir(RotateDirection.valueOf(0));
        door3.setAutoClose(0);
        door3.setBlocksToMove(0);
        door3.setDoorOwner(new DoorOwner(door3.getDoorUID(), player2UUID, player2Name, 0));
    }

    @Test
    public void insertDoors()
    {
        init();
        initDoors();
        storage.insert(door1);
        storage.insert(door2);
        storage.insert(door3);
    }

    private void testRetrieval(DoorBase door) throws TooManyDoorsException
    {
        assertNotNull(storage);
        assertNotNull(door);
        assertNotNull(door.getPlayerUUID().toString());
        assertNotNull(door.getName());

        Optional<DoorBase> test = storage.getDoor(door.getPlayerUUID().toString(), door.getName());
        if (!test.isPresent())
            fail("COULD NOT RETRIEVE DOOR WITH ID \"" + door.getDoorUID() + "\"!");

        if (!door.equals(test.get()))
            fail("Data of retrieved door is not the same!");
    }

    // TODO: Put the doors in an array or something.
    @Test
    public void verifyDoors() throws TooManyDoorsException
    {
        init();
        initDoors();
        testRetrieval(door1);
        testRetrieval(door2);
        testRetrieval(door3);
    }

    @Test
    public void addOwners()
    {
        init();
    }
}
