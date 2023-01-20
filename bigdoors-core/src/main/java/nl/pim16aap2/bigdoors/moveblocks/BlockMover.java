package nl.pim16aap2.bigdoors.moveblocks;

import com.google.common.flogger.StackSize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimationHook;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nl.pim16aap2.bigdoors.api.animatedblock.IAnimation.AnimationState;

/**
 * Represents a class that animates blocks.
 *
 * @author Pim
 */
@ToString
@Flogger
public abstract class BlockMover
{
    /**
     * The delay (measured in milliseconds) between initialization of the animation and starting to move the blocks.
     */
    private static final int START_DELAY = 700;

    private final boolean drawDebugBlocks = false;

    /**
     * The movable whose blocks are going to be moved.
     */
    @Getter
    private final AbstractMovable movable;

    /**
     * A snapshot of the movable created before the toggle.
     */
    @Getter
    protected final MovableSnapshot snapshot;

    /**
     * The player responsible for the movement.
     * <p>
     * This player may be offline.
     */
    @Getter
    protected final IPPlayer player;

    /**
     * What caused the movable to be moved.
     */
    @Getter
    private final MovableActionCause cause;

    /**
     * The type of action that is fulfilled by moving the movable.
     */
    @Getter
    private final MovableActionType actionType;

    @ToString.Exclude
    protected final IAnimatedBlockFactory animatedBlockFactory;

    @ToString.Exclude
    protected final MovableActivityManager movableActivityManager;

    @ToString.Exclude
    protected final AutoCloseScheduler autoCloseScheduler;

    @ToString.Exclude
    protected final IPExecutor executor;

    @ToString.Exclude
    protected final GlowingBlockSpawner glowingBlockSpawner;

    @ToString.Exclude
    protected final IPLocationFactory locationFactory;

    @ToString.Exclude
    private final AnimationHookManager animationHookManager;

    @ToString.Exclude
    protected final int serverTickTime;

    /**
     * The type of movement to apply to animated blocks.
     * <p>
     * Subclasses are free to override this if a different type of movement is desired for that type.
     * <p>
     * Each animated block is moved using {@link MovementMethod#apply(IAnimatedBlock, Vector3Dd, int)}.
     */
    protected volatile MovementMethod movementMethod = MovementMethod.TELEPORT_VELOCITY;

    /**
     * The amount of time (in seconds) that the animation will take.
     * <p>
     * This excludes any additional time specified by {@link MovementMethod#finishDuration()}.
     */
    @Getter
    protected final double time;

    /**
     * When true, the blocks are moved without animating them. No animated blocks are spawned.
     */
    @Getter
    protected final boolean skipAnimation;

    /**
     * The direction in of the movement.
     */
    protected final RotateDirection openDirection;

    /**
     * The modifiable list of animated blocks.
     */
    @ToString.Exclude
    private final List<IAnimatedBlock> privateAnimatedBlocks;

    /**
     * The (unmodifiable) list of animated blocks.
     */
    @ToString.Exclude
    @Getter(AccessLevel.PROTECTED)
    private final List<IAnimatedBlock> animatedBlocks;

    /**
     * True for types of movement that are supposed to keep going until otherwise stopped. For example, flags,
     * windmills, etc.
     * <p>
     * False to have the movement be time-bound, such as for doors, drawbridges, etc.
     */
    protected volatile boolean perpetualMovement = false;

    /**
     * Keeps track of whether the animation has finished.
     */
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    /**
     * Keeps track of whether the animation has started.
     */
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);

    private volatile @Nullable List<IAnimationHook<IAnimatedBlock>> hooks;

    /**
     * The task that moves the animated blocks.
     * <p>
     * This will be null until the animation starts (if it does, see {@link #skipAnimation}).
     */
    protected volatile @Nullable TimerTask moverTask = null;

    /**
     * The ID of the {@link #moverTask}.
     */
    @Getter(AccessLevel.PROTECTED)
    private volatile @Nullable Integer moverTaskID = null;

    /**
     * The duration of the animation measured in ticks.
     */
    protected final int animationDuration;

    /**
     * The cuboid that describes the location of the movable after the blocks have been moved.
     */
    protected final Cuboid oldCuboid;

    /**
     * The cuboid that describes the location of the movable after the blocks have been moved.
     */
    protected final Cuboid newCuboid;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param snapshot
     *     A snapshot of the movable created before the toggle.
     * @param time
     *     See {@link #time}.
     * @param skipAnimation
     *     If the movable should be opened instantly (i.e. skip animation) or not.
     * @param openDirection
     *     The direction the {@link AbstractMovable} will move.
     * @param player
     *     The player who opened this movable.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the movable will take up after the toggle.
     */
    protected BlockMover(
        Context context, AbstractMovable movable, MovableSnapshot snapshot, double time, boolean skipAnimation,
        RotateDirection openDirection, IPPlayer player, Cuboid newCuboid,
        MovableActionCause cause, MovableActionType actionType)
        throws Exception
    {
        executor = context.getExecutor();
        movableActivityManager = context.getMovableActivityManager();
        autoCloseScheduler = context.getAutoCloseScheduler();
        animatedBlockFactory = context.getAnimatedBlockFactory();
        locationFactory = context.getLocationFactory();
        animationHookManager = context.getAnimationHookManager();
        glowingBlockSpawner = context.getGlowingBlockSpawner();
        serverTickTime = context.getServerTickTime();

        autoCloseScheduler.unscheduleAutoClose(snapshot.getUid());
        this.movable = movable;
        this.snapshot = snapshot;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.openDirection = openDirection;
        this.player = player;
        privateAnimatedBlocks = new CopyOnWriteArrayList<>();
        animatedBlocks = Collections.unmodifiableList(privateAnimatedBlocks);
        this.newCuboid = newCuboid;
        this.oldCuboid = snapshot.getCuboid();
        this.cause = cause;
        this.actionType = actionType;
        this.perpetualMovement = movable instanceof IPerpetualMover perpetualMover && perpetualMover.isPerpetual();

        this.animationDuration = (int) Math.min(Integer.MAX_VALUE, Math.round(1000 * this.time / serverTickTime));
    }

    public void abort()
    {
        final @Nullable TimerTask moverTask0 = moverTask;
        if (moverTask0 != null)
            executor.cancel(moverTask0, Objects.requireNonNull(moverTaskID));
        putBlocks(true);
    }

    /**
     * Rotates in the {@link #openDirection} and then respawns the {@link IAnimatedBlock}. Note that this is executed on
     * the thread it was called from, which MUST BE the main thread!
     */
    private void applyRotationOnCurrentThread()
    {
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            if (animatedBlock.getAnimatedBlockData().canRotate() &&
                animatedBlock.getAnimatedBlockData().rotateBlock(openDirection))
                animatedBlock.respawn();
    }

    /**
     * Rotates in the {@link #openDirection} and then respawns an {@link IAnimatedBlock}. This is executed on the main
     * thread.
     */
    protected void applyRotation()
    {
        executor.runSync(this::applyRotationOnCurrentThread);
    }

    /**
     * Respawns all blocks. Note that this is executed on the thread it was called from, which MUST BE the main thread!
     */
    private void respawnBlocksOnCurrentThread()
    {
        animatedBlocks.forEach(IAnimatedBlock::respawn);
    }

    /**
     * Respawns all blocks. This is executed on the main thread.
     */
    protected final void respawnBlocks()
    {
        executor.runSync(this::respawnBlocksOnCurrentThread);
    }

    /**
     * Replaces all blocks of the {@link AbstractMovable} with animated blocks and starts the animation.
     * <p>
     * Note that if {@link #skipAnimation} is true, the blocks will be placed in the new position immediately without
     * any animations.
     */
    public final void startAnimation()
    {
        if (animationDuration < 0)
            throw new IllegalStateException("Trying to start an animation with invalid endCount value: " +
                                                animationDuration);
        executor.runOnMainThread(this::startAnimation0);
    }

    /**
     * @throws IllegalStateException
     *     1) When called asynchronously; this method needs to be called on the main thread as determined by
     *     {@link IPExecutor#isMainThread()}.
     *     <p>
     *     2) When {@link #hasStarted} has already been set to true.
     */
    private void startAnimation0()
    {
        executor.assertMainThread("Animations must be started on the main thread!");

        if (hasStarted.getAndSet(true))
            throw new IllegalStateException("Trying to start an animation again!");

        final Animation<IAnimatedBlock> animation = new Animation<>(
            animationDuration, oldCuboid, animatedBlocks, snapshot, movable.getType());
        final AnimationContext animationContext = new AnimationContext(movable.getType(), snapshot,
                                                                       animation);
        final List<IAnimatedBlock> newAnimatedBlocks = new ArrayList<>(snapshot.getBlockCount());

        try
        {
            final int xMin = oldCuboid.getMin().x();
            final int yMin = oldCuboid.getMin().y();
            final int zMin = oldCuboid.getMin().z();

            final int xMax = oldCuboid.getMax().x();
            final int yMax = oldCuboid.getMax().y();
            final int zMax = oldCuboid.getMax().z();

            for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (int yAxis = yMax; yAxis >= yMin; --yAxis)
                    for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    {
                        final boolean onEdge =
                            xAxis == xMin || xAxis == xMax ||
                                yAxis == yMin || yAxis == yMax ||
                                zAxis == zMin || zAxis == zMax;

                        final IPLocation location =
                            locationFactory.create(snapshot.getWorld(), xAxis + 0.5, yAxis, zAxis + 0.5);
                        final boolean bottom = (yAxis == yMin);
                        final float radius = getRadius(xAxis, yAxis, zAxis);
                        final float startAngle = getStartAngle(xAxis, yAxis, zAxis);
                        final Vector3Dd startPosition = new Vector3Dd(xAxis + 0.5, yAxis, zAxis + 0.5);
                        final Vector3Dd finalPosition = getFinalPosition(startPosition, radius);

                        animatedBlockFactory
                            .create(location, radius, startAngle, bottom, onEdge, animationContext, finalPosition,
                                    movementMethod)
                            .ifPresent(newAnimatedBlocks::add);
                    }
            this.privateAnimatedBlocks.addAll(newAnimatedBlocks);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
            // Add the block anyway, so we can deal with them in the initFailure method.
            this.privateAnimatedBlocks.addAll(newAnimatedBlocks);
            handleInitFailure();
            return;
        }

        if (!tryRemoveOriginalBlocks(false) || !tryRemoveOriginalBlocks(true))
            return;

        final boolean animationSkipped = skipAnimation || privateAnimatedBlocks.isEmpty();
        animation.setState(animationSkipped ? AnimationState.SKIPPED : AnimationState.ACTIVE);
        this.hooks = animationHookManager.instantiateHooks(animation);

        if (animationSkipped)
            putBlocks(false);
        else
            animateEntities(animation);
    }

    /**
     * Tries to remove the original blocks of all blocks in {@link #privateAnimatedBlocks}.
     * <p>
     * If an exception is thrown while removing the original blocks, the process is finished using
     * {@link #handleInitFailure()}.
     *
     * @param edgePass
     *     True to do a pass over the edges specifically.
     * @return True if the original blocks could be spawned. If something went wrong and the process had to be aborted,
     * false is returned instead.
     */
    private boolean tryRemoveOriginalBlocks(boolean edgePass)
    {
        executor.assertMainThread("Blocks must be removed on the main thread!");

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
        {
            try
            {
                if (edgePass && !animatedBlock.isOnEdge())
                    continue;
                animatedBlock.getAnimatedBlockData().deleteOriginalBlock(edgePass);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e)
                   .log("Failed to remove original block. Trying to restore blocks now...");
                handleInitFailure();
                return false;
            }
        }
        return true;
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

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
        {
            try
            {
                animatedBlock.kill();
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to kill animated block: %s", animatedBlock);
            }
            try
            {
                final Vector3Dd startPos = animatedBlock.getStartPosition();
                final Vector3Di goalPos = new Vector3Di((int) startPos.x(),
                                                        (int) Math.round(startPos.y()),
                                                        (int) startPos.z());
                animatedBlock.getAnimatedBlockData().putBlock(goalPos);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to restore block: %s", animatedBlock);
            }
        }
        privateAnimatedBlocks.clear();
        movableActivityManager.processFinishedBlockMover(this, false);
    }

    /**
     * @param startLocation
     *     The start location of a block.
     * @param radius
     *     The radius of the block to the rotation point.
     * @return The final position of an {@link IAnimatedBlock}.
     */
    protected abstract Vector3Dd getFinalPosition(IVector3D startLocation, float radius);

    /**
     * Runs a single step of the animation.
     *
     * @param ticks
     *     The number of ticks that have passed since the start of the animation.
     * @param ticksRemaining
     */
    protected abstract void executeAnimationStep(int ticks, int ticksRemaining);

    private void executeAnimationStep(int counter, Animation<IAnimatedBlock> animation)
    {
        executeAnimationStep(counter, animation.getRemainingSteps());

        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.ACTIVE);
    }

    protected final void applyMovement(IAnimatedBlock animatedBlock, IVector3D targetPosition, int ticksRemaining)
    {
        if (drawDebugBlocks)
            drawDebugBlock(targetPosition);
        animatedBlock.moveToTarget(new Vector3Dd(targetPosition), ticksRemaining);
    }

    private void drawDebugBlock(IVector3D finalPosition)
    {
        glowingBlockSpawner.builder()
                           .atPosition(finalPosition)
                           .inWorld(snapshot.getWorld())
                           .forDuration(Duration.ofMillis(250))
                           .withColor(PColor.GOLD)
                           .forPlayer(player)
                           .build();
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
    private void stopAnimation(Animation<IAnimatedBlock> animation)
    {
        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.STOPPING);

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            animatedBlock.setVelocity(new Vector3Dd(0D, 0D, 0D));

        forEachHook("onAnimationEnding", IAnimationHook::onAnimationEnding);

        putBlocks(false);

        final @Nullable TimerTask moverTask0 = moverTask;
        if (moverTask0 == null)
        {
            log.atWarning().log("MoverTask unexpectedly null for BlockMover:\n%s", this);
            return;
        }
        executor.cancel(moverTask0, Objects.requireNonNull(moverTaskID));

        animation.setState(AnimationState.COMPLETED);
        animation.setRegion(oldCuboid);
    }

    /**
     * This method is called right before the animation is started and spawns the animated blocks.
     * <p>
     * Overriding methods should not forget to either call this method or spawn the animated blocks themselves.
     */
    protected void prepareAnimation()
    {
        executor.assertMainThread("Animated blocks must be spawned on the main thread!");
        getAnimatedBlocks().forEach(IAnimatedBlock::spawn);
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

    /**
     * Gets the radius of a block at the given coordinates.
     *
     * @param xAxis
     *     The x coordinate.
     * @param yAxis
     *     The y coordinate.
     * @param zAxis
     *     The z coordinate.
     * @return The radius of a block at the given coordinates.
     */
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        return -1;
    }

    /**
     * Gets the starting angle of a block (in rads) at the given coordinates.
     *
     * @param xAxis
     *     The x coordinate.
     * @param yAxis
     *     The y coordinate.
     * @param zAxis
     *     The z coordinate.
     * @return The starting angle of a block at the given coordinates.
     */
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return -1;
    }

    private void putBlocks0(boolean onDisable)
    {
        executor.assertMainThread("Attempting async block placement!");

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
        {
            animatedBlock.kill();
            animatedBlock.getAnimatedBlockData().putBlock(animatedBlock.getFinalPosition());
        }

        // Tell the movable object it has been opened and what its new coordinates are.
        movable.withWriteLock(this::updateCoords);

        privateAnimatedBlocks.clear();

        forEachHook("onAnimationCompleted", IAnimationHook::onAnimationCompleted);

        if (onDisable)
            return;

        movableActivityManager.processFinishedBlockMover(this, true);
    }

    /**
     * Places all the blocks of the movable in their final position and kills all the animated blocks.
     * <p>
     * When the plugin is currently not in the process of disabling, it also schedules the auto close.
     *
     * @param onDisable
     *     Whether the plugin is currently being disabled.
     */
    public final void putBlocks(boolean onDisable)
    {
        // Only allow this method to be run once! If it can be run multiple times, it'll cause movable corruption
        // because while the blocks have already been placed, the coordinates can still be toggled!
        if (isFinished.getAndSet(true))
            return;
        executor.runOnMainThread(() -> putBlocks0(onDisable));
    }

    /**
     * Updates the coordinates of a {@link AbstractMovable} and toggles its open status.
     */
    private void updateCoords()
    {
        movable.setOpen(!snapshot.isOpen());
        if (!newCuboid.equals(snapshot.getCuboid()))
            movable.setCoordinates(newCuboid);
        movable.syncData();
    }

    /**
     * Gets the UID of the {@link AbstractMovable} being moved.
     *
     * @return The UID of the {@link AbstractMovable} being moved.
     */
    public final long getMovableUID()
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

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
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

    @Getter
    public static final class Context
    {
        private final MovableActivityManager movableActivityManager;
        private final AutoCloseScheduler autoCloseScheduler;
        private final IPLocationFactory locationFactory;
        private final IAudioPlayer audioPlayer;
        private final IPExecutor executor;
        private final IAnimatedBlockFactory animatedBlockFactory;
        private final AnimationHookManager animationHookManager;
        private final GlowingBlockSpawner glowingBlockSpawner;
        private final IConfigLoader config;
        private final int serverTickTime;

        @Inject
        public Context(
            MovableActivityManager movableActivityManager,
            AutoCloseScheduler autoCloseScheduler,
            IPLocationFactory locationFactory,
            IAudioPlayer audioPlayer,
            IPExecutor executor,
            IAnimatedBlockFactory animatedBlockFactory,
            AnimationHookManager animationHookManager,
            GlowingBlockSpawner glowingBlockSpawner,
            IConfigLoader config,
            @Named("serverTickTime") int serverTickTime)
        {
            this.movableActivityManager = movableActivityManager;
            this.autoCloseScheduler = autoCloseScheduler;
            this.locationFactory = locationFactory;
            this.audioPlayer = audioPlayer;
            this.executor = executor;
            this.animatedBlockFactory = animatedBlockFactory;
            this.animationHookManager = animationHookManager;
            this.glowingBlockSpawner = glowingBlockSpawner;
            this.config = config;
            this.serverTickTime = serverTickTime;
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
