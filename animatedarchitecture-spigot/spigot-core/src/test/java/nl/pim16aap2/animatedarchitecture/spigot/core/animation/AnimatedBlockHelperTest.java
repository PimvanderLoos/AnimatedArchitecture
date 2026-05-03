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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(5)
@ExtendWith(MockitoExtension.class)
class AnimatedBlockHelperTest
{
    @Mock
    private JavaPlugin plugin;

    @Mock
    private AnimationRunManager animationRunManager;

    @Mock
    private Entity entity;

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

        // lenient: not needed for setRecoveryData tests which use blockDisplay, not entity
        lenient().when(entity.getPersistentDataContainer()).thenReturn(entityContainer);
    }

    @Test
    void recoverAnimatedBlocks_shouldDoNothingForEmptyList()
    {
        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of());

        // verify
        verify(animationRunManager, never()).recordRecoveredBlocks(any(), any(int.class), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldSkipEntityWithNoRecoveryData()
    {
        // setup
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(null);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of(entity));

        // verify
        verify(animationRunManager, never()).recordRecoveredBlocks(any(), any(int.class), any());
        verify(entity, never()).remove();
    }

    @Test
    void recoverAnimatedBlocks_shouldRemoveEntityAndSkipWhenRecoveryDataIsEmpty()
    {
        // setup
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(IAnimatedBlockRecoveryData.EMPTY);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of(entity));

        // verify
        verify(entity).remove();
        verify(animationRunManager, never()).recordRecoveredBlocks(any(), any(int.class), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldRecoverAndRecordBlocksWithRunUuid()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final AnimatedBlockRecoveryData recoveryData = mockRecoveryData(runUuid, true);
        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of(entity));

        // verify
        verify(entity).remove();
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid), eq(1), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldBatchRecoveredBlocksByRunUuid()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final AnimatedBlockRecoveryData recoveryData = mockRecoveryData(runUuid, true);
        final Entity entity2 = mock(Entity.class);
        final PersistentDataContainer pdc2 = mock(PersistentDataContainer.class);

        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData);
        when(entity2.getPersistentDataContainer()).thenReturn(pdc2);
        when(pdc2.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of(entity, entity2));

        // verify
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid), eq(2), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldGroupRecoveryByRunUuid()
    {
        // setup
        final UUID runUuid1 = UUID.randomUUID();
        final UUID runUuid2 = UUID.randomUUID();
        final AnimatedBlockRecoveryData recoveryData1 = mockRecoveryData(runUuid1, true);
        final AnimatedBlockRecoveryData recoveryData2 = mockRecoveryData(runUuid2, true);
        final Entity entity2 = mock(Entity.class);
        final PersistentDataContainer pdc2 = mock(PersistentDataContainer.class);

        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData1);
        when(entity2.getPersistentDataContainer()).thenReturn(pdc2);
        when(pdc2.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(recoveryData2);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of(entity, entity2));

        // verify
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid1), eq(1), any());
        verify(animationRunManager).recordRecoveredBlocks(eq(runUuid2), eq(1), any());
    }

    @Test
    void recoverAnimatedBlocks_shouldContinueAfterRecoveryException()
    {
        // setup
        final UUID runUuid = UUID.randomUUID();
        final AnimatedBlockRecoveryData failingData = mockRecoveryDataThrowing();
        final AnimatedBlockRecoveryData succeedingData = mockRecoveryData(runUuid, true);
        final Entity entity2 = mock(Entity.class);
        final PersistentDataContainer pdc2 = mock(PersistentDataContainer.class);

        when(entityContainer.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(failingData);
        when(entity2.getPersistentDataContainer()).thenReturn(pdc2);
        when(pdc2.get(any(NamespacedKey.class), eq(AnimatedBlockRecoveryDataType.INSTANCE)))
            .thenReturn(succeedingData);

        // execute
        animatedBlockHelper.recoverAnimatedBlocks(List.of(entity, entity2));

        // verify: the second entity is still recovered despite the first failing
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
        final AnimatedBlockRecoveryData data = mock(AnimatedBlockRecoveryData.class);
        when(data.recover()).thenReturn(recoveryResult);
        when(data.animationRunUuid()).thenReturn(runUuid);
        return data;
    }

    private static AnimatedBlockRecoveryData mockRecoveryDataThrowing()
    {
        final AnimatedBlockRecoveryData data = mock(AnimatedBlockRecoveryData.class);
        when(data.recover()).thenThrow(new RuntimeException("Recovery failed"));
        return data;
    }
}
