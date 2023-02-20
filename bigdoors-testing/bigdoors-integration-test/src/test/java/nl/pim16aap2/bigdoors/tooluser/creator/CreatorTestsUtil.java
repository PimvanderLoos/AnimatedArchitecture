package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.core.api.IConfig;
import nl.pim16aap2.bigdoors.core.api.IEconomyManager;
import nl.pim16aap2.bigdoors.core.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.api.PlayerData;
import nl.pim16aap2.bigdoors.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.core.api.factories.ILocationFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPlayerFactory;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.commands.DelayedCommand;
import nl.pim16aap2.bigdoors.core.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.core.commands.SetBlocksToMoveDelayed;
import nl.pim16aap2.bigdoors.core.commands.SetOpenDirectionDelayed;
import nl.pim16aap2.bigdoors.core.commands.SetOpenStatusDelayed;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.core.managers.LimitsManager;
import nl.pim16aap2.bigdoors.core.managers.StructureDeletionManager;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.core.structures.StructureBaseBuilder;
import nl.pim16aap2.bigdoors.core.structures.StructureOwner;
import nl.pim16aap2.bigdoors.core.structures.StructureRegistry;
import nl.pim16aap2.bigdoors.core.tooluser.Step;
import nl.pim16aap2.bigdoors.core.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.testimplementations.TestLocationFactory;
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

import static nl.pim16aap2.bigdoors.core.UnitTestUtil.getWorld;
import static nl.pim16aap2.bigdoors.core.UnitTestUtil.newStructureBaseBuilder;

public class CreatorTestsUtil
{
    protected final Vector3Di min = new Vector3Di(10, 15, 20);
    protected final Vector3Di max = new Vector3Di(20, 25, 30);
    protected final Vector3Di powerblock = new Vector3Di(40, 40, 40);
    protected final String structureName = "testDoor123";
    protected final IWorld world = getWorld();
    protected Vector3Di rotationPoint = new Vector3Di(20, 15, 25);
    protected MovementDirection openDirection = MovementDirection.COUNTERCLOCKWISE;

    protected StructureOwner structureOwner;

    protected StructureBaseBuilder structureBaseBuilder;

    protected ILocalizer localizer;

    protected PlayerData playerData;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    protected IPlayer player;

    @Mock
    protected DatabaseManager databaseManager;

    @Mock
    protected IEconomyManager economyManager;

    @Mock
    protected IConfig config;

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

    protected ILocationFactory locationFactory = new TestLocationFactory();

    protected ToolUser.Context context;

    protected DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    private AutoCloseable mocks;

    private void initPlayer()
    {
        final var uuid = UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db5");
        final var name = "user";
        var structureSizeLimit = 8;
        var structureCountLimit = 9;

        playerData = new PlayerData(uuid, name, structureSizeLimit, structureCountLimit, true, true);

        structureOwner = new StructureOwner(-1, PermissionLevel.CREATOR, playerData);

        Mockito.when(player.getUUID()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getStructureCountLimit()).thenReturn(structureCountLimit);
        Mockito.when(player.getStructureSizeLimit()).thenReturn(structureSizeLimit);
        Mockito.when(player.isOp()).thenReturn(true);
        Mockito.when(player.hasProtectionBypassPermission()).thenReturn(true);
        Mockito.when(player.getStructureSizeLimit()).thenReturn(structureSizeLimit);
        Mockito.when(player.getLocation()).thenReturn(Optional.empty());

        Mockito.when(player.getPlayerData()).thenReturn(playerData);
    }

    private void beforeEach0()
        throws Exception
    {
        mocks = MockitoAnnotations.openMocks(this);

        localizer = UnitTestUtil.initLocalizer();
        limitsManager = new LimitsManager(permissionsManager, config);

        final var builderResult = newStructureBaseBuilder();
        builderResult.assistedFactoryMocker()
                     .setMock(ILocalizer.class, localizer)
                     .setMock(StructureRegistry.class,
                              StructureRegistry.unCached(debuggableRegistry,
                                                         Mockito.mock(StructureDeletionManager.class)));
        structureBaseBuilder = builderResult.structureBaseBuilder();

        final var assistedStepFactory = Mockito.mock(Step.Factory.IFactory.class);
        //noinspection deprecation
        Mockito.when(assistedStepFactory.stepName(Mockito.anyString()))
               .thenAnswer(invocation -> new Step.Factory(localizer, invocation.getArgument(0, String.class)));

        context = new ToolUser.Context(
            structureBaseBuilder, localizer, ITextFactory.getSimpleTextFactory(), toolUserManager, databaseManager,
            limitsManager, economyManager, protectionCompatManager, bigDoorsToolUtil, commandFactory,
            assistedStepFactory);

        initCommands();

        initPlayer();

        final IPlayerFactory playerFactory = Mockito.mock(IPlayerFactory.class);
        Mockito.when(playerFactory.create(playerData.getUUID()))
               .thenReturn(CompletableFuture.completedFuture(Optional.of(player)));

        // Immediately return whatever structure was being added to the database as if it was successful.
        Mockito.when(databaseManager.addStructure(ArgumentMatchers.any())).thenAnswer(
            (Answer<CompletableFuture<Optional<AbstractStructure>>>) invocation ->
                CompletableFuture.completedFuture(Optional.of((AbstractStructure) invocation.getArguments()[0])));

        Mockito.when(
                   databaseManager.addStructure(ArgumentMatchers.any(AbstractStructure.class), Mockito.any(IPlayer.class)))
               .thenAnswer((Answer<CompletableFuture<DatabaseManager.StructureInsertResult>>) invocation ->
                   CompletableFuture.completedFuture(new DatabaseManager.StructureInsertResult(
                       Optional.of(invocation.getArgument(0, AbstractStructure.class)), false)));

        Mockito.when(permissionsManager.hasPermission(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);

        Mockito.when(config.maxStructureSize()).thenReturn(OptionalInt.empty());
        Mockito.when(config.maxStructureCount()).thenReturn(OptionalInt.empty());
        Mockito.when(config.maxPowerBlockDistance()).thenReturn(OptionalInt.empty());
        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.empty());
    }

    @BeforeEach
    protected void beforeEach()
    {
        try
        {
            beforeEach0();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void initCommands()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<DelayedCommandInputRequest, DelayedCommandInputRequest.IFactory> assistedFactory =
            new AssistedFactoryMocker<>(DelayedCommandInputRequest.class, DelayedCommandInputRequest.IFactory.class)
                .setMock(ILocalizer.class, localizer)
                .setMock(ITextFactory.class, ITextFactory.getSimpleTextFactory())
                .setMock(DelayedCommandInputManager.class, delayedCommandInputManager);

        final var commandContext = new DelayedCommand.Context(delayedCommandInputManager, localizer,
                                                              ITextFactory.getSimpleTextFactory(),
                                                              () -> commandFactory);

        final SetOpenStatusDelayed setOpenStatusDelayed =
            new SetOpenStatusDelayed(commandContext, assistedFactory.getFactory());
        Mockito.when(commandFactory.getSetOpenStatusDelayed()).thenReturn(setOpenStatusDelayed);

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

    protected void setBuyStructure(boolean status)
    {
        Mockito.when(economyManager.buyStructure(ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
               .thenReturn(status);
    }


    protected AbstractStructure.BaseHolder constructStructureBase()
    {
        return structureBaseBuilder.builder()
                                   .uid(-1).name(structureName).cuboid(new Cuboid(min, max))
                                   .rotationPoint(rotationPoint)
                                   .powerBlock(powerblock).world(world).isOpen(false).isLocked(false)
                                   .openDir(openDirection).primeOwner(structureOwner)
                                   .build();
    }

    public void applySteps(Creator creator, Object... input)
    {
        for (int idx = 0; idx < input.length; ++idx)
        {
            final Object obj = input[idx];
            final @Nullable String stepName = creator.getCurrentStep().map(Step::getName).orElse(null);
            Assertions.assertNotNull(stepName);

            Assertions.assertTrue(creator.handleInput(obj),
                                  String.format("IDX: %d, Input: %s, Step: %s", idx, obj, stepName));
        }
    }

    public void testCreation(Creator creator, AbstractStructure actualStructure, Object... input)
    {
        applySteps(creator, input);
        Mockito.verify(creator.getPlayer(), Mockito.never())
               .sendMessage(UnitTestUtil.toText("creator.base.error.creation_cancelled"));
        Mockito.verify(databaseManager).addStructure(actualStructure, player);
    }
}
