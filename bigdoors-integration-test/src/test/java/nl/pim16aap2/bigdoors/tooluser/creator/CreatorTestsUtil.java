package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.testimplementations.TestPLocationFactory;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorldFactory;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nl.pim16aap2.bigdoors.UnitTestUtil.getWorld;
import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;

public class CreatorTestsUtil
{
    protected Vector3Di min = new Vector3Di(10, 15, 20);
    protected Vector3Di max = new Vector3Di(20, 25, 30);
    protected Vector3Di engine = new Vector3Di(20, 15, 25);
    protected Vector3Di powerblock = new Vector3Di(40, 40, 40);
    protected String doorName = "testDoor123";
    protected IPWorld world = getWorld();
    protected IPWorld world2 = getWorld();
    protected RotateDirection openDirection = RotateDirection.COUNTERCLOCKWISE;

    protected DoorOwner doorOwner;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    protected IPPlayer player;

    @Mock
    protected DatabaseManager databaseManager;

    protected IBigDoorsPlatform platform;

    @Mock
    protected PowerBlockManager powerBlockManager;

    @Mock
    protected IEconomyManager economyManager;

    @Mock
    protected IConfigLoader configLoader;

    @Mock
    protected IPermissionsManager permissionsManager;

    protected PPlayerData playerData;

    private void initPlayer()
    {
        val uuid = UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db5");
        val name = "user";
        var doorSizeLimit = 8;
        var doorCountLimit = 9;

        playerData = new PPlayerData(uuid, name, doorSizeLimit, doorCountLimit, true, true);

        doorOwner = new DoorOwner(-1, 0, playerData);

        Mockito.when(player.getUUID()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getDoorCountLimit()).thenReturn(doorCountLimit);
        Mockito.when(player.getDoorSizeLimit()).thenReturn(doorSizeLimit);
        Mockito.when(player.isOp()).thenReturn(true);
        Mockito.when(player.hasProtectionBypassPermission()).thenReturn(true);
        Mockito.when(player.getDoorSizeLimit()).thenReturn(doorSizeLimit);
        Mockito.when(player.getLocation()).thenReturn(Optional.empty());

        Mockito.when(player.getPPlayerData()).thenReturn(playerData);
    }

    @BeforeEach
    protected void beforeEach()
    {
        MockitoAnnotations.openMocks(this);
        platform = initPlatform();
        BigDoors.get().setBigDoorsPlatform(platform);

        Mockito.when(platform.getLimitsManager()).thenReturn(new LimitsManager());

        initPlayer();

        val playerFactory = Mockito.mock(IPPlayerFactory.class);
        Mockito.when(playerFactory.create(playerData.getUUID()))
               .thenReturn(CompletableFuture.completedFuture(Optional.of(player)));

        Mockito.when(platform.getPPlayerFactory()).thenReturn(playerFactory);
        Mockito.when(platform.getDatabaseManager()).thenReturn(databaseManager);
        Mockito.when(platform.getEconomyManager()).thenReturn(economyManager);
        Mockito.when(platform.getPowerBlockManager()).thenReturn(powerBlockManager);
        Mockito.when(platform.getBigDoorsToolUtil()).thenReturn(Mockito.mock(IBigDoorsToolUtil.class));
        Mockito.when(platform.getPLocationFactory()).thenReturn(new TestPLocationFactory());
        Mockito.when(platform.getPWorldFactory()).thenReturn(new TestPWorldFactory());
        Mockito.when(platform.getProtectionCompatManager()).thenReturn(Mockito.mock(IProtectionCompatManager.class));
        Mockito.when(platform.getConfigLoader()).thenReturn(configLoader);
        Mockito.when(platform.getPermissionsManager()).thenReturn(permissionsManager);
        Mockito.when(platform.getDoorRegistry()).thenReturn(DoorRegistry.uncached());
        Mockito.when(platform.getToolUserManager()).thenReturn(Mockito.mock(ToolUserManager.class));

        // Immediately return whatever door was being added to the database as if it was successful.
        Mockito.when(databaseManager.addDoor(ArgumentMatchers.any())).thenAnswer(
            (Answer<CompletableFuture<Optional<AbstractDoor>>>) invocation ->
                CompletableFuture.completedFuture(Optional.of((AbstractDoor) invocation.getArguments()[0])));
        Mockito.when(databaseManager.addDoor(ArgumentMatchers.any(), Mockito.any())).thenAnswer(
            (Answer<CompletableFuture<Optional<AbstractDoor>>>) invocation ->
                CompletableFuture.completedFuture(Optional.of((AbstractDoor) invocation.getArguments()[0])));

        Mockito.when(permissionsManager.hasPermission(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);

        Mockito.when(configLoader.maxDoorSize()).thenReturn(OptionalInt.empty());
        Mockito.when(configLoader.maxDoorCount()).thenReturn(OptionalInt.empty());
        Mockito.when(configLoader.maxPowerBlockDistance()).thenReturn(OptionalInt.empty());
        Mockito.when(configLoader.maxBlocksToMove()).thenReturn(OptionalInt.empty());
    }

    protected void setEconomyEnabled(boolean status)
    {
        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(status);
    }

    protected void setEconomyPrice(double price)
    {
        Mockito.when(economyManager.getPrice(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
               .thenReturn(OptionalDouble.of(price));
    }

    protected void setBuyDoor(boolean status)
    {
        Mockito.when(economyManager.buyDoor(ArgumentMatchers.any(), ArgumentMatchers.any(),
                                            ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
               .thenReturn(status);
    }


    protected DoorBase constructDoorBase()
    {
        return new DoorBase(-1, doorName, new Cuboid(min, max), engine, powerblock,
                            world, false, false, openDirection, doorOwner);
    }

    @SneakyThrows
    public void testCreation(final Creator creator, AbstractDoor actualDoor,
                             final Object... input)
    {
        for (int idx = 0; idx < input.length; ++idx)
        {
            val obj = input[idx];
            val stepName = creator.getCurrentStep().map(IStep::getName).orElse(null);
            Assertions.assertNotNull(stepName);

            Assertions.assertTrue(creator.handleInput(obj),
                                  String.format("IDX: %d, Input: %s, Step: %s", idx, obj, stepName));
        }

        Mockito.verify(creator.getPlayer(), Mockito.never()).sendMessage("Door creation was cancelled!");
        Mockito.verify(databaseManager).addDoor(actualDoor, player);
    }
}
