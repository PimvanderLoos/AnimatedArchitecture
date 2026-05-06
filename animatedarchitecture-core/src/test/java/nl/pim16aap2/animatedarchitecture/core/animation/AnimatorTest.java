package nl.pim16aap2.animatedarchitecture.core.animation;

import nl.pim16aap2.animatedarchitecture.core.animation.recovery.AnimationRunManager;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimationHookManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimatorTest
{
    @Mock
    private Structure structure;

    @Mock
    private StructureSnapshot structureSnapshot;

    @Mock
    private StructureActivityManager structureActivityManager;

    @Mock
    private IExecutor executor;

    @Mock
    private AnimationHookManager animationHookManager;

    @Mock
    private AnimationRunManager animationRunManager;

    @Mock
    private IAnimatedBlockContainer animatedBlockContainer;

    private UUID animationRunUuid;

    private Animator animator;

    @BeforeEach
    void setup()
    {
        animationRunUuid = UUID.randomUUID();
        animator = newAnimator(animationRunUuid);
    }

    @AfterEach
    void tearDown()
    {
        verifyNoMoreInteractions(
                structureActivityManager,
                executor,
                animationHookManager,
                animationRunManager,
                animatedBlockContainer
        );
    }

    @Test
    void finishAnimation_shouldAlwaysProcessFinishedAnimationWhenBlockingRestoreFails()
    {
        // setup
        when(executor.runOnMainThreadWithResponse(any(Runnable.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("restore failed")));

        // execute
        animator.finishAnimation(true);

        // verify
        verify(animationRunManager).registerRunFailure(eq(animationRunUuid), contains("restore failed"));
        verify(structureActivityManager).processFinishedAnimation(animator);
    }

    @Test
    void finishAnimation_shouldAlwaysProcessFinishedAnimationWhenAsyncRestoreFails()
    {
        // setup
        when(executor.runOnMainThreadWithResponse(any(Runnable.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("restore failed")));

        // execute
        animator.finishAnimation(false);

        // verify
        verify(animationRunManager).registerRunFailure(eq(animationRunUuid), contains("restore failed"));
        verify(structureActivityManager).processFinishedAnimation(animator);
    }

    private Animator newAnimator(UUID animationRunUuid)
    {
        final var cuboid = new Cuboid(new Vector3Di(0, 0, 0), new Vector3Di(1, 1, 1));
        when(structureSnapshot.getCuboid()).thenReturn(cuboid);
        when(structure.canMovePerpetually()).thenReturn(false);

        final var data = new AnimationRequestData(
            structureActivityManager,
            mock(IAudioPlayer.class),
            executor,
            animationHookManager,
            animationRunManager,
            50,
            mock(HighlightedBlockSpawner.class),
            mock(IConfig.class),
            structureSnapshot,
            StructureActionCause.PLAYER,
            1.0D,
            false,
            false,
            cuboid,
            mock(IPlayer.class),
            AnimationType.MOVE_BLOCKS,
            StructureActionType.TOGGLE
        );

        return new Animator(
            structure,
            data,
            mock(IAnimationComponent.class),
            animatedBlockContainer,
            animationRunUuid
        );
    }
}
