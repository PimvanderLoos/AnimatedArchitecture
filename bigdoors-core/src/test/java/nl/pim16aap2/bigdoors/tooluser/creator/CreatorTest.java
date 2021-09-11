package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.doors.DoorBaseFactory;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.tooluser.Procedure;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static nl.pim16aap2.bigdoors.UnitTestUtil.*;

class CreatorTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Creator creator;

    @Mock
    private IPPlayer player;

    @Mock
    private IEconomyManager economyManager;

    @Mock
    private LimitsManager limitsManager;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        final DoorType doorType = Mockito.mock(DoorType.class);

        Mockito.when(creator.getDoorType()).thenReturn(doorType);
        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);

        final IPLogger logger = new BasicPLogger();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);


        final IProtectionCompatManager protectionCompatManager = Mockito.mock(IProtectionCompatManager.class);
        Mockito.when(protectionCompatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(protectionCompatManager.canBreakBlocksBetweenLocs(Mockito.any(), Mockito.any(),
                                                                       Mockito.any(), Mockito.any()))
               .thenReturn(Optional.empty());


        UnitTestUtil.setField(Creator.class, creator, "limitsManager", limitsManager);
        UnitTestUtil.setField(Creator.class, creator, "handler", handler);
        UnitTestUtil.setField(Creator.class, creator, "doorBaseFactory", Mockito.mock(DoorBaseFactory.class));
        UnitTestUtil.setField(Creator.class, creator, "databaseManager", Mockito.mock(DatabaseManager.class));
        UnitTestUtil.setField(Creator.class, creator, "economyManager", economyManager);

        UnitTestUtil.setField(ToolUser.class, creator, "player", player);
        UnitTestUtil.setField(ToolUser.class, creator, "logger", logger);
        UnitTestUtil.setField(ToolUser.class, creator, "localizer", initLocalizer());
        UnitTestUtil.setField(ToolUser.class, creator, "protectionCompatManager", protectionCompatManager);
        UnitTestUtil.setField(ToolUser.class, creator, "bigDoorsToolUtil", Mockito.mock(IBigDoorsToolUtil.class));
        UnitTestUtil.setField(ToolUser.class, creator, "localizer", initLocalizer());
    }

    @Test
    void testNameInput()
    {
        final var input = "1";
        // Numerical names are not allowed.
        Assertions.assertFalse(creator.completeNamingStep(input));
        Mockito.verify(player).sendMessage("creator.base.error.invalid_name " + input);

        Assertions.assertTrue(creator.completeNamingStep("newDoor"));
        Mockito.verify(creator).giveTool();
    }

    @Test
    void testFirstLocation()
    {
        final var loc = getLocation(12.7, 128, 56.12);

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        // No access to location
        Assertions.assertFalse(creator.setFirstPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertTrue(creator.setFirstPos(loc));
        Assertions.assertEquals(loc.getWorld(), creator.world);
        Assertions.assertEquals(new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), creator.firstPos);
    }

    @Test
    void testWorldMatch()
    {
        final var world = getWorld();
        final var worldName = world.worldName();
        setField("world", world);

        final var secondWorld = getWorld();
        // Different world, so no match!
        Assertions.assertFalse(creator.verifyWorldMatch(Mockito.mock(IPWorld.class)));

        Mockito.when(secondWorld.worldName()).thenReturn(worldName);
        // Same world name, so match!
        Assertions.assertTrue(creator.verifyWorldMatch(secondWorld));
    }

    @Test
    void testInit()
    {
        Assertions.assertDoesNotThrow(() -> creator.init());
    }

    @Test
    void testSecondLocation()
    {
        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());

        final var world = getWorld();

        final var vec1 = new Vector3Di(12, 128, 56);
        final var vec2 = vec1.add(10, 10, 10);
        final var cuboid = new Cuboid(vec1, vec2);

        setField("firstPos", vec1);
        setField("world", world);

        final var loc = getLocation(vec2, world);

        // Not allowed, because no access to location
        Assertions.assertFalse(creator.setSecondPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
               .thenReturn(OptionalInt.of(cuboid.getVolume() - 1));
        // Not allowed, because the selected area is too big.
        Assertions.assertFalse(creator.setSecondPos(loc));
        Mockito.verify(player).sendMessage(String.format("creator.base.error.area_too_big %d %d",
                                                         cuboid.getVolume(), cuboid.getVolume() - 1));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
               .thenReturn(OptionalInt.of(cuboid.getVolume() + 1));
        Mockito.doReturn(false).when(creator).playerHasAccessToCuboid(Mockito.any(), Mockito.any());
        // Not allowed, because no access to one or more blocks in the cuboid area.
        Assertions.assertFalse(creator.setSecondPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToCuboid(Mockito.any(), Mockito.any());
        Assertions.assertTrue(creator.setSecondPos(loc));
        Assertions.assertEquals(cuboid, creator.cuboid);
    }

    @Test
    void testConfirmPrice()
    {
        Mockito.doNothing().when(creator).shutdown();

        final var procedure = Mockito.mock(Procedure.class);
        Mockito.doReturn(procedure).when(creator).getProcedure();

        Assertions.assertTrue(creator.confirmPrice(false));
        Mockito.verify(player).sendMessage("creator.base.error.creation_cancelled");

        Mockito.doReturn(OptionalDouble.empty()).when(creator).getPrice();
        Mockito.doReturn(false).when(creator).buyDoor();

        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(player).sendMessage("creator.base.error.insufficient_funds 0");

        var price = 123.41;
        Mockito.doReturn(OptionalDouble.of(price)).when(creator).getPrice();
        Mockito.doReturn(false).when(creator).buyDoor();
        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(player).sendMessage("creator.base.error.insufficient_funds " + price);

        Mockito.doReturn(true).when(creator).buyDoor();
        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(procedure).goToNextStep();
    }

    @Test
    void testSkipPrice()
    {
        Mockito.doReturn(OptionalDouble.empty()).when(creator).getPrice();
        Assertions.assertTrue(creator.skipConfirmPrice());

        Mockito.doReturn(OptionalDouble.of(1)).when(creator).getPrice();
        Assertions.assertFalse(creator.skipConfirmPrice());
    }

    @Test
    void testOpenDirectionStep()
    {
        final var doorType = Mockito.mock(DoorType.class);
        final var validOpenDirections = Arrays.asList(RotateDirection.EAST, RotateDirection.WEST);
        Mockito.when(doorType.getValidOpenDirections()).thenReturn(validOpenDirections);

        Mockito.when(creator.getDoorType()).thenReturn(doorType);

        Assertions.assertFalse(creator.completeSetOpenDirStep("-1"));
        Assertions.assertTrue(creator.completeSetOpenDirStep("0"));
        Assertions.assertTrue(creator.completeSetOpenDirStep("1"));
        Assertions.assertFalse(creator.completeSetOpenDirStep("2"));

        Assertions.assertFalse(creator.completeSetOpenDirStep("NORTH"));
        Assertions.assertTrue(creator.completeSetOpenDirStep("EAST"));
        Assertions.assertFalse(creator.completeSetOpenDirStep("SOUTH"));
        Assertions.assertTrue(creator.completeSetOpenDirStep("WEST"));

        Assertions.assertFalse(creator.completeSetOpenDirStep(""));
    }

    @Test
    void testGetPrice()
    {
        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);
        final var cuboid = new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6));
        setField("cuboid", cuboid);
        Assertions.assertTrue(creator.getPrice().isEmpty());

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt()))
               .thenAnswer(invocation -> OptionalDouble.of(invocation.getArgument(1, Integer.class).doubleValue()));

        final var price = creator.getPrice();
        Assertions.assertTrue(price.isPresent());
        Assertions.assertEquals(cuboid.getVolume(), price.getAsDouble());
    }

    @Test
    void testBuyDoor()
    {
        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);

        final var cuboid = new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6));
        setField("cuboid", cuboid);
        Assertions.assertTrue(creator.buyDoor());

        final var world = Mockito.mock(IPWorld.class);
        setField("world", world);

        final var doorType = Mockito.mock(DoorType.class);
        Mockito.when(creator.getDoorType()).thenReturn(doorType);

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        creator.buyDoor();
        Mockito.verify(economyManager).buyDoor(player, world, doorType, cuboid.getVolume());
    }

    @Test
    void testCompleteSetPowerBlockStep()
    {
        Mockito.doNothing().when(creator).shutdown();

        final var world = getWorld();

        final var cuboidMin = new Vector3Di(10, 20, 30);
        final var cuboidMax = new Vector3Di(40, 50, 60);
        final var cuboid = new Cuboid(cuboidMin, cuboidMax);

        final var outsideCuboid = getLocation(70, 80, 90, world);
        final var insideCuboid = getLocation(25, 35, 45, world);

        setField("cuboid", cuboid);
        setField("world", world);

        Assertions.assertFalse(creator.completeSetPowerBlockStep(getLocation(0, 1, 2)));

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(outsideCuboid));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(insideCuboid));
        Mockito.verify(player).sendMessage("creator.base.error.powerblock_inside_door");

        final double distance = cuboid.getCenter().getDistance(outsideCuboid.getPosition());
        final int lowLimit = (int) (distance - 1);
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit));

        Assertions.assertFalse(creator.completeSetPowerBlockStep(outsideCuboid));
        Mockito.verify(player).sendMessage(String.format("creator.base.error.powerblock_too_far %.2f %d",
                                                         distance, lowLimit));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit + 10));
        Assertions.assertTrue(creator.completeSetPowerBlockStep(outsideCuboid));
    }

    @Test
    void testCompleteSetEngineStep()
    {
        final var world = getWorld();

        final var cuboidMin = new Vector3Di(10, 20, 30);
        final var cuboidMax = new Vector3Di(40, 50, 60);
        final var cuboid = new Cuboid(cuboidMin, cuboidMax);

        setField("world", world);
        setField("cuboid", cuboid);


        // World mismatch, so not allowed
        Assertions.assertFalse(creator.completeSetEngineStep(getLocation(1, 1, 1)));

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        // Location not allowed
        Assertions.assertFalse(creator.completeSetEngineStep(getLocation(1, 1, 1, world)));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        // Point too far away
        Assertions.assertFalse(creator.completeSetEngineStep(getLocation(1, 1, 1, world)));
        Mockito.verify(player).sendMessage("creator.base.error.invalid_rotation_point");

        Assertions.assertTrue(creator.completeSetEngineStep(getLocation(11, 21, 31, world)));
    }

    @SneakyThrows
    private void setField(String fieldName, @Nullable Object obj)
    {
        final var f = Creator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(creator, obj);
    }
}
