package nl.pim16aap2.bigdoors.storage;

import com.google.common.flogger.LogSiteStackTrace;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.StructureDeletionManager;
import nl.pim16aap2.bigdoors.managers.StructureTypeManager;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.structures.StructureBaseBuilder;
import nl.pim16aap2.bigdoors.structures.StructureOwner;
import nl.pim16aap2.bigdoors.structures.StructureRegistry;
import nl.pim16aap2.bigdoors.structures.StructureSerializer;
import nl.pim16aap2.bigdoors.structures.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.structures.bigdoor.StructureTypeBigDoor;
import nl.pim16aap2.bigdoors.structures.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.structures.drawbridge.StructureTypeDrawbridge;
import nl.pim16aap2.bigdoors.structures.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.structures.portcullis.StructureTypePortcullis;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorldFactory;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssertionsUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nl.pim16aap2.bigdoors.UnitTestUtil.newStructureBaseBuilder;

@Flogger
public class SQLiteJDBCDriverConnectionTest
{
    /**
     * Name of structure 1.
     */
    private static final String STRUCTURE_1_NAME = "random_door_name";

    /**
     * Name of structures 2 and 3.
     */
    private static final String STRUCTURES_2_3_NAME = "popular_door_name";

    private static final String DELETE_STRUCTURE_NAME = "delete_meh";

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

    private AbstractStructure structure1;
    private AbstractStructure structure2;
    private AbstractStructure structure3;

    static
    {
        LogInspector.get().clearHistory();
        DB_FILE = Path.of(".", "tests", "test.db");
        DB_FILE_BACKUP = DB_FILE.resolveSibling(DB_FILE.getFileName() + ".BACKUP");
    }

    private IPWorldFactory worldFactory;

    private StructureTypeManager structureTypeManager;

    private StructureBaseBuilder structureBaseBuilder;

    private StructureRegistry structureRegistry;

    @Mock
    private RestartableHolder restartableHolder;

    @Mock
    private DebuggableRegistry debuggableRegistry;

    @Mock
    private LocalizationManager localizationManager;

    @BeforeEach
    void beforeEach()
        throws Exception
    {
        MockitoAnnotations.openMocks(this);

        worldFactory = new TestPWorldFactory();
        structureRegistry = StructureRegistry.unCached(
            restartableHolder, debuggableRegistry, Mockito.mock(StructureDeletionManager.class));

        structureTypeManager = new StructureTypeManager(restartableHolder, debuggableRegistry, localizationManager);

        final var builderResult = newStructureBaseBuilder();
        builderResult.assistedFactoryMocker().setMock(StructureRegistry.class, structureRegistry);
        structureBaseBuilder = builderResult.structureBaseBuilder();

        initStructures();

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
            log.atSevere().withCause(exception).log("Failed to move database file to finished file!");
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

    private void deleteStructureTypes()
    {
        // Just make sure it still exists, to make debugging easier.
        Assertions.assertTrue(storage.getStructure(13L).isPresent());
        Assertions.assertTrue(storage.deleteStructureType(StructureTypePortcullis.get()));
        Assertions.assertTrue(storage.getStructure(11L).isPresent());
        Assertions.assertTrue(storage.getStructure(12L).isPresent());
        Assertions.assertFalse(storage.getStructure(13L).isPresent());
    }

    private void testStructureTypes()
    {
        deleteStructureTypes();
    }

    private void registerStructureTypes()
    {
        structureTypeManager.registerStructureType(StructureTypeBigDoor.get());
        structureTypeManager.registerStructureType(StructureTypePortcullis.get());
        structureTypeManager.registerStructureType(StructureTypeDrawbridge.get());
    }

    /**
     * Runs all tests.
     */
    @Test
    void runTests()
        throws IllegalAccessException, NoSuchFieldException
    {
        registerStructureTypes();
        insertStructures();
        verifyStructures();
        partialIdentifiersFromName();
        auxiliaryMethods();
        modifyStructures();

        testStructureTypes();
        failures();

        insertBulkStructures();
        partialIdentifiersFromId();
    }

    private void insertBulkStructures()
    {
        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(structure3).isPresent());
    }

    /**
     * Tests inserting structures in the database.
     */
    public void insertStructures()
    {
        Assertions.assertTrue(storage.insert(structure1).isPresent());
        Assertions.assertTrue(storage.insert(structure2).isPresent());
        Assertions.assertTrue(storage.insert(structure3).isPresent());
    }

    /**
     * Checks if a structure was successfully added to the database and that all data in intact.
     *
     * @param structure
     *     The structure to verify.
     */
    private void testRetrieval(AbstractStructure structure)
    {
        Assertions.assertNotNull(storage);
        Assertions.assertNotNull(structure);
        Assertions.assertNotNull(structure.getPrimeOwner().toString());
        Assertions.assertNotNull(structure.getName());

        List<AbstractStructure> test = storage.getStructures(structure.getPrimeOwner().pPlayerData().getUUID(),
                                                             structure.getName());
        Assertions.assertEquals(1, test.size());

        Assertions.assertEquals(structure.getPrimeOwner(), test.get(0).getPrimeOwner());

        if (!structure.equals(test.get(0)))
            Assertions.fail(
                "Data of retrieved structure is not the same! ID = " + structure.getUid() + ", name = " +
                    structure.getName() +
                    ", found ID = " + test.get(0).getUid() + ", found name = " + test.get(0).getName());
    }

    /**
     * Verifies that the data of all structures that have been added to the database so far is correct.
     */
    public void verifyStructures()
    {
        testRetrieval(structure1);
        testRetrieval(structure2);
        testRetrieval(structure3);
    }

    public void partialIdentifiersFromName()
    {
        Assertions.assertEquals(List.of(new DatabaseManager.StructureIdentifier(12L, "popular_door_name"),
                                        new DatabaseManager.StructureIdentifier(13L, "popular_door_name")),
                                storage.getPartialIdentifiers("popular_", null, PermissionLevel.NO_PERMISSION));

        final IPPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(List.of(new DatabaseManager.StructureIdentifier(12L, "popular_door_name")),
                                storage.getPartialIdentifiers("popular_", player1, PermissionLevel.NO_PERMISSION));
    }

    public void partialIdentifiersFromId()
    {
        Assertions.assertEquals(List.of(new DatabaseManager.StructureIdentifier(25L, "popular_door_name"),
                                        new DatabaseManager.StructureIdentifier(26L, "popular_door_name"),
                                        new DatabaseManager.StructureIdentifier(27L, "popular_door_name"),
                                        new DatabaseManager.StructureIdentifier(28L, "popular_door_name"),
                                        new DatabaseManager.StructureIdentifier(29L, "popular_door_name")),
                                storage.getPartialIdentifiers("2", null, PermissionLevel.NO_PERMISSION));

        final IPPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(List.of(new DatabaseManager.StructureIdentifier(11L, "random_door_name"),
                                        new DatabaseManager.StructureIdentifier(12L, "popular_door_name")),
                                storage.getPartialIdentifiers("1", player1, PermissionLevel.NO_PERMISSION));
    }

    /**
     * Tests the basic SQL methods.
     */
    public void auxiliaryMethods()
    {
        // Check simple methods.
        Assertions.assertEquals(1, storage.getStructureCountForPlayer(PLAYER_DATA_1.getUUID(), STRUCTURE_1_NAME));
        Assertions.assertEquals(2, storage.getStructureCountForPlayer(PLAYER_DATA_1.getUUID()));
        Assertions.assertEquals(1, storage.getStructureCountForPlayer(PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getStructureCountByName(STRUCTURE_1_NAME));
        Assertions.assertTrue(storage.getStructure(PLAYER_DATA_1.getUUID(), 11L).isPresent());
        Assertions.assertEquals(structure1, storage.getStructure(PLAYER_DATA_1.getUUID(), 11L).get());
        Assertions.assertFalse(storage.getStructure(PLAYER_DATA_1.getUUID(), 13L).isPresent());
        final Optional<AbstractStructure> testStructure1 = storage.getStructure(11L);
        Assertions.assertTrue(testStructure1.isPresent());
        Assertions.assertEquals(structure1.getPrimeOwner(), testStructure1.get().getPrimeOwner());
        Assertions.assertEquals(structure1, testStructure1.get());
        Assertions.assertFalse(storage.getStructure(9999999).isPresent());
        Assertions.assertTrue(storage.isBigDoorsWorld(WORLD_NAME));
        Assertions.assertFalse(storage.isBigDoorsWorld("fakeWorld"));

        Assertions.assertEquals(1, storage.getOwnerCountOfStructure(11L));

        long chunkId = Util.getChunkId(structure1.getPowerBlock());
        Assertions.assertEquals(3, storage.getStructuresInChunk(chunkId).size());

        // Check if adding owners works correctly.
        UnitTestUtil.optionalEquals(1, storage.getStructure(11L), (structure) -> structure.getOwners().size());

        // Try adding playerData2 as owner of structure 2.
        Assertions.assertTrue(storage.addOwner(12L, PLAYER_DATA_2, PermissionLevel.ADMIN));

        // Try adding player 1 as owner of structure 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(12L, PLAYER_DATA_1, PermissionLevel.CREATOR));

        // Try adding player 2 as owner of structure 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(12L, PLAYER_DATA_2, PermissionLevel.CREATOR));

        // Try adding a player that is not in the database yet as owner.
        UnitTestUtil.optionalEquals(1, storage.getStructure(11L), (structure) -> structure.getOwners().size());
        Assertions.assertTrue(storage.addOwner(11L, PLAYER_DATA_3, PermissionLevel.ADMIN));
        UnitTestUtil.optionalEquals(2, storage.getStructure(11L), (structure) -> structure.getOwners().size());

        // Verify the permission level of player 2 over structure 2.
        UnitTestUtil.optionalEquals(PermissionLevel.ADMIN, storage.getStructure(12L),
                                    (structure) -> structure.getOwner(PLAYER_DATA_2.getUUID())
                                                            .map(StructureOwner::permission)
                                                            .orElse(PermissionLevel.NO_PERMISSION));
        // Verify there are only 2 owners of structure 2 (player 1 didn't get copied).
        UnitTestUtil.optionalEquals(2, storage.getStructure(12L), (structure) -> structure.getOwners().size());

        // Verify that player 2 is the creator of exactly 1 structure.
        Assertions.assertEquals(1, storage.getStructures(PLAYER_DATA_2.getUUID(), PermissionLevel.CREATOR).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 structures (structure 3 (0) and structure 2 (1)).
        Assertions.assertEquals(2, storage.getStructures(PLAYER_DATA_2.getUUID(), PermissionLevel.ADMIN).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 structures,
        // with the name shared between structures 2 and 3.
        Assertions.assertEquals(2,
                                storage.getStructures(PLAYER_DATA_2.getUUID(), STRUCTURES_2_3_NAME,
                                                      PermissionLevel.ADMIN)
                                       .size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 structure,
        // with the name shared between structures 2 and 3.
        Assertions.assertEquals(1, storage.getStructures(
                                              PLAYER_DATA_2.getUUID(), STRUCTURES_2_3_NAME, PermissionLevel.CREATOR)
                                          .size());

        // Verify that adding an existing owner overrides the permission level.
        Assertions.assertTrue(storage.addOwner(12L, PLAYER_DATA_2, PermissionLevel.USER));
        UnitTestUtil.optionalEquals(PermissionLevel.USER, storage.getStructure(12L),
                                    (structure) -> structure.getOwner(PLAYER_DATA_2.getUUID())
                                                            .map(StructureOwner::permission)
                                                            .orElse(PermissionLevel.NO_PERMISSION));

        // Remove player 2 as owner of structure 2.
        Assertions.assertTrue(storage.removeOwner(12L, PLAYER_DATA_2.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getStructure(12L), (structure) -> structure.getOwners().size());

        // Try to remove player 1 (creator) of structure 2. This is not allowed.
        Assertions.assertFalse(storage.removeOwner(12L, PLAYER_DATA_1.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getStructure(12L), (structure) -> structure.getOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 structure, with the name shared between structures 2 and 3. This will be structure 3.
        Assertions.assertEquals(1, storage.getStructures(
                                              PLAYER_DATA_2.getUUID(), STRUCTURES_2_3_NAME, PermissionLevel.ADMIN)
                                          .size());

        // Verify that player 1 is owner of exactly 1 structure with the name shared between structures 2 and 3.
        Assertions.assertEquals(1, storage.getStructures(PLAYER_DATA_1.getUUID(), STRUCTURES_2_3_NAME).size());

        // Verify that player 1 owns exactly 2 structures.
        Assertions.assertEquals(2, storage.getStructures(PLAYER_DATA_1.getUUID()).size());

        // Verify that there are exactly 2 structures with the name shared between structures 2 and 3 in the database.
        Assertions.assertEquals(2, storage.getStructures(STRUCTURES_2_3_NAME).size());

        // Insert a copy of structure 1 in the database (will have structureUID = 14L).
        Assertions.assertTrue(storage.insert(structure1).isPresent());

        // Verify there are now exactly 2 structures with the name of structure 1 in the database.
        Assertions.assertEquals(2, storage.getStructures(STRUCTURE_1_NAME).size());

        // Remove the just-added copy of structure 1 (structureUID = 14L) from the database.
        Assertions.assertTrue(storage.removeStructure(14L));

        // Verify that after removal of the copy of structure 1 (structureUID = 14L),
        // there is now exactly 1 structure named STRUCTURE_1_NAME in the database again.
        Assertions.assertEquals(1, storage.getStructures(STRUCTURE_1_NAME).size());

        // Verify that player 2 cannot delete structures they do not own (structure 11L belongs to player 1).
        Assertions.assertFalse(storage.removeOwner(11L, PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getStructures(STRUCTURE_1_NAME).size());

        // Add 10 copies of structure3 with a different name to the database.
        structure3.setName(DELETE_STRUCTURE_NAME);
        // Verify there are currently exactly 0 structures with this different name in the database.
        Assertions.assertEquals(0, storage.getStructures(DELETE_STRUCTURE_NAME).size());

        for (int idx = 0; idx < 10; ++idx)
            Assertions.assertTrue(storage.insert(structure3).isPresent());

        // Verify there are now exactly 10 structures with this different name in the database.
        Assertions.assertEquals(10, storage.getStructures(DELETE_STRUCTURE_NAME).size());

        // Remove all 10 structures we just added (owned by player 2) and verify there are exactly 0
        // entries of the structure with the new name after batch removal. Also revert the name change of structure 3.
        Assertions.assertTrue(storage.removeStructures(PLAYER_DATA_2.getUUID(), DELETE_STRUCTURE_NAME));
        Assertions.assertEquals(0, storage.getStructures(DELETE_STRUCTURE_NAME).size());
        Assertions.assertTrue(storage.getStructure(13L).isPresent());
        structure3.setName(storage.getStructure(13L).get().getName());


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

        chunkId = Util.getChunkId(structure1.getPowerBlock());
        final Int2ObjectMap<LongList> powerBlockData = storage.getPowerBlockData(chunkId);
        Assertions.assertNotNull(powerBlockData);
        final List<List<Long>> entries = new ArrayList<>(powerBlockData.values());
        Assertions.assertEquals(1, entries.size());
        Assertions.assertEquals(3, entries.get(0).size());
    }

    /**
     * Runs tests of the methods that modify structures in the database.
     */
    public void modifyStructures()
    {
        StructureSerializer<?> serializer =
            Assertions.assertDoesNotThrow(() -> new StructureSerializer<>(structure3.getType().getStructureClass()));
        Assertions.assertNotNull(serializer);

        // Test (un)locking (i.e. syncing base data).
        {
            structure3.setLocked(true);
            Assertions.assertTrue(storage.syncStructureData(structure3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(structure3))));
            UnitTestUtil.optionalEquals(true, storage.getStructure(13L), AbstractStructure::isLocked);

            structure3.setLocked(false);
            Assertions.assertTrue(storage.syncStructureData(structure3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(structure3))));
            UnitTestUtil.optionalEquals(false, storage.getStructure(13L), AbstractStructure::isLocked);
        }

        // Test syncing all data.
        {
            Portcullis pc = ((Portcullis) structure3);

            // Save the current data
            final MovementDirection oldDir = structure3.getOpenDir();
            final MovementDirection newDir = MovementDirection.getOpposite(oldDir);
            Assertions.assertNotSame(oldDir, newDir);

            final Vector3Di oldPowerBlock = structure3.getPowerBlock();
            final Vector3Di newPowerBlock = new Vector3Di(oldPowerBlock.x(),
                                                          (oldPowerBlock.x() + 30) % 256,
                                                          oldPowerBlock.z());

            final Vector3Di oldMin = structure3.getMinimum();
            final Vector3Di oldMax = structure3.getMaximum();
            final Vector3Di newMin = oldMin.add(0, 20, 10);
            final Vector3Di newMax = oldMax.add(40, 0, 20);
            Assertions.assertNotSame(oldMin, newMin);
            Assertions.assertNotSame(oldMax, newMax);

            final boolean isLocked = structure3.isLocked();
            final boolean isOpen = structure3.isOpen();


            // update some general data.
            structure3.setLocked(!isLocked);
            structure3.setOpen(!isOpen);
            structure3.setPowerBlock(newPowerBlock);
            structure3.setCoordinates(newMin, newMax);
            structure3.setOpenDir(newDir);


            // Update some type-specific data
            final int blocksToMove = pc.getBlocksToMove();
            final int newBlocksToMove = blocksToMove * 2;
            Assertions.assertNotSame(0, blocksToMove);
            pc.setBlocksToMove(newBlocksToMove);

            Assertions.assertTrue(storage.syncStructureData(structure3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(structure3))));

            Optional<AbstractStructure> retrievedOpt = storage.getStructure(13L);
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
            structure3.setLocked(isLocked);
            structure3.setOpen(isOpen);
            structure3.setPowerBlock(oldPowerBlock);
            structure3.setCoordinates(oldMin, oldMax);
            structure3.setOpenDir(oldDir);

            // Reset type-specific data
            pc.setBlocksToMove(blocksToMove);

            Assertions.assertTrue(storage.syncStructureData(structure3.getSnapshot(), Assertions
                .assertDoesNotThrow(() -> serializer.serialize(structure3))));
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

        AssertionsUtil.assertThrowablesLogged(() -> storage.getStructure(PLAYER_DATA_1.getUUID(), 11L),
                                              LogSiteStackTrace.class);

        // Set the database state to enabled again and verify that it's now possible to retrieve structures again.
        databaseLock.set(storage, IStorage.DatabaseState.OK);
        Assertions.assertTrue(storage.getStructure(PLAYER_DATA_1.getUUID(), 11L).isPresent());
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
    {
        storage = new SQLiteJDBCDriverConnection(DB_FILE, structureBaseBuilder, structureRegistry, structureTypeManager,
                                                 worldFactory,
                                                 debuggableRegistry);
    }

    private void initStructures()
    {
        Vector3Di min = new Vector3Di(144, 75, 153);
        Vector3Di max = new Vector3Di(144, 131, 167);
        Vector3Di powerBlock = new Vector3Di(144, 75, 153);
        Vector3Di rotationPoint = new Vector3Di(144, 75, 153);
        structure1 = new BigDoor(
            structureBaseBuilder
                .builder()
                .uid(11L).name(STRUCTURE_1_NAME).cuboid(min, max)
                .rotationPoint(rotationPoint)
                .powerBlock(powerBlock)
                .world(WORLD).isOpen(false).isLocked(false)
                .openDir(MovementDirection.EAST)
                .primeOwner(
                    new StructureOwner(11L, PermissionLevel.CREATOR, PLAYER_DATA_1))
                .build());

        min = new Vector3Di(144, 75, 168);
        max = new Vector3Di(144, 131, 182);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);
        boolean modeUp = true;

        structure2 = new Drawbridge(
            structureBaseBuilder
                .builder()
                .uid(12L).name(STRUCTURES_2_3_NAME).cuboid(min, max)
                .rotationPoint(rotationPoint)
                .powerBlock(powerBlock).world(WORLD).isOpen(false)
                .isLocked(false).openDir(MovementDirection.NONE)
                .primeOwner(
                    new StructureOwner(12L, PermissionLevel.CREATOR, PLAYER_DATA_1))
                .build(),
            modeUp);

        min = new Vector3Di(144, 70, 168);
        max = new Vector3Di(144, 151, 112);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);
        int blocksToMove = 8;
        structure3 = new Portcullis(
            structureBaseBuilder
                .builder()
                .uid(13L).name(STRUCTURES_2_3_NAME).cuboid(min, max)
                .rotationPoint(rotationPoint)
                .powerBlock(powerBlock).world(WORLD).isOpen(false)
                .isLocked(false).openDir(MovementDirection.UP)
                .primeOwner(
                    new StructureOwner(13L, PermissionLevel.CREATOR, PLAYER_DATA_2))
                .build(),
            blocksToMove);
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
