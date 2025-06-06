package nl.pim16aap2.animatedarchitecture.creator;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.LimitContainer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.DelayedCommand;
import nl.pim16aap2.animatedarchitecture.core.commands.DelayedCommandInputRequest;
import nl.pim16aap2.animatedarchitecture.core.commands.SetBlocksToMoveDelayed;
import nl.pim16aap2.animatedarchitecture.core.commands.SetOpenDirectionDelayed;
import nl.pim16aap2.animatedarchitecture.core.commands.SetOpenStatusDelayed;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.testimplementations.TestLocationFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
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
import java.util.concurrent.Executors;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.getWorld;
import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.newStructureBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
public class CreatorTestsUtil
{
    protected final Vector3Di min = new Vector3Di(10, 15, 20);
    protected final Vector3Di max = new Vector3Di(20, 25, 30);
    protected final Cuboid cuboid = new Cuboid(min, max);
    protected final Vector3Di powerblock = max.add(1, 2, 3);
    protected final String structureName = "testDoor123";
    protected final IWorld world = getWorld();
    protected MovementDirection openDirection = MovementDirection.COUNTERCLOCKWISE;

    protected StructureBuilder structureBuilder;

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
    protected IProtectionHookManager protectionHookManager;

    @Mock
    protected IAnimatedArchitectureToolUtil animatedArchitectureToolUtil;

    @Mock
    protected CommandFactory commandFactory;

    @Mock
    protected IExecutor executor;

    protected ILocationFactory locationFactory = new TestLocationFactory();

    protected ToolUser.Context context;

    protected DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    private AutoCloseable mocks;

    private void initPlayer()
    {
        final var uuid = UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db5");
        final var name = "user";

        final var limits = new LimitContainer(
            OptionalInt.of(8),
            OptionalInt.of(9),
            OptionalInt.of(10),
            OptionalInt.of(11)
        );

        UnitTestUtil.initMessageable(player);
        playerData = new PlayerData(uuid, name, limits, true, true);

        when(player.getUUID()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);

        when(player
            .getLimit(ArgumentMatchers.any(Limit.class)))
            .thenAnswer(invocation -> limits.getLimit(invocation.getArgument(0, Limit.class)));

        when(player.isOp()).thenReturn(true);
        when(player.hasProtectionBypassPermission()).thenReturn(true);
        when(player.getLocation()).thenReturn(Optional.empty());

        when(player.getPlayerData()).thenReturn(playerData);
    }

    private void beforeEach0()
        throws Exception
    {
        mocks = MockitoAnnotations.openMocks(this);

        localizer = UnitTestUtil.initLocalizer();
        limitsManager = new LimitsManager(permissionsManager, config);
        structureBuilder = newStructureBuilder().structureBuilder();

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final var assistedStepFactory = Mockito.mock(Step.Factory.IFactory.class);
        when(assistedStepFactory
            .stepName(any(), anyString()))
            .thenAnswer(invocation -> new Step.Factory(invocation.getArgument(0), invocation.getArgument(1)));

        when(protectionHookManager
            .canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
        when(protectionHookManager
            .canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        context = new ToolUser.Context(
            structureBuilder,
            toolUserManager,
            databaseManager,
            limitsManager,
            economyManager,
            protectionHookManager,
            animatedArchitectureToolUtil,
            null,
            Mockito.mock(StructureActivityManager.class),
            commandFactory,
            assistedStepFactory
        );

        initCommands();

        initPlayer();

        // Immediately return whatever structure was being added to the database as if it was successful.
        when(databaseManager
            .addStructure(ArgumentMatchers.any()))
            .thenAnswer((Answer<CompletableFuture<Optional<Structure>>>) invocation ->
                CompletableFuture.completedFuture(Optional.of((Structure) invocation.getArguments()[0]))
            );

        when(databaseManager
            .addStructure(
                ArgumentMatchers.any(Structure.class),
                Mockito.any(IPlayer.class)))
            .thenAnswer((Answer<CompletableFuture<DatabaseManager.StructureInsertResult>>) invocation ->
                CompletableFuture.completedFuture(new DatabaseManager.StructureInsertResult(
                    Optional.of(invocation.getArgument(0, Structure.class)),
                    false))
            );

        when(permissionsManager.hasPermission(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);

        when(player.getLimit(Mockito.eq(Limit.STRUCTURE_SIZE))).thenReturn(OptionalInt.of(cuboid.getVolume()));

        when(config.maxStructureSize()).thenReturn(OptionalInt.empty());
        when(config.maxStructureCount()).thenReturn(OptionalInt.empty());
        when(config.maxPowerBlockDistance()).thenReturn(OptionalInt.empty());
        when(config.maxBlocksToMove()).thenReturn(OptionalInt.empty());
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
                .injectParameters(executor, delayedCommandInputManager);

        final var commandContext = new DelayedCommand.Context(
            delayedCommandInputManager,
            () -> commandFactory
        );

        final var setOpenStatusDelayed = new SetOpenStatusDelayed(commandContext, assistedFactory.getFactory());
        when(commandFactory.getSetOpenStatusDelayed()).thenReturn(setOpenStatusDelayed);

        final var setOpenDirectionDelayed = new SetOpenDirectionDelayed(commandContext, assistedFactory.getFactory());
        when(commandFactory.getSetOpenDirectionDelayed()).thenReturn(setOpenDirectionDelayed);

        final var setBlocksToMoveDelayed = new SetBlocksToMoveDelayed(commandContext, assistedFactory.getFactory());
        when(commandFactory.getSetBlocksToMoveDelayed()).thenReturn(setBlocksToMoveDelayed);
    }

    @AfterEach
    void cleanup()
        throws Exception
    {
        mocks.close();
    }

    protected void setEconomyEnabled(boolean status)
    {
        when(economyManager.isEconomyEnabled()).thenReturn(status);
    }

    protected void setEconomyPrice(double price)
    {
        when(economyManager.getPrice(any(), anyInt())).thenReturn(OptionalDouble.of(price));
    }

    protected void setBuyStructure(boolean status)
    {
        when(economyManager.buyStructure(any(), any(), any(), anyInt())).thenReturn(status);
    }

    protected Structure constructStructure(StructureType type, long uid, Object... properties)
    {
        return structureBuilder
            .builder(type)
            .uid(UnitTestUtil.newStructureID(uid))
            .name(structureName)
            .cuboid(cuboid)
            .powerBlock(powerblock)
            .world(world)
            .isLocked(false)
            .openDir(openDirection)
            .primeOwner(new StructureOwner(uid, PermissionLevel.CREATOR, playerData))
            .ownersOfStructure(null)
            .propertiesOfStructure(PropertyContainer.ofAll(properties))
            .build();
    }

    public void applySteps(Creator creator, Object... input)
    {
        for (int idx = 0; idx < input.length; ++idx)
        {
            final Object obj = input[idx];
            final @Nullable String stepName = creator.getCurrentStep().map(Step::getName).orElse(null);
            assertNotNull(stepName);

            assertTrue(
                creator.handleInput(obj).join(),
                String.format("IDX: %d, Input: %s, Step: %s", idx, obj, stepName)
            );
        }
    }

    public void testCreation(Creator creator, Structure actualStructure, Object... input)
    {
        applySteps(creator, input);
        verify(creator.getPlayer(), never())
            .sendSuccess(eq("creator.base.error.creation_cancelled"), any(Text.ArgumentCreator[].class));
        verify(databaseManager).addStructure(actualStructure, player);
    }
}
