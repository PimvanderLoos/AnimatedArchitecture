package nl.pim16aap2.animatedarchitecture.core.structures;

import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StructureRedstoneHandlerTest
{
    private static final long UID = 42L;
    private static final Vector3Di POWER_BLOCK = new Vector3Di(10, 20, 30);

    @Mock
    private Structure structure;

    @Mock
    private IWorld world;

    @Mock
    private IConfig config;

    @Mock
    private IRedstoneManager redstoneManager;

    @Mock
    private IChunkLoader chunkLoader;

    @Mock
    private StructureActivityManager structureActivityManager;

    @Mock
    private StructureAnimationRequestBuilder toggleRequestBuilder;

    @Mock
    private IPlayerFactory playerFactory;

    @Mock
    private IExecutor executor;

    private StructureOwner primeOwner;
    private AtomicReference<StructureRedstoneHandler.RedstoneSnapshot> snapshotRef;
    private StructureRedstoneHandler handler;

    @BeforeEach
    void beforeEach()
    {
        // setup
        final var playerData = mock(PlayerData.class);
        when(playerData.getUUID()).thenReturn(UUID.randomUUID());
        primeOwner = new StructureOwner(UID, PermissionLevel.CREATOR, playerData);

        when(config.allowRedstone()).thenReturn(true);
        when(chunkLoader.checkChunk(any(), any(), any()))
            .thenReturn(IChunkLoader.ChunkLoadResult.PASS);

        // Default: executor.runAsyncLater runs the task immediately
        doAnswer(invocation ->
        {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).runAsyncLater(any(Runnable.class), anyLong());

        snapshotRef = new AtomicReference<>(createSnapshot(false, false, 0));

        handler = new StructureRedstoneHandler(
            structure, UID, world, primeOwner, config,
            snapshotRef::get,
            redstoneManager, chunkLoader, structureActivityManager,
            toggleRequestBuilder, playerFactory, executor
        );
    }

    @Test
    void onRedstoneChange_shouldOpenWhenPoweredAndClosed()
    {
        // setup
        snapshotRef.set(createSnapshot(false, false, 0)); // closed, not perpetual

        mockToggleBuilderChain();

        // execute
        handler.onRedstoneChange(true);

        // verify
        verify(toggleRequestBuilder).builder();
    }

    @Test
    void onRedstoneChange_shouldCloseWhenUnpoweredAndOpen()
    {
        // setup
        snapshotRef.set(createSnapshot(false, true, 0)); // open, not perpetual

        mockToggleBuilderChain();

        // execute
        handler.onRedstoneChange(false);

        // verify
        verify(toggleRequestBuilder).builder();
    }

    @Test
    void onRedstoneChange_shouldStopPerpetualWhenUnpowered()
    {
        // setup
        snapshotRef.set(createSnapshot(true, false, 0));

        // execute
        handler.onRedstoneChange(false);

        // verify
        verify(structureActivityManager).stopAnimatorsWithWriteAccess(UID);
        verify(toggleRequestBuilder, never()).builder();
    }

    @Test
    void onRedstoneChange_shouldTogglePerpetualWhenPowered()
    {
        // setup
        snapshotRef.set(createSnapshot(true, false, 0));
        mockToggleBuilderChain();

        // execute
        handler.onRedstoneChange(true);

        // verify
        verify(toggleRequestBuilder).builder();
    }

    @Test
    void onRedstoneChange_shouldDoNothingWhenRedstoneDisabledInConfig()
    {
        // setup
        when(config.allowRedstone()).thenReturn(false);

        // execute
        handler.onRedstoneChange(true);

        // verify
        verify(toggleRequestBuilder, never()).builder();
        verify(redstoneManager, never()).isBlockPowered(any(), any());
    }

    @Test
    void onRedstoneChange_shouldDoNothingWhenUidIsInvalid()
    {
        // setup
        handler = new StructureRedstoneHandler(
            structure, 0L, world, primeOwner, config,
            snapshotRef::get,
            redstoneManager, chunkLoader, structureActivityManager,
            toggleRequestBuilder, playerFactory, executor
        );

        // execute
        handler.onRedstoneChange(true);

        // verify
        verify(toggleRequestBuilder, never()).builder();
    }

    @Test
    void verifyRedstoneState_shouldNotActWhenChunkNotLoaded()
    {
        // setup
        when(chunkLoader.checkChunk(any(), any(), any()))
            .thenReturn(IChunkLoader.ChunkLoadResult.FAIL);

        // execute
        handler.verifyRedstoneState();

        // verify
        verify(redstoneManager, never()).isBlockPowered(any(), any());
        verify(toggleRequestBuilder, never()).builder();
    }

    @Test
    void verifyRedstoneState_shouldQueryRedstoneAndApply()
    {
        // setup
        snapshotRef.set(createSnapshot(true, false, 0));
        when(redstoneManager.isBlockPowered(world, POWER_BLOCK))
            .thenReturn(IRedstoneManager.RedstoneStatus.POWERED);
        mockToggleBuilderChain();

        // execute
        handler.verifyRedstoneState();

        // verify
        verify(redstoneManager).isBlockPowered(world, POWER_BLOCK);
        verify(toggleRequestBuilder).builder();
    }

    @Test
    void verifyRedstoneState_shouldDoNothingWhenRedstoneDisabled()
    {
        // setup
        when(redstoneManager.isBlockPowered(world, POWER_BLOCK))
            .thenReturn(IRedstoneManager.RedstoneStatus.DISABLED);

        // execute
        handler.verifyRedstoneState();

        // verify
        verify(toggleRequestBuilder, never()).builder();
    }

    @Test
    void onStateChanged_shouldIncrementVersionAndScheduleVerification()
    {
        // setup
        final long initialVersion = handler.currentVersion();
        // Prevent the scheduled verification from actually running so we can inspect state
        doNothing().when(executor).runAsyncLater(any(Runnable.class), anyLong());

        // execute
        handler.onStateChanged();

        // verify
        assertThat(handler.currentVersion()).isEqualTo(initialVersion + 1);
        verify(executor).runAsyncLater(any(Runnable.class), eq(1L));
    }

    @Test
    void applyRedstoneAction_shouldDiscardStaleSnapshotAndScheduleVerification()
    {
        // setup
        // Stop the executor from running tasks immediately so we can control version/snapshot state
        doNothing().when(executor).runAsyncLater(any(Runnable.class), anyLong());

        // Bump version to 1; snapshot returned by the supplier still reports version 0 (stale)
        handler.onStateChanged(); // version -> 1

        // execute - onRedstoneChange builds a snapshot at version 0, which is stale vs version 1
        handler.onRedstoneChange(true);

        // verify - stale snapshot discarded; no toggle dispatched, but a coalesced retry was scheduled
        verify(toggleRequestBuilder, never()).builder();
        verify(executor).runAsyncLater(any(Runnable.class), anyLong());
    }

    @Test
    void onChunkLoad_shouldNotActWhenRotationPointChunkNotLoaded()
    {
        // setup
        final var rotationPoint = new Vector3Di(50, 60, 70);
        snapshotRef.set(new StructureRedstoneHandler.RedstoneSnapshot(
            POWER_BLOCK, unsetOpenStatus(), rotationPoint, false, 0));

        when(chunkLoader.checkChunk(eq(world), eq(POWER_BLOCK), any()))
            .thenReturn(IChunkLoader.ChunkLoadResult.PASS);
        when(chunkLoader.checkChunk(eq(world), eq(rotationPoint), any()))
            .thenReturn(IChunkLoader.ChunkLoadResult.FAIL);

        // execute
        handler.onChunkLoad();

        // verify
        verify(redstoneManager, never()).isBlockPowered(any(), any());
    }

    @Test
    void onChunkLoad_shouldProceedWhenAllChunksLoaded()
    {
        // setup
        snapshotRef.set(createSnapshot(true, false, 0));
        when(redstoneManager.isBlockPowered(world, POWER_BLOCK))
            .thenReturn(IRedstoneManager.RedstoneStatus.POWERED);
        mockToggleBuilderChain();

        // execute
        handler.onChunkLoad();

        // verify
        verify(redstoneManager).isBlockPowered(world, POWER_BLOCK);
        verify(toggleRequestBuilder).builder();
    }

    @Test
    void onRedstoneChange_shouldAbortWhenPoweredAndAlreadyOpen()
    {
        // setup
        snapshotRef.set(createSnapshot(false, true, 0)); // already open

        // execute
        handler.onRedstoneChange(true); // powered, but already open

        // verify
        verify(toggleRequestBuilder, never()).builder();
    }

    @Test
    void onRedstoneChange_shouldAbortWhenUnpoweredAndAlreadyClosed()
    {
        // setup
        snapshotRef.set(createSnapshot(false, false, 0)); // already closed

        // execute
        handler.onRedstoneChange(false); // unpowered, but already closed

        // verify
        verify(toggleRequestBuilder, never()).builder();
    }

    /**
     * Creates a {@link StructureRedstoneHandler.RedstoneSnapshot} using real {@link IPropertyValue} instances
     * sourced from a {@link PropertyContainer}.
     * <p>
     * When {@code canMovePerpetually} is true, the open-status property is left unset (no {@link Property#OPEN_STATUS}).
     * When false, the property is set to {@code isOpen}.
     */
    private StructureRedstoneHandler.RedstoneSnapshot createSnapshot(
        boolean canMovePerpetually,
        boolean isOpen,
        long version)
    {
        final IPropertyValue<Boolean> openStatus;
        if (canMovePerpetually)
        {
            openStatus = unsetOpenStatus();
        }
        else
        {
            openStatus = PropertyContainer
                .of(Property.OPEN_STATUS, isOpen, false)
                .getPropertyValue(Property.OPEN_STATUS);
        }
        return new StructureRedstoneHandler.RedstoneSnapshot(
            POWER_BLOCK, openStatus, null, canMovePerpetually, version);
    }

    private IPropertyValue<Boolean> unsetOpenStatus()
    {
        // A container without OPEN_STATUS returns the unset sentinel
        return PropertyContainer
            .forProperties(java.util.List.of(), false)
            .getPropertyValue(Property.OPEN_STATUS);
    }

    @SuppressWarnings("unchecked")
    private void mockToggleBuilderChain()
    {
        final var builderStep1 = mock(StructureAnimationRequestBuilder.IBuilderStructure.class);
        final var builderStep2 = mock(StructureAnimationRequestBuilder.IBuilderStructureActionCause.class);
        final var builderStep3 = mock(StructureAnimationRequestBuilder.IBuilderStructureActionType.class);
        final var builderStep4 = mock(StructureAnimationRequestBuilder.IBuilder.class);
        final var request = mock(StructureAnimationRequest.class);
        final var player = mock(IPlayer.class);

        when(toggleRequestBuilder.builder()).thenReturn(builderStep1);
        when(builderStep1.structure(any(Structure.class))).thenReturn(builderStep2);
        when(builderStep2.structureActionCause(any())).thenReturn(builderStep3);
        when(builderStep3.structureActionType(any())).thenReturn(builderStep4);
        when(builderStep4.messageReceiverServer()).thenReturn(builderStep4);
        when(builderStep4.responsible(any(IPlayer.class))).thenReturn(builderStep4);
        when(builderStep4.build()).thenReturn(request);
        when(request.execute()).thenReturn(CompletableFuture.completedFuture(null));

        when(playerFactory.create(any(PlayerData.class))).thenReturn(player);
    }
}
