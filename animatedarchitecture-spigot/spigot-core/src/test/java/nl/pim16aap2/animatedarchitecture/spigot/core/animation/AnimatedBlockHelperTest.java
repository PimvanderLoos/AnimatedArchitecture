package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import nl.pim16aap2.animatedarchitecture.core.animation.recovery.AnimationRunManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.AnimatedBlockRecoveryDataType;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.IAnimatedBlockRecoveryData;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.IAnimatedBlockRecoveryData.AnimatedBlockRecoveryData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(5)
@ExtendWith(MockitoExtension.class)
class AnimatedBlockHelperTest
{
    @Mock
    private JavaPlugin plugin;

    @Mock
    private AnimationRunManager animationRunManager;

    @Mock
    private Entity entity0;
    @Mock
    private Entity entity1;

    @Mock
    private PersistentDataContainer entityContainer;

    @Mock
    private BlockDisplay blockDisplay;

    @Mock
    private PersistentDataContainer blockDisplayContainer;

    private AnimatedBlockHelper animatedBlockHelper;

    @BeforeEach
    void setUp()
    {
        try (MockedConstruction<NamespacedKey> ignored = mockConstruction(NamespacedKey.class))
        {
            animatedBlockHelper = new AnimatedBlockHelper(plugin, animationRunManager);
        }
    }

    @Test
    void recoverAnimatedBlocks_shouldDoNothingForNoEntities()
    {
        // execute
        animatedBlockHelper.recoverAnimatedBlocks();

        // verify
        verify(animationRunManager, never()).recordRecoveredBlocks(any(), anyInt(), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldSkipEntityWithNoRecoveryData()
    {
        // setup
        //noinspection DataFlowIssue
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(null);
        when(entity0.getPersistentDataContainer()).thenReturn(entityContainer);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(entity0);

        // verify
        verify(animationRunManager, never()).recordRecoveredBlocks(any(), anyInt(), any());
        verify(entity0, never()).remove();
    }

    @Test
    void recoverAnimatedBlocks_shouldRemoveEntityAndSkipWhenRecoveryDataIsEmpty()
    {
        // setup
        //noinspection DataFlowIssue
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(IAnimatedBlockRecoveryData.EMPTY);
        when(entity0.getPersistentDataContainer()).thenReturn(entityContainer);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(entity0);

        // verify
        verify(entity0).remove();
        verify(animationRunManager, never()).recordRecoveredBlocks(any(), anyInt(), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldRecoverAndRecordBlocksWithRunUuid()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final AnimatedBlockRecoveryData recoveryData = mockRecoveryData(runUuid, true);

        //noinspection DataFlowIssue
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData);
        when(entity0.getPersistentDataContainer()).thenReturn(entityContainer);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(entity0);

        // verify
        verify(entity0).remove();
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid), eq(1), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldBatchRecoveredBlocksByRunUuid()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();

        final AnimatedBlockRecoveryData recoveryData = mockRecoveryData(runUuid, true);

        final Entity entity1 = mock();
        final PersistentDataContainer pdc1 = mock();

        //noinspection DataFlowIssue
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData);
        when(entity0.getPersistentDataContainer()).thenReturn(entityContainer);

        //noinspection DataFlowIssue
        when(pdc1.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData);
        when(entity1.getPersistentDataContainer()).thenReturn(pdc1);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(entity0, entity1);

        // verify
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid), eq(2), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldGroupRecoveryByRunUuid()
    {
        // setup
        final UUID runUuid0 = UUID.randomUUID();
        final UUID runUuid1 = UUID.randomUUID();

        final PersistentDataContainer pdc1 = mock();

        final AnimatedBlockRecoveryData recoveryData1 = mockRecoveryData(runUuid0, true);
        final AnimatedBlockRecoveryData recoveryData2 = mockRecoveryData(runUuid1, true);

        //noinspection DataFlowIssue
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData1);
        when(entity0.getPersistentDataContainer()).thenReturn(entityContainer);

        //noinspection DataFlowIssue
        when(pdc1.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData2);
        when(entity1.getPersistentDataContainer()).thenReturn(pdc1);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(entity0, entity1);

        // verify
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid0), eq(1), any());
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid1), eq(1), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldContinueAfterRecoveryException()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();

        final PersistentDataContainer pdc1 = mock();

        final AnimatedBlockRecoveryData failingData = mockRecoveryDataThrowing();
        final AnimatedBlockRecoveryData succeedingData = mockRecoveryData(runUuid, true);

        //noinspection DataFlowIssue
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(failingData);
        when(entity0.getPersistentDataContainer()).thenReturn(entityContainer);

        //noinspection DataFlowIssue
        when(pdc1.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(succeedingData);
        when(entity1.getPersistentDataContainer()).thenReturn(pdc1);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(entity0, entity1);

        // verify
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid), eq(1), any());
    }

    @Test
    void setRecoveryData_shouldSetDataOnEntity()
    {
        // setup
        final IAnimatedBlockRecoveryData recoveryData = IAnimatedBlockRecoveryData.EMPTY;

        when(blockDisplay.getPersistentDataContainer()).thenReturn(blockDisplayContainer);

        // execute
        animatedBlockHelper.setRecoveryData(blockDisplay, recoveryData);

        // verify
        verify(blockDisplayContainer).set(
            any(NamespacedKey.class),
            eq(AnimatedBlockRecoveryDataType.INSTANCE),
            eq(recoveryData)
        );
    }

    @Test
    void setRecoveryData_shouldUseEmptyDataWhenNull()
    {
        // setup
        when(blockDisplay.getPersistentDataContainer()).thenReturn(blockDisplayContainer);

        // execute
        animatedBlockHelper.setRecoveryData(blockDisplay, null);

        // verify
        verify(blockDisplayContainer).set(
            any(NamespacedKey.class),
            eq(AnimatedBlockRecoveryDataType.INSTANCE),
            eq(IAnimatedBlockRecoveryData.EMPTY)
        );
    }

    private static AnimatedBlockRecoveryData mockRecoveryData(UUID runUuid, boolean recoveryResult)
    {
        final AnimatedBlockRecoveryData data = mock();
        when(data.recover()).thenReturn(recoveryResult);
        when(data.animationRunUuid()).thenReturn(runUuid);
        return data;
    }

    private static AnimatedBlockRecoveryData mockRecoveryDataThrowing()
    {
        final AnimatedBlockRecoveryData data = mock();
        when(data.recover()).thenThrow(new RuntimeException("Recovery failed"));
        return data;
    }
}
