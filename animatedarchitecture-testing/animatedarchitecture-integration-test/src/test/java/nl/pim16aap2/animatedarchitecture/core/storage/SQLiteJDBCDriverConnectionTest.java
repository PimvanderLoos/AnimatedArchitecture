package nl.pim16aap2.animatedarchitecture.core.storage;

import com.google.common.flogger.LogSiteStackTrace;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongList;
import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.LimitContainer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.storage.sqlite.DataSourceInfoSQLite;
import nl.pim16aap2.animatedarchitecture.core.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureRegistry;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSerializer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.LocationUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.BigDoor;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.StructureTypeBigDoor;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.Drawbridge;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.StructureTypeDrawbridge;
import nl.pim16aap2.animatedarchitecture.structures.portcullis.Portcullis;
import nl.pim16aap2.animatedarchitecture.structures.portcullis.StructureTypePortcullis;
import nl.pim16aap2.animatedarchitecture.testimplementations.TestWorld;
import nl.pim16aap2.animatedarchitecture.testimplementations.TestWorldFactory;
import nl.pim16aap2.testing.logging.LogAssertionsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.newStructureBaseBuilder;
import static nl.pim16aap2.testing.logging.LogAssertionsUtil.MessageComparisonMethod;

@WithLogCapture
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    private static final PlayerData PLAYER_DATA_1 = new PlayerData(
        UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"),
        "pim16aap2",
        new LimitContainer(10, 11, 12, 13),
        true,
        true
    );

    private static final PlayerData PLAYER_DATA_2 = new PlayerData(
        UUID.fromString("af5c6f36-445d-3786-803d-c2e3ba0dc3ed"),
        "TestBoiii",
        new LimitContainer(20, 22, 24, null),
        true,
        false
    );

    private static final PlayerData PLAYER_DATA_3 = new PlayerData(
        UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f"),
        "thirdWheel",
        new LimitContainer(30, 33, 36, 39),
        false,
        true
    );

    private static final IWorld WORLD = new TestWorld(WORLD_NAME);

    private static final Path DB_FILE;
    private static final Path DB_FILE_BACKUP;
    private static final DataSourceInfoSQLite DATA_SOURCE_INFO;

    private SQLiteJDBCDriverConnection storage;

    private BigDoor structure1;
    private Drawbridge structure2;
    private Portcullis structure3;

    static
    {
        DB_FILE = Path.of(".", "tests", "test.db");
        DB_FILE_BACKUP = DB_FILE.resolveSibling(DB_FILE.getFileName() + ".BACKUP");
        DATA_SOURCE_INFO = new DataSourceInfoSQLite(DB_FILE);
    }

    private IWorldFactory worldFactory;

    private StructureTypeManager structureTypeManager;

    private StructureBaseBuilder structureBaseBuilder;

    private StructureRegistry structureRegistry;

    @Mock
    private DebuggableRegistry debuggableRegistry;

    @Mock
    private LocalizationManager localizationManager;

    private FlywayManager flywayManager;

    @BeforeEach
    void beforeEach()
        throws Exception
    {
        flywayManager = new FlywayManager(
            DB_FILE.getParent(),
            getClass().getClassLoader(),
            DATA_SOURCE_INFO,
            debuggableRegistry
        );

        worldFactory = new TestWorldFactory();
        structureRegistry =
            StructureRegistry.unCached(debuggableRegistry, Mockito.mock(StructureDeletionManager.class));

        structureTypeManager = new StructureTypeManager(debuggableRegistry, localizationManager);

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
            Files.createDirectories(DB_FILE.getParent());
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
            exception.printStackTrace();
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
        Assertions.assertTrue(storage.getStructure(3L).isPresent());
        Assertions.assertTrue(storage.deleteStructureType(StructureTypePortcullis.get()));
        Assertions.assertTrue(storage.getStructure(1L).isPresent());
        Assertions.assertTrue(storage.getStructure(2L).isPresent());
        Assertions.assertFalse(storage.getStructure(3L).isPresent());
    }

    private void testStructureTypes()
    {
        deleteStructureTypes();
    }

    private void registerStructureTypes()
    {
        structureTypeManager.register(StructureTypeBigDoor.get());
        structureTypeManager.register(StructureTypePortcullis.get());
        structureTypeManager.register(StructureTypeDrawbridge.get());
    }

    private void resetLogCaptor(LogCaptor logCaptor)
    {
        logCaptor.clearLogs();
        logCaptor.setLogLevelToInfo();
        logCaptor.enableConsoleOutput();
    }

    /**
     * Runs all tests.
     */
    @Test
    void runTests(LogCaptor logCaptor)
        throws Exception
    {
        // Start with a reset, so we can ensure all methods use the same settings.
        resetLogCaptor(logCaptor);

        registerStructureTypes();
        resetLogCaptor(logCaptor);

        logCaptor.setLogLevelToTrace();
        logCaptor.enableConsoleOutput();

        insertStructures();
        resetLogCaptor(logCaptor);

        verifyStructures();
        resetLogCaptor(logCaptor);

        partialIdentifiersFromName();
        resetLogCaptor(logCaptor);

        auxiliaryMethods();
        resetLogCaptor(logCaptor);

        modifyStructures();
        resetLogCaptor(logCaptor);

        testStructureTypes();
        resetLogCaptor(logCaptor);

        failures(logCaptor);
        resetLogCaptor(logCaptor);

        insertBulkStructures();
        resetLogCaptor(logCaptor);

        partialIdentifiersFromId();
        resetLogCaptor(logCaptor);
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

        final UUID ownerUUID = structure.getPrimeOwner().playerData().getUUID();

        final Optional<PlayerData> playerData = storage.getPlayerData(ownerUUID);
        Assertions.assertTrue(playerData.isPresent());
        Assertions.assertEquals(structure.getPrimeOwner().playerData(), playerData.get());

        final List<AbstractStructure> test = storage.getStructures(ownerUUID, structure.getName());

        Assertions.assertEquals(
            1,
            test.size(),
            "Failed to find structure for prime owner with UUID = " + structure.getPrimeOwner().playerData().getUUID() +
                ", and structure name = '" + structure.getName() + "'! Found " + test.size() + " structures!"
        );

        Assertions.assertEquals(structure.getPrimeOwner(), test.getFirst().getPrimeOwner());

        if (!structure.equals(test.getFirst()))
            Assertions.fail(
                "Data of retrieved structure is not the same!" +
                    " ID = " + structure.getUid() +
                    ", name = " + structure.getName() +
                    ", found ID = " + test.getFirst().getUid() +
                    ", found name = " + test.getFirst().getName());
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
        Assertions.assertEquals(
            List.of(
                new DatabaseManager.StructureIdentifier(2L, "popular_door_name"),
                new DatabaseManager.StructureIdentifier(3L, "popular_door_name")),
            storage.getPartialIdentifiers("popular_", null, PermissionLevel.NO_PERMISSION)
        );

        final IPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(
            List.of(new DatabaseManager.StructureIdentifier(2L, "popular_door_name")),
            storage.getPartialIdentifiers("popular_", player1, PermissionLevel.NO_PERMISSION)
        );
    }

    public void partialIdentifiersFromId()
    {
        Assertions.assertEquals(
            List.of(
                new DatabaseManager.StructureIdentifier(1L, "random_door_name"),
                new DatabaseManager.StructureIdentifier(15L, "popular_door_name"),
                new DatabaseManager.StructureIdentifier(16L, "popular_door_name"),
                new DatabaseManager.StructureIdentifier(17L, "popular_door_name"),
                new DatabaseManager.StructureIdentifier(18L, "popular_door_name"),
                new DatabaseManager.StructureIdentifier(19L, "popular_door_name")),
            storage.getPartialIdentifiers("1", null, PermissionLevel.NO_PERMISSION)
        );

        final IPlayer player1 = createPlayer(PLAYER_DATA_1);
        Assertions.assertEquals(
            List.of(new DatabaseManager.StructureIdentifier(1L, "random_door_name")),
            storage.getPartialIdentifiers("1", player1, PermissionLevel.NO_PERMISSION)
        );
    }

    /**
     * Tests the basic SQL methods.
     */
    public void auxiliaryMethods()
    {
        // Check simple methods.
        Assertions.assertEquals(1, storage.getStructuresOfType(StructureTypeBigDoor.get().getFullKey()).size());
        Assertions.assertEquals(
            1,
            storage.getStructuresOfType(
                StructureTypePortcullis.get().getFullKey(),
                StructureTypePortcullis.get().getVersion()).size()
        );
        Assertions.assertEquals(1, storage.getStructureCountForPlayer(PLAYER_DATA_1.getUUID(), STRUCTURE_1_NAME));
        Assertions.assertEquals(2, storage.getStructureCountForPlayer(PLAYER_DATA_1.getUUID()));
        Assertions.assertEquals(1, storage.getStructureCountForPlayer(PLAYER_DATA_2.getUUID()));
        Assertions.assertEquals(1, storage.getStructureCountByName(STRUCTURE_1_NAME));
        Assertions.assertTrue(storage.getStructure(PLAYER_DATA_1.getUUID(), 1L).isPresent());
        Assertions.assertEquals(structure1, storage.getStructure(PLAYER_DATA_1.getUUID(), 1L).get());
        Assertions.assertFalse(storage.getStructure(PLAYER_DATA_1.getUUID(), 3L).isPresent());
        final Optional<AbstractStructure> testStructure1 = storage.getStructure(1L);
        Assertions.assertTrue(testStructure1.isPresent());
        Assertions.assertEquals(structure1.getPrimeOwner(), testStructure1.get().getPrimeOwner());
        Assertions.assertEquals(structure1, testStructure1.get());
        Assertions.assertFalse(storage.getStructure(9999999).isPresent());
        Assertions.assertTrue(storage.isAnimatedArchitectureWorld(WORLD_NAME));
        Assertions.assertFalse(storage.isAnimatedArchitectureWorld("fakeWorld"));

        Assertions.assertEquals(1, storage.getOwnerCountOfStructure(1L));

        long chunkId = LocationUtil.getChunkId(structure1.getPowerBlock());
        Assertions.assertEquals(3, storage.getStructuresInChunk(chunkId).size());

        // Check if adding owners works correctly.
        UnitTestUtil.optionalEquals(1, storage.getStructure(1L), (structure) -> structure.getOwners().size());

        // Try adding playerData2 as owner of structure 2.
        Assertions.assertTrue(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.ADMIN));

        // Try adding player 1 as owner of structure 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, PLAYER_DATA_1, PermissionLevel.CREATOR));

        // Try adding player 2 as owner of structure 2, while player 1 is already the creator! This is not allowed.
        Assertions.assertFalse(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.CREATOR));

        // Try adding a player that is not in the database yet as owner.
        UnitTestUtil.optionalEquals(1, storage.getStructure(1L), (structure) -> structure.getOwners().size());
        Assertions.assertTrue(storage.addOwner(1L, PLAYER_DATA_3, PermissionLevel.ADMIN));
        UnitTestUtil.optionalEquals(2, storage.getStructure(1L), (structure) -> structure.getOwners().size());

        // Verify the permission level of player 2 over structure 2.
        UnitTestUtil.optionalEquals(
            PermissionLevel.ADMIN,
            storage.getStructure(2L),
            (structure) -> structure
                .getOwner(PLAYER_DATA_2.getUUID())
                .map(StructureOwner::permission)
                .orElse(PermissionLevel.NO_PERMISSION)
        );

        // Verify there are only 2 owners of structure 2 (player 1 didn't get copied).
        UnitTestUtil.optionalEquals(2, storage.getStructure(2L), (structure) -> structure.getOwners().size());

        // Verify that player 2 is the creator of exactly 1 structure.
        Assertions.assertEquals(1, storage.getStructures(PLAYER_DATA_2.getUUID(), PermissionLevel.CREATOR).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 structures
        // (structure 3 (0) and structure 2 (1)).
        Assertions.assertEquals(2, storage.getStructures(PLAYER_DATA_2.getUUID(), PermissionLevel.ADMIN).size());

        // Verify that player 2 is owner with permission level <= 1 of exactly 2 structures,
        // with the name shared between structures 2 and 3.
        Assertions.assertEquals(
            2,
            storage.getStructures(PLAYER_DATA_2.getUUID(), STRUCTURES_2_3_NAME, PermissionLevel.ADMIN).size()
        );

        // Verify that player 2 is owner with permission level <= 1 of exactly 1 structure,
        // with the name shared between structures 2 and 3.
        Assertions.assertEquals(
            1,
            storage.getStructures(PLAYER_DATA_2.getUUID(), STRUCTURES_2_3_NAME, PermissionLevel.CREATOR).size()
        );

        // Verify that adding an existing owner overrides the permission level.
        Assertions.assertTrue(storage.addOwner(2L, PLAYER_DATA_2, PermissionLevel.USER));

        UnitTestUtil.optionalEquals(
            PermissionLevel.USER,
            storage.getStructure(2L),
            (structure) -> structure
                .getOwner(PLAYER_DATA_2.getUUID())
                .map(StructureOwner::permission)
                .orElse(PermissionLevel.NO_PERMISSION)
        );

        // Remove player 2 as owner of structure 2.
        Assertions.assertTrue(storage.removeOwner(2L, PLAYER_DATA_2.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getStructure(2L), (structure) -> structure.getOwners().size());

        // Try to remove player 1 (creator) of structure 2. This is not allowed.
        Assertions.assertFalse(storage.removeOwner(2L, PLAYER_DATA_1.getUUID()));
        UnitTestUtil.optionalEquals(1, storage.getStructure(2L), (structure) -> structure.getOwners().size());

        // Verify that after deletion of player 2 as owner, player 2 is now owner with permission level <= 1
        // of exactly 1 structure, with the name shared between structures 2 and 3. This will be structure 3.
        Assertions.assertEquals(
            1,
            storage.getStructures(PLAYER_DATA_2.getUUID(), STRUCTURES_2_3_NAME, PermissionLevel.ADMIN).size()
        );

        // Verify that player 1 is owner of exactly 1 structure with the name shared between structures 2 and 3.
        Assertions.assertEquals(1, storage.getStructures(PLAYER_DATA_1.getUUID(), STRUCTURES_2_3_NAME).size());

        // Verify that player 1 owns exactly 2 structures.
        Assertions.assertEquals(2, storage.getStructures(PLAYER_DATA_1.getUUID()).size());

        // Verify that there are exactly 2 structures with the name shared between structures 2 and 3 in the database.
        Assertions.assertEquals(2, storage.getStructures(STRUCTURES_2_3_NAME).size());

        // Insert a copy of structure 1 in the database (will have structureUID = 4L).
        Assertions.assertTrue(storage.insert(structure1).isPresent());

        // Verify there are now exactly 2 structures with the name of structure 1 in the database.
        Assertions.assertEquals(2, storage.getStructures(STRUCTURE_1_NAME).size());

        // Remove the just-added copy of structure 1 (structureUID = 4L) from the database.
        Assertions.assertTrue(storage.removeStructure(4L));

        // Verify that after removal of the copy of structure 1 (structureUID = 4L),
        // there is now exactly 1 structure named STRUCTURE_1_NAME in the database again.
        Assertions.assertEquals(1, storage.getStructures(STRUCTURE_1_NAME).size());

        // Verify that player 2 cannot delete structures they do not own (structure 1L belongs to player 1).
        Assertions.assertFalse(storage.removeOwner(1L, PLAYER_DATA_2.getUUID()));
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
        Assertions.assertTrue(storage.getStructure(3L).isPresent());
        structure3.setName(storage.getStructure(3L).get().getName());


        // Make sure the player name corresponds to the correct UUID.
        Assertions.assertTrue(storage.getPlayerData(PLAYER_DATA_2.getUUID()).isPresent());
        Assertions.assertEquals(PLAYER_DATA_2, storage.getPlayerData(PLAYER_DATA_2.getUUID()).get());
        Assertions.assertEquals(1, storage.getPlayerData(PLAYER_DATA_2.getName()).size());
        Assertions.assertEquals(PLAYER_DATA_2, storage.getPlayerData(PLAYER_DATA_2.getName()).getFirst());
        Assertions.assertEquals(0, storage.getPlayerData(PLAYER_2_NAME_ALT).size());
        Assertions.assertEquals(PLAYER_DATA_2, storage.getPlayerData(PLAYER_DATA_2.getUUID()).get());

        // Update player 2's name to their alt name and make sure the old name is gone and the new one is reachable.
        final PlayerData playerData2ALT = new PlayerData(
            PLAYER_DATA_2.getUUID(),
            PLAYER_2_NAME_ALT,
            new LimitContainer(
                null,
                22,
                null,
                24
            ),
            PLAYER_DATA_2.isOp(),
            PLAYER_DATA_2.hasProtectionBypassPermission()
        );

        Assertions.assertTrue(storage.updatePlayerData(playerData2ALT));

        final Optional<PlayerData> retrieved = storage.getPlayerData(PLAYER_DATA_2.getUUID());
        UnitTestUtil.optionalEquals(playerData2ALT, retrieved);

        Assertions.assertEquals(0, storage.getPlayerData(PLAYER_DATA_2.getName()).size());
        Assertions.assertEquals(1, storage.getPlayerData(playerData2ALT.getName()).size());

        // Revert name change of player 2.
        Assertions.assertTrue(storage.updatePlayerData(PLAYER_DATA_2));

        chunkId = LocationUtil.getChunkId(structure1.getPowerBlock());
        final Int2ObjectMap<LongList> powerBlockData = storage.getPowerBlockData(chunkId);
        Assertions.assertNotNull(powerBlockData);
        final List<List<Long>> entries = new ArrayList<>(powerBlockData.values());
        Assertions.assertEquals(1, entries.size());
        Assertions.assertEquals(3, entries.getFirst().size());
    }

    /**
     * Runs tests of the methods that modify structures in the database.
     */
    public void modifyStructures()
        throws Exception
    {
        StructureSerializer<?> serializer =
            Assertions.assertDoesNotThrow(() -> new StructureSerializer<>(structure3.getType()));
        Assertions.assertNotNull(serializer);

        // Test (un)locking (i.e. syncing base data).
        {
            structure3.setLocked(true);
            Assertions.assertTrue(
                storage.syncStructureData(structure3.getSnapshot(), serializer.serializeTypeData(structure3)));
            UnitTestUtil.optionalEquals(true, storage.getStructure(3L), AbstractStructure::isLocked);

            structure3.setLocked(false);
            Assertions.assertTrue(
                storage.syncStructureData(structure3.getSnapshot(), serializer.serializeTypeData(structure3)));
            UnitTestUtil.optionalEquals(false, storage.getStructure(3L), AbstractStructure::isLocked);
        }

        // Test syncing all data.
        {
            // Save the current data
            final MovementDirection oldDir = structure3.getOpenDir();
            final MovementDirection newDir = MovementDirection.getOpposite(oldDir);
            Assertions.assertNotSame(oldDir, newDir);

            final Vector3Di oldPowerBlock = structure3.getPowerBlock();
            final Vector3Di newPowerBlock = new Vector3Di(
                oldPowerBlock.x(),
                (oldPowerBlock.x() + 30) % 256,
                oldPowerBlock.z()
            );

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
            structure3.setOpenStatus(!isOpen);
            structure3.setPowerBlock(newPowerBlock);
            structure3.setCoordinates(newMin, newMax);
            structure3.setOpenDir(newDir);


            // Update some type-specific data
            final int blocksToMove = structure3.getBlocksToMove();
            final int newBlocksToMove = blocksToMove * 2;
            Assertions.assertNotSame(0, blocksToMove);
            structure3.setBlocksToMove(newBlocksToMove);

            Assertions.assertTrue(
                storage.syncStructureData(structure3.getSnapshot(), serializer.serializeTypeData(structure3)));

            Optional<AbstractStructure> retrievedOpt = storage.getStructure(3L);
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
            structure3.setOpenStatus(isOpen);
            structure3.setPowerBlock(oldPowerBlock);
            structure3.setCoordinates(oldMin, oldMax);
            structure3.setOpenDir(oldDir);

            // Reset type-specific data
            structure3.setBlocksToMove(blocksToMove);

            Assertions.assertTrue(
                storage.syncStructureData(structure3.getSnapshot(), serializer.serializeTypeData(structure3)));
        }
    }

    /**
     * Runs tests to verify that exceptions are caught when they should be and properly handled.
     */
    public void failures(LogCaptor logCaptor)
        throws NoSuchFieldException, IllegalAccessException
    {
        logCaptor.clearLogs();
        logCaptor.setLogLevelToTrace();
        logCaptor.disableConsoleOutput();

        // Set the enabled status of the database to false.
        final Field databaseLock = SQLiteJDBCDriverConnection.class.getDeclaredField("databaseState");
        databaseLock.setAccessible(true);
        databaseLock.set(storage, IStorage.DatabaseState.ERROR);

        storage.getStructure(PLAYER_DATA_1.getUUID(), 1L);

        LogAssertionsUtil.assertThrowableLogged(
            logCaptor,
            0,
            "Database connection could not be created! " +
                "Requested database for state 'OK' while it is actually in state 'ERROR'!",
            LogSiteStackTrace.class
        );

        LogAssertionsUtil.assertThrowableLogged(
            logCaptor,
            1,
            "Failed to execute query: Connection is null!",
            LogSiteStackTrace.class
        );

        LogAssertionsUtil.assertLogged(
            logCaptor,
            2,
            "Executed statement: ",
            MessageComparisonMethod.STARTS_WITH
        );

        // Set the database state to enabled again and verify that it's now possible to retrieve structures again.
        databaseLock.set(storage, IStorage.DatabaseState.OK);
        Assertions.assertTrue(storage.getStructure(PLAYER_DATA_1.getUUID(), 1L).isPresent());
    }

    /**
     * Initializes the storage object.
     */
    private void initStorage()
    {
        storage = new SQLiteJDBCDriverConnection(
            DATA_SOURCE_INFO,
            flywayManager,
            structureBaseBuilder,
            structureRegistry,
            structureTypeManager,
            worldFactory,
            debuggableRegistry
        );
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
                .uid(1L)
                .name(STRUCTURE_1_NAME)
                .cuboid(min, max)
                .powerBlock(powerBlock)
                .world(WORLD)
                .isLocked(false)
                .openDir(MovementDirection.EAST)
                .primeOwner(new StructureOwner(1L, PermissionLevel.CREATOR, PLAYER_DATA_1))
                .ownersOfStructure(null)
                .propertiesOfStructure(
                    StructureTypeBigDoor.get(),
                    Property.ROTATION_POINT, rotationPoint,
                    Property.OPEN_STATUS, false
                )
                .build()
        );

        min = new Vector3Di(144, 75, 168);
        max = new Vector3Di(144, 131, 182);
        rotationPoint = new Vector3Di(144, 75, 153);
        powerBlock = new Vector3Di(144, 75, 153);

        structure2 = new Drawbridge(
            structureBaseBuilder
                .builder()
                .uid(2L)
                .name(STRUCTURES_2_3_NAME)
                .cuboid(min, max)
                .powerBlock(powerBlock)
                .world(WORLD)
                .isLocked(false)
                .openDir(MovementDirection.NONE)
                .primeOwner(new StructureOwner(2L, PermissionLevel.CREATOR, PLAYER_DATA_1))
                .ownersOfStructure(null)
                .propertiesOfStructure(
                    StructureTypeDrawbridge.get(),
                    Property.ROTATION_POINT, rotationPoint,
                    Property.OPEN_STATUS, false
                )
                .build()
        );

        min = new Vector3Di(144, 70, 168);
        max = new Vector3Di(144, 151, 112);
        powerBlock = new Vector3Di(144, 75, 153);
        int blocksToMove = 8;
        structure3 = new Portcullis(
            structureBaseBuilder
                .builder()
                .uid(3L)
                .name(STRUCTURES_2_3_NAME)
                .cuboid(min, max)
                .powerBlock(powerBlock)
                .world(WORLD)
                .isLocked(false)
                .openDir(MovementDirection.UP)
                .primeOwner(new StructureOwner(3L, PermissionLevel.CREATOR, PLAYER_DATA_2))
                .ownersOfStructure(null)
                .propertiesOfStructure(
                    StructureTypePortcullis.get(),
                    Property.OPEN_STATUS, false
                )
                .build(),
            blocksToMove
        );
    }

    private static IPlayer createPlayer(PlayerData data)
    {
        final IPlayer player = Mockito.mock(IPlayer.class);
        Mockito.when(player.getName()).thenReturn(data.getName());
        Mockito.when(player.getUUID()).thenReturn(data.getUUID());
        Mockito.when(player.getPlayerData()).thenReturn(data);
        Mockito.when(player.getLocation()).thenReturn(Optional.empty());
        return player;
    }
}
