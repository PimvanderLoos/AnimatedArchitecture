package nl.pim16aap2.bigdoors.core.moveblocks;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IExecutor;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimationHook;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.core.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimation.AnimationState;

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
     * The delay (measured in milliseconds) between initialization of the animation and starting to move the blocks.
     */
    private static final int START_DELAY = 700;

    /**
     * The delay (in ticks) before verifying the redstone state after the animation has ended.
     */
    private static final long VERIFY_REDSTONE_DELAY = 20;

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

    private final MovementMethod movementMethod;

    /**
     * The amount of time (in seconds) that the animation will take.
     * <p>
     * This excludes any additional time specified by {@link MovementMethod#finishDuration()}.
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

    private volatile @Nullable List<IAnimationHook<IAnimatedBlock>> hooks;

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
        AbstractStructure structure, StructureRequestData data, IAnimationComponent animationComponent,
        IAnimationBlockManager animationBlockManager)
    {
        executor = data.getExecutor();
        structureActivityManager = data.getStructureActivityManager();
        animationHookManager = data.getAnimationHookManager();
        serverTickTime = data.getServerTickTime();

        this.movementMethod = animationComponent.getMovementMethod();
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

        this.perpetualMovement = isPerpetualMovement();
    }

    private boolean isPerpetualMovement()
    {
        return this.animationType.allowsPerpetualAnimation() &&
            structure instanceof IPerpetualMover perpetualMover &&
            perpetualMover.isPerpetual();
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

    /**
     * Rotates an {@link IAnimatedBlock} in the provided direction and then respawns it. Note that this is executed on
     * the thread it was called from, which MUST BE the main thread!
     */
    private void applyRotation0(MovementDirection direction)
    {
        for (final IAnimatedBlock animatedBlock : animationBlockManager.getAnimatedBlocks())
            if (animatedBlock.getAnimatedBlockData().canRotate() &&
                animatedBlock.getAnimatedBlockData().rotateBlock(direction))
                animatedBlock.respawn();
    }

    @Override
    public List<IAnimatedBlock> getAnimatedBlocks()
    {
        return animationBlockManager.getAnimatedBlocks();
    }

    @Override
    public void applyRotation(MovementDirection direction)
    {
        executor.runSync(() -> this.applyRotation0(direction));
    }

    /**
     * Respawns all blocks. Note that this is executed on the thread it was called from, which MUST BE the main thread!
     */
    private void respawnBlocksOnCurrentThread()
    {
        getAnimatedBlocks().forEach(IAnimatedBlock::respawn);
    }

    @Override
    public void respawnBlocks()
    {
        executor.runSync(this::respawnBlocksOnCurrentThread);
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
            throw new IllegalStateException("Trying to start an animation with invalid endCount value: " +
                                                animationDuration);
        executor.runOnMainThread(this::startAnimation0);
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
            animationDuration, oldCuboid, getAnimatedBlocks(), snapshot, structure.getType(), animationType);
        this.animationData = animation;

        final AnimationContext animationContext = new AnimationContext(structure.getType(), snapshot, animation);

        if (!animationBlockManager.createAnimatedBlocks(snapshot, animationComponent, animationContext, movementMethod))
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

    /**
     * Runs a single step of the animation.
     *
     * @param ticks
     *     The number of ticks that have passed since the start of the animation.
     * @param ticksRemaining
     */
    private void executeAnimationStep(int ticks, int ticksRemaining)
    {
        animationComponent.executeAnimationStep(this, ticks, ticksRemaining);
    }

    private void executeAnimationStep(int counter, Animation<IAnimatedBlock> animation)
    {
        executeAnimationStep(counter, animation.getRemainingSteps());

        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.ACTIVE);
    }

    @Override
    public void applyMovement(IAnimatedBlock animatedBlock, IVector3D targetPosition, int ticksRemaining)
    {
        animatedBlock.moveToTarget(new Vector3Dd(targetPosition), ticksRemaining);
    }

    private void executeFinishingStep(Animation<IAnimatedBlock> animation)
    {
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, animatedBlock.getFinalPosition(), -1);

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

        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            animatedBlock.setVelocity(new Vector3Dd(0D, 0D, 0D));

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

        final int finishDurationTicks = Math.round((float) movementMethod.finishDuration() / serverTickTime);
        final int stopCount = animationDuration + Math.max(0, finishDurationTicks);

        final int initialDelay = Math.round((float) START_DELAY / serverTickTime);

        final TimerTask moverTask0 = new TimerTask()
        {
            private int counter = 0;
            private @Nullable Long startTime = null; // Initialize on the first run.
            private long currentTime = System.nanoTime();

            @Override
            public void run()
            {
                if (startTime == null)
                    startTime = System.nanoTime();
                forEachHook("onPreAnimationStep", IAnimationHook::onPreAnimationStep);
                ++counter;

                final long lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (perpetualMovement || counter <= animationDuration)
                    executeAnimationStep(counter, animation);
                else if (counter > stopCount)
                    stopAnimation(animation);
                else
                    executeFinishingStep(animation);

                animation.setStepsExecuted(counter);
                forEachHook("onPostAnimationStep", IAnimationHook::onPostAnimationStep);
            }
        };
        moverTask = moverTask0;
        moverTaskID = executor.runAsyncRepeated(moverTask0, initialDelay, 1);
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
        structure.syncData();
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

    private void forEachHook(String actionName, Consumer<IAnimationHook<IAnimatedBlock>> call)
    {
        final @Nullable var hooks0 = hooks;
        if (hooks0 == null)
            return;

        for (final IAnimationHook<IAnimatedBlock> hook : hooks0)
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

    /**
     * Represents the different ways in which an animated block can be moved.
     */
    @SuppressWarnings("unused")
    @ToString
    public abstract static class MovementMethod
    {
        /**
         * The animated blocks are moved in the direction of the goal position by applying a velocity along the vector
         * between the current and the target positions.
         * <p>
         * When this method is used, animated blocks will always lag slightly behind the target position, and they
         * generally tend to move slightly towards the center of any rotation (so corners will be rounded, circles
         * slightly smaller).
         */
        public static final MovementMethod VELOCITY = new MovementMethod("VELOCITY", 1500)
        {
            @Override
            public void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos, int ticksRemaining)
            {
                animatedBlock.setVelocity(goalPos.subtract(animatedBlock.getCurrentPosition()));
            }
        };

        /**
         * Teleports the animated blocks directly to their target positions.
         */
        public static final MovementMethod TELEPORT = new MovementMethod("TELEPORT", 100)
        {
            @Override
            public void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos, int ticksRemaining)
            {
                animatedBlock.teleport(goalPos);
            }
        };

        /**
         * Combination of {@link #TELEPORT} and {@link #VELOCITY}.
         */
        public static final MovementMethod TELEPORT_VELOCITY = new MovementMethod("TELEPORT", 600)
        {
            /**
             * Only teleport the animated blocks once every several ticks.
             */
            private static final int TELEPORT_FREQUENCY = 3;

            @Override
            public void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos, int ticksRemaining)
            {
                if (ticksRemaining == 0 ||
                    (ticksRemaining > 0 && animatedBlock.getTicksLived() % TELEPORT_FREQUENCY == 0))
                    animatedBlock.teleport(goalPos, IAnimatedBlock.TeleportMode.ABSOLUTE);

                final Vector3Dd velocitySourcePosition =
                    ticksRemaining < 2 ? animatedBlock.getCurrentPosition() : animatedBlock.getPreviousTarget();
                animatedBlock.setVelocity(goalPos.subtract(velocitySourcePosition));
            }
        };

        private final String name;

        /**
         * The duration (measured in milliseconds) of the final animation state that moves blocks to their final
         * position.
         */
        private final int finishDuration;

        protected MovementMethod(String name, int finishDuration)
        {
            this.name = name;
            this.finishDuration = finishDuration;
        }

        public String name()
        {
            return name;
        }

        /**
         * The duration (measured in milliseconds) of the final step executed after the animation has ended. This step
         * is used to move the animated blocks to their final positions gracefully.
         */
        public int finishDuration()
        {
            return finishDuration;
        }

        /**
         * Moves an animated block to a given goal position using the specified method.
         */
        public abstract void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos, int ticksRemaining);
    }
}
