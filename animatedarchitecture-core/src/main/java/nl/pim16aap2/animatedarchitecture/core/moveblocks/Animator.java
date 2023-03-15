package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHook;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimationHookManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nl.pim16aap2.animatedarchitecture.core.moveblocks.Animation.AnimationState;

/**
 * Represents a class that animates blocks.
 *
 * @author Pim
 */
@ToString
@Flogger
public final class Animator implements IAnimator
{
    /**
     * The amount of time (in milliseconds) to wait between the end of the animation and the placement of the blocks.
     * <p>
     * Allows for a more smooth transition between states.
     */
    private static final int FINISH_DURATION = 0;

    /**
     * The delay (measured in milliseconds) between initialization of the animation and starting to move the blocks.
     */
    private static final int START_DELAY = 700;

    /**
     * The delay (in milliseconds) before verifying the redstone state after the animation has ended.
     */
    private static final long VERIFY_REDSTONE_DELAY = 1_000L;

    /**
     * The structure whose blocks are going to be moved.
     */
    @Getter
    private final AbstractStructure structure;

    /**
     * A snapshot of the structure created before the toggle.
     */
    @Getter
    private final StructureSnapshot snapshot;

    /**
     * The player responsible for the movement.
     * <p>
     * This player may be offline.
     */
    @Getter
    private final IPlayer player;

    /**
     * The animation component used to do all the animation stuff.
     */
    private final IAnimationComponent animationComponent;

    private final IAnimationBlockManager animationBlockManager;

    /**
     * What caused the structure to be moved.
     */
    @Getter
    private final StructureActionCause cause;

    /**
     * The type of action that is fulfilled by moving the structure.
     */
    @Getter
    private final StructureActionType actionType;

    @ToString.Exclude
    private final StructureActivityManager structureActivityManager;

    @ToString.Exclude
    private final IExecutor executor;

    @ToString.Exclude
    private final AnimationHookManager animationHookManager;

    @ToString.Exclude
    private final int serverTickTime;

    /**
     * The amount of time (in seconds) that the animation will take.
     */
    @Getter
    private final double time;

    /**
     * When true, the blocks are moved without animating them. No animated blocks are spawned.
     */
    @Getter
    private final boolean skipAnimation;

    /**
     * True for types of movement that are supposed to keep going until otherwise stopped. For example, flags,
     * windmills, etc.
     * <p>
     * False to have the movement be time-bound, such as for doors, drawbridges, etc.
     */
    private final boolean perpetualMovement;

    /**
     * Keeps track of whether the animation has finished.
     */
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    /**
     * Keeps track of whether the animation has started.
     */
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);

    @Getter
    private final AnimationType animationType;

    private volatile @Nullable List<IAnimationHook> hooks;

    /**
     * The task that moves the animated blocks.
     * <p>
     * This will be null until the animation starts (if it does, see {@link #skipAnimation}).
     */
    private volatile @Nullable TimerTask moverTask = null;

    /**
     * The ID of the {@link #moverTask}.
     */
    private volatile @Nullable Integer moverTaskID = null;

    /**
     * The duration of the animation measured in ticks.
     */
    private final int animationDuration;

    /**
     * The cuboid that describes the location of the structure after the blocks have been moved.
     */
    private final Cuboid oldCuboid;

    /**
     * The cuboid that describes the location of the structure after the blocks have been moved.
     */
    private final Cuboid newCuboid;


    private volatile @Nullable Animation<IAnimatedBlock> animationData;

    /**
     * Constructs a {@link Animator}.
     * <p>
     * Once created, the animation does not start immediately. Use {@link #startAnimation()} to start it.
     *
     * @param structure
     *     The {@link AbstractStructure} that is being animated.
     * @param data
     *     The data of the movement request.
     * @param animationComponent
     *     The animation component to use for the animation. This determines the type of animation.
     * @param animationBlockManager
     *     The manager of the animated blocks. This is responsible for handling the lifecycle of the animated blocks.
     */
    public Animator(
        AbstractStructure structure, AnimationRequestData data, IAnimationComponent animationComponent,
        IAnimationBlockManager animationBlockManager)
    {
        executor = data.getExecutor();
        structureActivityManager = data.getStructureActivityManager();
        animationHookManager = data.getAnimationHookManager();
        serverTickTime = data.getServerTickTime();

        this.structure = structure;
        this.snapshot = data.getStructureSnapshot();
        this.time = data.getAnimationTime();
        this.skipAnimation = data.isAnimationSkipped();
        this.player = data.getResponsible();
        this.animationComponent = animationComponent;
        this.animationBlockManager = animationBlockManager;
        this.newCuboid = data.getNewCuboid();
        this.oldCuboid = snapshot.getCuboid();
        this.cause = data.getCause();
        this.animationType = data.getAnimationType();
        this.actionType = data.getActionType();

        // Some animation types may place constraints on the duration of the animation.
        // When this is the case, we limit the duration here.
        // The components do not need to take this into account, as they use the duration
        // to figure out how fast they need to move, which should not change because of a time limit.
        final double animationTime = Math.min(animationType.getAnimationDurationLimit(), data.getAnimationTime());
        this.animationDuration = AnimationUtil.getAnimationTicks(animationTime, data.getServerTickTime());

        this.perpetualMovement = !data.isPreventPerpetualMovement() && isPerpetualMovement();
    }

    private boolean isPerpetualMovement()
    {
        return this.animationType.allowsPerpetualAnimation() && structure instanceof IPerpetualMover;
    }

    /**
     * Stops the animation gracefully. May cause the animation to restart later.
     */
    public void stopAnimation()
    {
        this.stopAnimation(animationData);
    }

    /**
     * Aborts the animation.
     */
    public void abort()
    {
        final @Nullable TimerTask moverTask0 = moverTask;
        if (moverTask0 != null)
            executor.cancel(moverTask0, Objects.requireNonNull(moverTaskID));
        putBlocks();
        forEachHook("onAnimationAborted", IAnimationHook::onAnimationAborted);
    }

    @Override
    public List<IAnimatedBlock> getAnimatedBlocks()
    {
        return animationBlockManager.getAnimatedBlocks();
    }

    /**
     * Replaces all blocks of the {@link AbstractStructure} with animated blocks and starts the animation.
     * <p>
     * Note that if {@link #skipAnimation} is true, the blocks will be placed in the new position immediately without
     * any animations.
     */
    public void startAnimation()
    {
        if (animationDuration < 0)
            throw new IllegalStateException(
                "Trying to start an animation with invalid endCount value: " + animationDuration);

        executor.runOnMainThread(
            () ->
            {
                try
                {
                    startAnimation0();
                }
                catch (Exception e)
                {
                    log.atSevere().withCause(e).log("Failed to start animation!");
                    handleInitFailure();
                }
            });
    }

    /**
     * @throws IllegalStateException
     *     1) When called asynchronously; this method needs to be called on the main thread as determined by
     *     {@link IExecutor#isMainThread()}.
     *     <p>
     *     2) When {@link #hasStarted} has already been set to true.
     */
    private void startAnimation0()
    {
        executor.assertMainThread("Animations must be started on the main thread!");

        if (hasStarted.getAndSet(true))
            throw new IllegalStateException("Trying to start an animation again!");

        final Animation<IAnimatedBlock> animation = new Animation<>(
            animationDuration, perpetualMovement, oldCuboid, getAnimatedBlocks(),
            snapshot, structure.getType(), animationType);

        this.animationData = animation;

        if (!animationBlockManager.createAnimatedBlocks(snapshot, animationComponent))
        {
            handleInitFailure();
            return;
        }

        final boolean animationSkipped = skipAnimation || getAnimatedBlocks().isEmpty();
        animation.setState(animationSkipped ? AnimationState.SKIPPED : AnimationState.ACTIVE);
        this.hooks = animationHookManager.instantiateHooks(animation);

        if (animationSkipped)
            putBlocks();
        else
            animateEntities(animation);
    }

    /**
     * Handles initialization failure.
     * <p>
     * This means that this block mover will be unregistered, that living animated blocks will be killed, and that we
     * will attempt to restore blocks to their original positions.
     */
    private void handleInitFailure()
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Trying to handle init failure asynchronously!");
            executor.runOnMainThread(this::handleInitFailure);
            return;
        }

        animationBlockManager.restoreBlocksOnFailure();
        structureActivityManager.processFinishedAnimation(this);
    }

    private void executeAnimationStep(int counter, Animation<IAnimatedBlock> animation)
    {
        animationComponent.executeAnimationStep(this, counter);
        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.ACTIVE);
    }

    @Override
    public void applyMovement(IAnimatedBlock animatedBlock, RotatedPosition targetPosition)
    {
        animatedBlock.moveToTarget(targetPosition);
    }

    private void executeFinishingStep(Animation<IAnimatedBlock> animation)
    {
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, animatedBlock.getFinalPosition());

        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.FINISHING);
    }

    /**
     * Gracefully stops the animation: Freeze any animated blocks, kill the animation task and place the blocks in their
     * new location.
     */
    private void stopAnimation(@Nullable Animation<IAnimatedBlock> animation)
    {
        if (animation != null)
        {
            animation.setRegion(getAnimationRegion());
            animation.setState(AnimationState.STOPPING);
        }

        forEachHook("onAnimationEnding", IAnimationHook::onAnimationEnding);

        putBlocks();

        final @Nullable TimerTask moverTask0 = moverTask;
        if (moverTask0 == null)
        {
            log.atWarning().log("MoverTask unexpectedly null for BlockMover:\n%s", this);
            return;
        }
        executor.cancel(moverTask0, Objects.requireNonNull(moverTaskID));

        if (animation != null)
        {
            animation.setState(AnimationState.COMPLETED);
            animation.setRegion(oldCuboid);
        }

        executor.runAsyncLater(structure::verifyRedstoneState, VERIFY_REDSTONE_DELAY);
    }

    /**
     * This method is called right before the animation is started and spawns the animated blocks.
     * <p>
     * Overriding methods should not forget to either call this method or spawn the animated blocks themselves.
     */
    private void prepareAnimation()
    {
        executor.assertMainThread("Animated blocks must be spawned on the main thread!");
        getAnimatedBlocks().forEach(IAnimatedBlock::spawn);
        animationComponent.prepareAnimation(this);
    }

    /**
     * Calculates the number of ticks required in addition to the base duration of the animation to gracefully end an
     * animation.
     *
     * @return The number of ticks required to gracefully stop the animation.
     */
    private int getStopCount()
    {
        // Perpetual movers will usually not require to stop gracefully; Their blocks are not meant to
        // travel to a given position, so they can stop at any position and adding a stopCount can look glitchy.
        if (structure instanceof IPerpetualMover)
            return 0;

        final int finishDurationTicks = Math.round((float) FINISH_DURATION / serverTickTime);
        return animationDuration + Math.max(0, finishDurationTicks);
    }

    /**
     * Runs the animation of the animated blocks.
     */
    private void animateEntities(Animation<IAnimatedBlock> animation)
    {
        executor.assertMainThread("Animation must be started on the main thread!");

        try
        {
            prepareAnimation();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to prepare animation!");
            handleInitFailure();
            return;
        }

        forEachHook("onPrepare", IAnimationHook::onPrepare);

        final int stopCount = getStopCount();

        final TimerTask moverTask0 = new TimerTask()
        {
            private int counter = 0;

            @Override
            public void run()
            {
                try
                {
                    forEachHook("onPreAnimationStep", IAnimationHook::onPreAnimationStep);
                    ++counter;

                    if (perpetualMovement || counter <= animationDuration)
                        executeAnimationStep(counter, animation);
                    else if (counter > stopCount)
                        stopAnimation(animation);
                    else
                        executeFinishingStep(animation);

                    animation.setStepsExecuted(counter);
                    forEachHook("onPostAnimationStep", IAnimationHook::onPostAnimationStep);
                }
                catch (Exception e)
                {
                    // TODO: Stop animation etc.
                    log.atSevere().withCause(e).log("Failed to execute animation step!");
                }
            }
        };
        moverTask = moverTask0;
        moverTaskID = executor.runAsyncRepeated(moverTask0, START_DELAY, this.serverTickTime);
    }

    private void putBlocks0()
    {
        executor.assertMainThread("Attempting async block placement!");

        animationBlockManager.handleAnimationCompletion();

        if (animationType.requiresWriteAccess())
            // Tell the structure object it has been opened and what its new coordinates are.
            structure.withWriteLock(this::updateCoords);

        forEachHook("onAnimationCompleted", IAnimationHook::onAnimationCompleted);

        structureActivityManager.processFinishedAnimation(this);
    }

    /**
     * Places all the blocks of the structure in their final position and kills all the animated blocks.
     */
    private void putBlocks()
    {
        // Only allow this method to be run once! If it can be run multiple times, it'll cause structure corruption
        // because while the blocks have already been placed, the coordinates can still be toggled!
        if (isFinished.getAndSet(true))
            return;
        executor.runOnMainThread(this::putBlocks0);
    }

    /**
     * Updates the coordinates of a {@link AbstractStructure} and toggles its open status.
     */
    private void updateCoords()
    {
        structure.setOpen(!snapshot.isOpen());
        if (!newCuboid.equals(snapshot.getCuboid()))
            structure.setCoordinates(newCuboid);
        structure.syncData().exceptionally(Util::exceptionally);
    }

    /**
     * Gets the UID of the {@link AbstractStructure} being moved.
     *
     * @return The UID of the {@link AbstractStructure} being moved.
     */
    public long getStructureUID()
    {
        return snapshot.getUid();
    }

    private Cuboid getAnimationRegion()
    {
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double zMin = Double.MAX_VALUE;

        double xMax = Double.MIN_VALUE;
        double yMax = Double.MIN_VALUE;
        double zMax = Double.MIN_VALUE;

        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
        {
            final Vector3Dd pos = animatedBlock.getPosition();
            if (pos.x() < xMin)
                xMin = pos.x();
            else if (pos.x() > xMax)
                xMax = pos.x();

            if (pos.y() < yMin)
                yMin = pos.y();
            else if (pos.y() > yMax)
                yMax = pos.y();

            if (pos.z() < zMin)
                zMin = pos.z();
            else if (pos.z() > zMax)
                zMax = pos.z();
        }
        return Cuboid.of(new Vector3Dd(xMin, yMin, zMin), new Vector3Dd(xMax, yMax, zMax), Cuboid.RoundingMode.OUTWARD);
    }

    private void forEachHook(String actionName, Consumer<IAnimationHook> call)
    {
        final @Nullable var hooks0 = hooks;
        if (hooks0 == null)
            return;

        for (final IAnimationHook hook : hooks0)
        {
            log.atFinest().log("Executing '%s' for hook '%s'!", actionName, hook.getName());
            try
            {
                call.accept(hook);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e)
                   .log("Failed to execute '%s' for hook '%s'!", actionName, hook.getName());
            }
        }
    }
}
