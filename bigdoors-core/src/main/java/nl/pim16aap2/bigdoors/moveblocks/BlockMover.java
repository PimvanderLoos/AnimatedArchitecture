package nl.pim16aap2.bigdoors.moveblocks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimationHook;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

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
    protected boolean drawDebugBlocks = false;

    /**
     * The world in which the blocks are going to be moved.
     */
    protected final IPWorld world;

    /**
     * The door whose blocks are going to be moved.
     */
    protected final AbstractDoor door;

    /**
     * The player responsible for the movement.
     * <p>
     * This player may be offline.
     */
    @Getter
    protected final IPPlayer player;

    /**
     * What caused the door to be moved.
     */
    @Getter
    private final DoorActionCause cause;

    /**
     * The type of action that is fulfilled by moving the door.
     */
    @Getter
    private final DoorActionType actionType;

    @ToString.Exclude
    protected final IAnimatedBlockFactory animatedBlockFactory;

    @ToString.Exclude
    protected final DoorActivityManager doorActivityManager;

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

    /**
     * The type of movement to apply to animated blocks.
     * <p>
     * Subclasses are free to override this if a different type of movement is desired for that type.
     * <p>
     * Each animated block is moved using {@link MovementMethod#apply(IAnimatedBlock, Vector3Dd)}.
     */
    protected MovementMethod movementMethod = MovementMethod.TELEPORT_VELOCITY;

    /**
     * The amount of time (in seconds) that an animation is requested to take.
     * <p>
     * The actual duration of the animation is likely to be different due to:
     * <p>
     * 1) {@link MovementMethod#finishDuration()}.
     * <p>
     * 2) There may be speed limits in place for animated blocks. This can result in lower time bounds that depend on
     * the shape of the door and the type of movement.
     */
    @Getter
    protected double time;

    /**
     * When true, the blocks are moved without animating them. No animated blocks are spawned.
     */
    @Getter
    protected boolean skipAnimation;

    /**
     * The direction in of the movement.
     */
    protected RotateDirection openDirection;

    /**
     * The modifiable list of animated blocks.
     */
    @ToString.Exclude
    private final ArrayList<IAnimatedBlock> privateAnimatedBlocks;

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
    protected boolean perpetualMovement = false;

    protected int xMin;

    protected int yMin;

    protected int zMin;

    protected int xMax;

    protected int yMax;

    protected int zMax;

    /**
     * Keeps track of whether the animation has finished.
     */
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    /**
     * Keeps track of whether the animation has started.
     */
    private volatile boolean hasStarted = false;

    private @Nullable List<IAnimationHook<IAnimatedBlock>> hooks;

    /**
     * The task that moves the animated blocks.
     * <p>
     * This will be null until the animation starts (if it does, see {@link #skipAnimation}).
     */
    protected @Nullable TimerTask moverTask = null;

    /**
     * The ID of the {@link #moverTask}.
     */
    @Getter(AccessLevel.PROTECTED)
    private @Nullable Integer moverTaskID = null;

    /**
     * The duration of the animation measured in ticks.
     */
    protected int animationDuration = -1;

    /**
     * The cuboid that describes the location of the door after the blocks have been moved.
     */
    protected final Cuboid newCuboid;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param time
     *     See {@link #time}.
     * @param skipAnimation
     *     If the door should be opened instantly (i.e. skip animation) or not.
     * @param openDirection
     *     The direction the {@link AbstractDoor} will move.
     * @param player
     *     The player who opened this door.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     */
    protected BlockMover(
        Context context, AbstractDoor door, double time, boolean skipAnimation,
        RotateDirection openDirection, IPPlayer player, Cuboid newCuboid,
        DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        executor = context.getExecutor();
        doorActivityManager = context.getDoorActivityManager();
        autoCloseScheduler = context.getAutoCloseScheduler();
        animatedBlockFactory = context.getAnimatedBlockFactory();
        locationFactory = context.getLocationFactory();
        animationHookManager = context.getAnimationHookManager();
        glowingBlockSpawner = context.getGlowingBlockSpawner();

        if (!context.getExecutor().isMainThread(Thread.currentThread().threadId()))
            throw new Exception("BlockMovers must be called on the main thread!");

        autoCloseScheduler.unscheduleAutoClose(door.getDoorUID());
        world = door.getWorld();
        this.door = door;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.openDirection = openDirection;
        this.player = player;
        privateAnimatedBlocks = new ArrayList<>(door.getBlockCount());
        animatedBlocks = Collections.unmodifiableList(privateAnimatedBlocks);
        this.newCuboid = newCuboid;
        this.cause = cause;
        this.actionType = actionType;

        xMin = door.getMinimum().x();
        yMin = door.getMinimum().y();
        zMin = door.getMinimum().z();
        xMax = door.getMaximum().x();
        yMax = door.getMaximum().y();
        zMax = door.getMaximum().z();
    }

    public void abort()
    {
        if (moverTask != null)
            executor.cancel(moverTask, Objects.requireNonNull(moverTaskID));
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
        privateAnimatedBlocks.forEach(IAnimatedBlock::respawn);
    }

    /**
     * Respawns all blocks. This is executed on the main thread.
     */
    protected final void respawnBlocks()
    {
        executor.runSync(this::respawnBlocksOnCurrentThread);
    }

    /**
     * Replaces all blocks of the {@link AbstractDoor} with animated blocks and starts the animation.
     * <p>
     * Note that if {@link #skipAnimation} is true, the blocks will be placed in the new position immediately without
     * any animations.
     */
    protected synchronized void startAnimation()
    {
        if (animationDuration < 0)
            throw new IllegalStateException("Trying to start an animation with invalid endCount value: " +
                                                animationDuration);
        if (hasStarted)
            throw new IllegalStateException("Trying to start an animation again!");
        hasStarted = true;

        final Animation<IAnimatedBlock> animation = new Animation<>(animationDuration, door.getCuboid(),
                                                                    privateAnimatedBlocks,
                                                                    door);
        final AnimationContext animationContext = new AnimationContext(door.getDoorType(), door, animation);

        try
        {
            for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (int yAxis = yMax; yAxis >= yMin; --yAxis)
                    for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    {
                        final boolean onEdge =
                            xAxis == xMin || xAxis == xMax ||
                                yAxis == yMin || yAxis == yMax ||
                                zAxis == zMin || zAxis == zMax;

                        final IPLocation location = locationFactory.create(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                        final boolean bottom = (yAxis == yMin);
                        final float radius = getRadius(xAxis, yAxis, zAxis);
                        final float startAngle = getStartAngle(xAxis, yAxis, zAxis);
                        final Vector3Dd startPosition = new Vector3Dd(xAxis + 0.5, yAxis, zAxis + 0.5);
                        final Vector3Dd finalPosition = getFinalPosition(startPosition, radius);

                        animatedBlockFactory
                            .create(location, radius, startAngle, bottom, onEdge, animationContext, finalPosition)
                            .ifPresent(privateAnimatedBlocks::add);
                    }
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            handleInitFailure();
            return;
        }

        privateAnimatedBlocks.trimToSize();

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
        for (final IAnimatedBlock animatedBlock : privateAnimatedBlocks)
        {
            try
            {
                if (edgePass && !animatedBlock.isOnEdge())
                    continue;
                animatedBlock.getAnimatedBlockData().deleteOriginalBlock(edgePass);
            }
            catch (Exception e)
            {
                log.at(Level.SEVERE).withCause(e)
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
        for (final IAnimatedBlock animatedBlock : privateAnimatedBlocks)
        {
            try
            {
                animatedBlock.kill();
            }
            catch (Exception e)
            {
                log.at(Level.SEVERE).withCause(e).log("Failed to kill animated block: %s", animatedBlock);
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
                log.at(Level.SEVERE).withCause(e).log("Failed to restore block: %s", animatedBlock);
            }
        }
        doorActivityManager.processFinishedBlockMover(this, false);
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
     */
    protected abstract void executeAnimationStep(int ticks);

    private void executeAnimationStep(int counter, Animation<IAnimatedBlock> animation)
    {
        executeAnimationStep(counter);

        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.ACTIVE);
    }

    /**
     * Runs a single step of the animation after the actual animation has completed.
     * <p>
     * This should be used to finish up the animation by moving the animated blocks to their final positions
     * gracefully.
     *
     * @param counter
     *     The number of ticks since the animation started.
     */
    protected void executeFinishingStep(@SuppressWarnings("unused") int counter)
    {
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, animatedBlock.getFinalPosition());
    }

    protected final void applyMovement(IAnimatedBlock animatedBlock, Vector3Dd finalPosition)
    {
        if (drawDebugBlocks)
            drawDebugBlock(finalPosition);
        movementMethod.apply(animatedBlock, finalPosition);
    }

    private void drawDebugBlock(Vector3Dd finalPosition)
    {
        glowingBlockSpawner.builder()
                           .atPosition(finalPosition)
                           .inWorld(world)
                           .forDuration(Duration.ofMillis(250))
                           .withColor(PColor.GOLD)
                           .forPlayer(player)
                           .build();
    }

    private void executeFinishingStep(int counter, Animation<IAnimatedBlock> animation)
    {
        executeFinishingStep(counter);

        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.FINISHING);
    }

    /**
     * Gracefully stops the animation: Freeze any animated blocks, kill the animation task and place the blocks in their
     * new location.
     */
    private synchronized void stopAnimation(Animation<IAnimatedBlock> animation)
    {
        animation.setRegion(getAnimationRegion());
        animation.setState(AnimationState.STOPPING);

        for (final IAnimatedBlock animatedBlock : privateAnimatedBlocks)
            animatedBlock.setVelocity(new Vector3Dd(0D, 0D, 0D));

        forEachHook("onAnimationEnding", IAnimationHook::onAnimationEnding);

        executor.runSync(() -> putBlocks(false));
        if (moverTask == null)
        {
            log.at(Level.WARNING).log("MoverTask unexpectedly null for BlockMover:\n%s", this);
            return;
        }
        executor.cancel(moverTask, Objects.requireNonNull(moverTaskID));

        animation.setState(AnimationState.COMPLETED);
        animation.setRegion(door.getCuboid());
    }

    /**
     * This method is called right before the animation is started and spawns the animated blocks.
     * <p>
     * Overriding methods should not forget to either call this method or spawn the animated blocks themselves.
     */
    protected void prepareAnimation()
    {
        if (!executor.isMainThread())
            throw new IllegalStateException("Animated blocks must be spawned on the main thread!");
        getAnimatedBlocks().forEach(IAnimatedBlock::spawn);
    }

    /**
     * Runs the animation of the animated blocks.
     */
    private synchronized void animateEntities(Animation<IAnimatedBlock> animation)
    {
        try
        {
            prepareAnimation();
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to prepare animation!");
            handleInitFailure();
            return;
        }

        forEachHook("onPrepare", IAnimationHook::onPrepare);

        final int stopCount = animationDuration + Math.max(0, movementMethod.finishDuration());
        moverTask = new TimerTask()
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
                    executeFinishingStep(counter, animation);

                animation.setStepsExecuted(counter);
                forEachHook("onPostAnimationStep", IAnimationHook::onPostAnimationStep);
            }
        };
        moverTaskID = executor.runAsyncRepeated(moverTask, 14, 1);
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

    private synchronized void putBlocks0(boolean onDisable)
    {
        if (!executor.isMainThread())
            throw new IllegalStateException("Attempting async block placement!");

        for (final IAnimatedBlock animatedBlock : privateAnimatedBlocks)
        {
            animatedBlock.kill();
            animatedBlock.getAnimatedBlockData().putBlock(animatedBlock.getFinalPosition());
        }

        // Tell the door object it has been opened and what its new coordinates are.
        updateCoords(door);

        privateAnimatedBlocks.clear();

        forEachHook("onAnimationCompleted", IAnimationHook::onAnimationCompleted);

        if (onDisable)
            return;

        doorActivityManager.processFinishedBlockMover(this, true);
    }

    /**
     * Places all the blocks of the door in their final position and kills all the animated blocks.
     * <p>
     * When the plugin is currently not in the process of disabling, it also schedules the auto close.
     *
     * @param onDisable
     *     Whether the plugin is currently being disabled.
     */
    public final void putBlocks(boolean onDisable)
    {
        // Only allow this method to be run once! If it can be run multiple times, it'll cause door corruption because
        // While the blocks have already been placed, the coordinates can still be toggled!
        if (isFinished.getAndSet(true))
            return;
        executor.runOnMainThread(() -> putBlocks0(onDisable));
    }

    /**
     * Updates the coordinates of a {@link AbstractDoor} and toggles its open status.
     *
     * @param door
     *     The {@link AbstractDoor}.
     */
    private synchronized void updateCoords(AbstractDoor door)
    {
        if (newCuboid.equals(door.getCuboid()))
            return;

        door.setCoordinates(newCuboid);

        door.setOpen(!door.isOpen());
        door.setCoordinates(newCuboid);
        door.syncData();
    }

    /**
     * Gets the UID of the {@link AbstractDoor} being moved.
     *
     * @return The UID of the {@link AbstractDoor} being moved.
     */
    public final long getDoorUID()
    {
        return door.getDoorUID();
    }

    /**
     * Gets the {@link AbstractDoor} being moved.
     *
     * @return The {@link AbstractDoor} being moved.
     */
    public final AbstractDoor getDoor()
    {
        return door;
    }

    private Cuboid getAnimationRegion()
    {
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double zMin = Double.MAX_VALUE;

        double xMax = Double.MIN_VALUE;
        double yMax = Double.MIN_VALUE;
        double zMax = Double.MIN_VALUE;

        for (final IAnimatedBlock animatedBlock : privateAnimatedBlocks)
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
        if (hooks == null)
            return;

        for (final IAnimationHook<IAnimatedBlock> hook : hooks)
        {
            log.at(Level.FINEST).log("Executing '%s' for hook '%s'!", actionName, hook.getName());
            try
            {
                call.accept(hook);
            }
            catch (Exception e)
            {
                log.at(Level.SEVERE).withCause(e)
                   .log("Failed to execute '%s' for hook '%s'!", actionName, hook.getName());
            }
        }
    }

    @Getter
    public static final class Context
    {
        private final DoorActivityManager doorActivityManager;
        private final AutoCloseScheduler autoCloseScheduler;
        private final IPLocationFactory locationFactory;
        private final IAudioPlayer audioPlayer;
        private final IPExecutor executor;
        private final IAnimatedBlockFactory animatedBlockFactory;
        private final AnimationHookManager animationHookManager;
        private final GlowingBlockSpawner glowingBlockSpawner;

        @Inject
        public Context(
            DoorActivityManager doorActivityManager, AutoCloseScheduler autoCloseScheduler,
            IPLocationFactory locationFactory, IAudioPlayer audioPlayer, IPExecutor executor,
            IAnimatedBlockFactory animatedBlockFactory, AnimationHookManager animationHookManager,
            GlowingBlockSpawner glowingBlockSpawner)
        {
            this.doorActivityManager = doorActivityManager;
            this.autoCloseScheduler = autoCloseScheduler;
            this.locationFactory = locationFactory;
            this.audioPlayer = audioPlayer;
            this.executor = executor;
            this.animatedBlockFactory = animatedBlockFactory;
            this.animationHookManager = animationHookManager;
            this.glowingBlockSpawner = glowingBlockSpawner;
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
        public static final MovementMethod VELOCITY = new MovementMethod("VELOCITY", 30)
        {
            @Override
            public void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos)
            {
                animatedBlock.setVelocity(goalPos.subtract(animatedBlock.getCurrentPosition()).multiply(0.101));
            }
        };

        /**
         * Teleports the animated blocks directly to their target positions.
         */
        public static final MovementMethod TELEPORT = new MovementMethod("TELEPORT", 2)
        {
            @Override
            public void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos)
            {
                animatedBlock.teleport(goalPos);
            }
        };

        /**
         * Combination of {@link #TELEPORT} and {@link #VELOCITY}.
         */
        public static final MovementMethod TELEPORT_VELOCITY = new MovementMethod("TELEPORT", 12)
        {
            @Override
            public void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos)
            {
                TELEPORT.apply(animatedBlock, goalPos);
                VELOCITY.apply(animatedBlock, goalPos);
            }
        };

        private final String name;
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
         * The duration (measured in ticks) of the final step executed after the animation has ended. This step is used
         * to move the animated blocks to their final positions gracefully.
         */
        public int finishDuration()
        {
            return finishDuration;
        }

        /**
         * Moves an animated block to a given goal position using the specified method.
         */
        abstract void apply(IAnimatedBlock animatedBlock, Vector3Dd goalPos);
    }
}
