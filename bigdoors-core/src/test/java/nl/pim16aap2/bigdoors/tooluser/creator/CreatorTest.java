package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.tooluser.Procedure;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static nl.pim16aap2.bigdoors.UnitTestUtil.*;

class CreatorTest
{
    private IBigDoorsPlatform platform;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Creator creator;

    @Mock
    private IPPlayer player;

    @Mock
    private IEconomyManager economyManager;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        Mockito.when(creator.getPlayer()).thenReturn(player);

        val messages = initMessages();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(platform.getEconomyManager()).thenReturn(economyManager);

        Mockito.when(platform.getMessages()).thenReturn(messages);
    }

    @Test
    void testNameInput()
    {
        val input = "1";
        // Numerical names are not allowed.
        Assertions.assertFalse(creator.completeNamingStep(input));
        Mockito.verify(player).sendMessage("\"" + input + "\" is an invalid door name! Please try again!");

        Assertions.assertTrue(creator.completeNamingStep("newDoor"));
        Mockito.verify(creator).giveTool();
    }

    @Test
    void testFirstLocation()
    {
        val loc = getLocation(12.7, 128, 56.12);

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
        val world = getWorld();
        val worldName = world.getWorldName();
        setField("world", world);

        val secondWorld = getWorld();
        // Different world, so no match!
        Assertions.assertFalse(creator.verifyWorldMatch(Mockito.mock(IPWorld.class)));

        Mockito.when(secondWorld.getWorldName()).thenReturn(worldName);
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

        val limitsManager = Mockito.mock(LimitsManager.class);
        Mockito.when(platform.getLimitsManager()).thenReturn(limitsManager);

        val world = getWorld();

        val vec1 = new Vector3DiConst(12, 128, 56);
        val vec2 = vec1.clone().add(10, 10, 10);
        val cuboid = new Cuboid(vec1, vec2);

        setField("firstPos", vec1);
        setField("world", world);

        val loc = getLocation(vec2, world);

        // Not allowed, because no access to location
        Assertions.assertFalse(creator.setSecondPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
               .thenReturn(OptionalInt.of(cuboid.getVolume() - 1));
        // Not allowed, because the selected area is too big.
        Assertions.assertFalse(creator.setSecondPos(loc));
        Mockito.verify(player).sendMessage(String.format("%s %d %d",
                                                         Message.CREATOR_GENERAL_AREATOOBIG.name(),
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

        val procedure = Mockito.mock(Procedure.class);
        Mockito.doReturn(procedure).when(creator).getProcedure();

        Assertions.assertTrue(creator.confirmPrice(false));
        Mockito.verify(player).sendMessage("CREATOR_GENERAL_CANCELLED");

        Mockito.doReturn(OptionalDouble.empty()).when(creator).getPrice();
        Mockito.doReturn(false).when(creator).buyDoor();

        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(player).sendMessage(Message.CREATOR_GENERAL_INSUFFICIENTFUNDS.name() + " 0");

        var price = 123.41;
        Mockito.doReturn(OptionalDouble.of(price)).when(creator).getPrice();
        Mockito.doReturn(false).when(creator).buyDoor();
        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(player).sendMessage(Message.CREATOR_GENERAL_INSUFFICIENTFUNDS.name() + " " + price);

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
        val doorType = Mockito.mock(DoorType.class);
        val validOpenDirections = Arrays.asList(RotateDirection.EAST, RotateDirection.WEST);
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
        val economyManager = Mockito.mock(IEconomyManager.class);
        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(platform.getEconomyManager()).thenReturn(economyManager);

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);
        val cuboid = new Cuboid(new Vector3DiConst(1, 2, 3), new Vector3DiConst(4, 5, 6));
        setField("cuboid", cuboid);
        Assertions.assertTrue(creator.getPrice().isEmpty());

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt()))
               .thenAnswer(invocation -> OptionalDouble.of(invocation.getArgument(1, Integer.class).doubleValue()));

        val price = creator.getPrice();
        Assertions.assertTrue(price.isPresent());
        Assertions.assertEquals(cuboid.getVolume(), price.getAsDouble());
    }

    @Test
    void testBuyDoor()
    {
        val economyManager = Mockito.mock(IEconomyManager.class);
        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);
        Mockito.when(platform.getEconomyManager()).thenReturn(economyManager);

        val cuboid = new Cuboid(new Vector3DiConst(1, 2, 3), new Vector3DiConst(4, 5, 6));
        setField("cuboid", cuboid);
        Assertions.assertTrue(creator.buyDoor());

        val world = Mockito.mock(IPWorld.class);
        setField("world", world);

        val doorType = Mockito.mock(DoorType.class);
        Mockito.when(creator.getDoorType()).thenReturn(doorType);

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        creator.buyDoor();
        Mockito.verify(economyManager).buyDoor(player, world, doorType, cuboid.getVolume());
    }

    @Test
    void testCompleteSetPowerBlockStep()
    {
        Mockito.doNothing().when(creator).shutdown();

        val limitsManager = Mockito.mock(LimitsManager.class);
        Mockito.when(platform.getLimitsManager()).thenReturn(limitsManager);


        val world = getWorld();

        val cuboidMin = new Vector3DiConst(10, 20, 30);
        val cuboidMax = new Vector3DiConst(40, 50, 60);
        val cuboid = new Cuboid(cuboidMin, cuboidMax);

        val outsideCuboid = getLocation(70, 80, 90, world);
        val insideCuboid = getLocation(25, 35, 45, world);

        setField("cuboid", cuboid);
        setField("world", world);

        Assertions.assertFalse(creator.completeSetPowerBlockStep(getLocation(0, 1, 2)));

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(outsideCuboid));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(insideCuboid));
        Mockito.verify(player).sendMessage(Message.CREATOR_GENERAL_POWERBLOCKINSIDEDOOR.name());

        final double distance = cuboid.getCenter().getDistance(outsideCuboid.getPosition());
        final int lowLimit = (int) (distance - 1);
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit));

        Assertions.assertFalse(creator.completeSetPowerBlockStep(outsideCuboid));
        Mockito.verify(player).sendMessage(String.format("%s %.2f %d",
                                                         Message.CREATOR_GENERAL_POWERBLOCKTOOFAR.name(),
                                                         distance, lowLimit));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit + 10));
        Mockito.when(platform.getBigDoorsToolUtil()).thenReturn(Mockito.mock(IBigDoorsToolUtil.class));
        Assertions.assertTrue(creator.completeSetPowerBlockStep(outsideCuboid));
    }

    @Test
    void testCompleteSetEngineStep()
    {
        val world = getWorld();

        val cuboidMin = new Vector3DiConst(10, 20, 30);
        val cuboidMax = new Vector3DiConst(40, 50, 60);
        val cuboid = new Cuboid(cuboidMin, cuboidMax);

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
        Mockito.verify(player).sendMessage(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT.name());

        Assertions.assertTrue(creator.completeSetEngineStep(getLocation(11, 21, 31, world)));
    }

    @SneakyThrows
    private void setField(final @NotNull String fieldName, final @Nullable Object obj)
    {
        @NotNull val f = Creator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(creator, obj);
    }
}
