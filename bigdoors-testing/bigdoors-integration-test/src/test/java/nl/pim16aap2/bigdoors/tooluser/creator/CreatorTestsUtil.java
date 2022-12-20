package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.DelayedCommand;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.SetBlocksToMoveDelayed;
import nl.pim16aap2.bigdoors.commands.SetOpenDirectionDelayed;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorBaseBuilder;
import nl.pim16aap2.bigdoors.doors.DoorOwner;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.testimplementations.TestPLocationFactory;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
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

public class CreatorTestsUtil
{
    protected final Vector3Di min = new Vector3Di(10, 15, 20);
    protected final Vector3Di max = new Vector3Di(20, 25, 30);
    protected final Vector3Di powerblock = new Vector3Di(40, 40, 40);
    protected final String doorName = "testDoor123";
    protected final IPWorld world = getWorld();
    protected Vector3Di rotationPoint = new Vector3Di(20, 15, 25);
    protected RotateDirection openDirection = RotateDirection.COUNTERCLOCKWISE;

    protected DoorOwner doorOwner;

    protected DoorBaseBuilder doorBaseBuilder;

    protected ILocalizer localizer;

    protected PPlayerData playerData;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    protected IPPlayer player;

    @Mock
    protected DatabaseManager databaseManager;

    @Mock
    protected IEconomyManager economyManager;

    @Mock
    protected IConfigLoader configLoader;

    @Mock
    protected IPermissionsManager permissionsManager;

    @Mock
    protected ToolUserManager toolUserManager;

    protected LimitsManager limitsManager;

    @Mock
    protected IProtectionCompatManager protectionCompatManager;

    @Mock
    protected IBigDoorsToolUtil bigDoorsToolUtil;

    @Mock
    protected DebuggableRegistry debuggableRegistry;

    @Mock
    protected CommandFactory commandFactory;

    protected IPLocationFactory locationFactory = new TestPLocationFactory();

    protected ToolUser.Context context;

    protected DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    private AutoCloseable mocks;

    private void initPlayer()
    {
        final var uuid = UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db5");
        final var name = "user";
        var doorSizeLimit = 8;
        var doorCountLimit = 9;

        playerData = new PPlayerData(uuid, name, doorSizeLimit, doorCountLimit, true, true);

        doorOwner = new DoorOwner(-1, PermissionLevel.CREATOR, playerData);

        Mockito.when(player.getUUID()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getDoorCountLimit()).thenReturn(doorCountLimit);
        Mockito.when(player.getDoorSizeLimit()).thenReturn(doorSizeLimit);
        Mockito.when(player.isOp()).thenReturn(true);
        Mockito.when(player.hasProtectionBypassPermission()).thenReturn(true);
        Mockito.when(player.getDoorSizeLimit()).thenReturn(doorSizeLimit);
        Mockito.when(player.getLocation()).thenReturn(Optional.empty());

        Mockito.when(player.getPPlayerData()).thenReturn(playerData);

        UnitTestUtil.redirectSendMessageText(player);
    }

    @BeforeEach
    @SneakyThrows
    protected void beforeEach()
    {
        mocks = MockitoAnnotations.openMocks(this);

        localizer = UnitTestUtil.initLocalizer();
        limitsManager = new LimitsManager(permissionsManager, configLoader);

        final DoorBase.IFactory doorBaseIFactory =
            new AssistedFactoryMocker<>(DoorBase.class, DoorBase.IFactory.class)
                .setMock(ILocalizer.class, localizer)
                .setMock(DoorRegistry.class,
                         DoorRegistry.unCached(Mockito.mock(RestartableHolder.class), debuggableRegistry))
                .getFactory();
        doorBaseBuilder = new DoorBaseBuilder(doorBaseIFactory);

        context = new ToolUser.Context(
            doorBaseBuilder, localizer, ITextFactory.getSimpleTextFactory(), toolUserManager, databaseManager,
            limitsManager, economyManager, protectionCompatManager, bigDoorsToolUtil, commandFactory);

        initCommands();

        initPlayer();

        final IPPlayerFactory playerFactory = Mockito.mock(IPPlayerFactory.class);
        Mockito.when(playerFactory.create(playerData.getUUID()))
               .thenReturn(CompletableFuture.completedFuture(Optional.of(player)));

        // Immediately return whatever door was being added to the database as if it was successful.
        Mockito.when(databaseManager.addDoor(ArgumentMatchers.any())).thenAnswer(
            (Answer<CompletableFuture<Optional<AbstractDoor>>>) invocation ->
                CompletableFuture.completedFuture(Optional.of((AbstractDoor) invocation.getArguments()[0])));

        Mockito.when(databaseManager.addDoor(ArgumentMatchers.any(AbstractDoor.class), Mockito.any(IPPlayer.class)))
               .thenAnswer((Answer<CompletableFuture<DatabaseManager.DoorInsertResult>>) invocation ->
                   CompletableFuture.completedFuture(new DatabaseManager.DoorInsertResult(
                       Optional.of(invocation.getArgument(0, AbstractDoor.class)), false)));

        Mockito.when(permissionsManager.hasPermission(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);

        Mockito.when(configLoader.maxDoorSize()).thenReturn(OptionalInt.empty());
        Mockito.when(configLoader.maxDoorCount()).thenReturn(OptionalInt.empty());
        Mockito.when(configLoader.maxPowerBlockDistance()).thenReturn(OptionalInt.empty());
        Mockito.when(configLoader.maxBlocksToMove()).thenReturn(OptionalInt.empty());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void initCommands()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<DelayedCommandInputRequest, DelayedCommandInputRequest.IFactory> assistedFactory =
            new AssistedFactoryMocker<>(DelayedCommandInputRequest.class, DelayedCommandInputRequest.IFactory.class)
                .setMock(ILocalizer.class, localizer)
                .setMock(DelayedCommandInputManager.class, delayedCommandInputManager);

        final var commandContext = new DelayedCommand.Context(delayedCommandInputManager, localizer,
                                                              ITextFactory.getSimpleTextFactory(),
                                                              () -> commandFactory);
        final SetOpenDirectionDelayed setOpenDirectionDelayed =
            new SetOpenDirectionDelayed(commandContext, assistedFactory.getFactory());
        Mockito.when(commandFactory.getSetOpenDirectionDelayed()).thenReturn(setOpenDirectionDelayed);

        final SetBlocksToMoveDelayed setBlocksToMoveDelayed =
            new SetBlocksToMoveDelayed(commandContext, assistedFactory.getFactory());
        Mockito.when(commandFactory.getSetBlocksToMoveDelayed()).thenReturn(setBlocksToMoveDelayed);
    }

    @AfterEach
    void cleanup()
        throws Exception
    {
        mocks.close();
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
        return doorBaseBuilder.builder()
                              .uid(-1).name(doorName).cuboid(new Cuboid(min, max)).rotationPoint(rotationPoint)
                              .powerBlock(powerblock).world(world).isOpen(false).isLocked(false)
                              .openDir(openDirection).primeOwner(doorOwner)
                              .build();
    }

    public void applySteps(Creator creator, Object... input)
    {
        for (int idx = 0; idx < input.length; ++idx)
        {
            final Object obj = input[idx];
            final @Nullable String stepName = creator.getCurrentStep().map(IStep::getName).orElse(null);
            Assertions.assertNotNull(stepName);

            Assertions.assertTrue(creator.handleInput(obj),
                                  String.format("IDX: %d, Input: %s, Step: %s", idx, obj, stepName));
        }
    }

    @SneakyThrows
    public void testCreation(Creator creator, AbstractDoor actualDoor, Object... input)
    {
        applySteps(creator, input);
        Mockito.verify(creator.getPlayer(), Mockito.never()).sendMessage("Door creation was cancelled!");
        Mockito.verify(databaseManager).addDoor(actualDoor, player);
    }
}
