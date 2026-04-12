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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
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

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(
            config,
            redstoneManager,
            chunkLoader,
            structureActivityManager,
            toggleRequestBuilder,
            playerFactory,
            executor
        );
    }

    @BeforeEach
    void beforeEach()
    {
        final var playerData = mock(PlayerData.class);
        primeOwner = new StructureOwner(UID, PermissionLevel.CREATOR, playerData);

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
        mockRedstoneEnabled();
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
        mockRedstoneEnabled();
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
        mockRedstoneEnabled();
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
        mockRedstoneEnabled();
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
        mockRedstoneEnabled();
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
        mockRedstoneEnabled();
        mockPowerBlockChunkLoaded();
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
        mockRedstoneEnabled();
        mockPowerBlockChunkLoaded();
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
        mockRedstoneEnabled();
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
    void onChunkLoad_shouldNotActWhenStructureIsLocked()
    {
        // setup
        mockRedstoneEnabled();
        snapshotRef.set(createSnapshot(false, false, 0, true));

        // execute
        handler.onChunkLoad();

        // verify
        verify(chunkLoader, never()).checkChunk(any(), any(), any());
        verify(redstoneManager, never()).isBlockPowered(any(), any());
    }

    @Test
    void verifyRedstoneState_shouldNotActWhenStructureIsLocked()
    {
        // setup
        mockRedstoneEnabled();
        snapshotRef.set(createSnapshot(false, false, 0, true));

        // execute
        handler.verifyRedstoneState();

        // verify
        verify(chunkLoader, never()).checkChunk(any(), any(), any());
        verify(redstoneManager, never()).isBlockPowered(any(), any());
        verify(toggleRequestBuilder, never()).builder();
    }

    @Test
    void onRedstoneChange_shouldNotActWhenStructureIsLocked()
    {
        // setup
        mockRedstoneEnabled();
        snapshotRef.set(createSnapshot(false, false, 0, true));

        // execute
        handler.onRedstoneChange(true);

        // verify
        verify(toggleRequestBuilder, never()).builder();
        verify(structureActivityManager, never()).stopAnimatorsWithWriteAccess(anyLong());
    }

    @Test
    void onChunkLoad_shouldNotActWhenRotationPointChunkNotLoaded()
    {
        // setup
        mockRedstoneEnabled();
        final var rotationPoint = new Vector3Di(50, 60, 70);
        snapshotRef.set(new StructureRedstoneHandler.RedstoneSnapshot(
            POWER_BLOCK,
            unsetOpenStatus(),
            rotationPoint,
            false,
            false,
            0
        ));

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
        mockRedstoneEnabled();
        mockAllChunksLoaded();
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
        mockRedstoneEnabled();
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
        mockRedstoneEnabled();
        snapshotRef.set(createSnapshot(false, false, 0)); // already closed

        // execute
        handler.onRedstoneChange(false); // unpowered, but already closed

        // verify
        verify(toggleRequestBuilder, never()).builder();
    }

    /**
     * Creates a {@link StructureRedstoneHandler.RedstoneSnapshot} using real {@link IPropertyValue} instances sourced
     * from a {@link PropertyContainer}.
     * <p>
     * When {@code canMovePerpetually} is true, the open-status property is left unset (no
     * {@link Property#OPEN_STATUS}). When false, the property is set to {@code isOpen}.
     */
    private StructureRedstoneHandler.RedstoneSnapshot createSnapshot(
        boolean canMovePerpetually,
        boolean isOpen,
        long version)
    {
        return createSnapshot(canMovePerpetually, isOpen, version, false);
    }

    private StructureRedstoneHandler.RedstoneSnapshot createSnapshot(
        boolean canMovePerpetually,
        boolean isOpen,
        long version,
        boolean isLocked)
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
            POWER_BLOCK,
            openStatus,
            null,
            canMovePerpetually,
            isLocked,
            version
        );
    }

    private IPropertyValue<Boolean> unsetOpenStatus()
    {
        // A container without OPEN_STATUS returns the unset sentinel
        return PropertyContainer
            .forProperties(java.util.List.of(), false)
            .getPropertyValue(Property.OPEN_STATUS);
    }

    private void mockToggleBuilderChain()
    {
        final StructureAnimationRequestBuilder.IBuilderStructure builderStep1 = mock();
        final StructureAnimationRequestBuilder.IBuilderStructureActionCause builderStep2 = mock();
        final StructureAnimationRequestBuilder.IBuilderStructureActionType builderStep3 = mock();
        final StructureAnimationRequestBuilder.IBuilder builderStep4 = mock();
        final StructureAnimationRequest request = mock();
        final IPlayer player = mock();

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

    private void mockRedstoneEnabled()
    {
        when(config.allowRedstone()).thenReturn(true);
    }

    private void mockPowerBlockChunkLoaded()
    {
        when(chunkLoader.checkChunk(eq(world), eq(POWER_BLOCK), any()))
            .thenReturn(IChunkLoader.ChunkLoadResult.PASS);
    }

    private void mockAllChunksLoaded()
    {
        when(chunkLoader.checkChunk(any(), any(), any()))
            .thenReturn(IChunkLoader.ChunkLoadResult.PASS);
    }
}
