package nl.pim16aap2.bigdoors.moveblocks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Represents a class that animates blocks.
 *
 * @author Pim
 */
@ToString
@Flogger
public abstract class BlockMover implements IRestartable
{
    protected final IPWorld world;

    protected final AbstractDoor door;

    @Getter
    protected final IPPlayer player;

    @Getter
    private final DoorActionCause cause;

    @Getter
    private final DoorActionType actionType;

    @ToString.Exclude
    protected final IFallingBlockFactory fallingBlockFactory;

    @ToString.Exclude
    protected final DoorActivityManager doorActivityManager;

    @ToString.Exclude
    protected final AutoCloseScheduler autoCloseScheduler;

    @ToString.Exclude
    protected final IPExecutor executor;

    @Getter
    protected double time;

    @Getter
    protected boolean skipAnimation;

    protected RotateDirection openDirection;

    @ToString.Exclude
    protected List<PBlockData> savedBlocks;

    protected int xMin;

    protected int yMin;

    protected int zMin;

    protected int xMax;

    protected int yMax;

    protected int zMax;

    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    protected final IPLocationFactory locationFactory;

    protected final IPBlockDataFactory blockDataFactory;

    protected @Nullable TimerTask moverTask = null;

    protected int moverTaskID = 0;

    /**
     * The tick at which to stop the animation.
     */
    protected int endCount = 0;

    /**
     * The sound to play while the animation is active.
     */
    protected @Nullable PSoundDescription soundActive = null;

    /**
     * The sound to play upon finishing the animation.
     */
    protected @Nullable PSoundDescription soundFinish = null;

    protected final Cuboid newCuboid;
    private final ISoundEngine soundEngine;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param time
     *     The amount of time (in seconds) the door will try to toggle itself in.
     * @param skipAnimation
     *     If the door should be opened instantly (i.e. skip animation) or not.
     * @param openDirection
     *     The direction the {@link AbstractDoor} will move.
     * @param player
     *     The player who opened this door.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     */
    protected BlockMover(Context context, AbstractDoor door, double time, boolean skipAnimation,
                         RotateDirection openDirection, IPPlayer player, Cuboid newCuboid,
                         DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        executor = context.getExecutor();
        doorActivityManager = context.getDoorActivityManager();
        autoCloseScheduler = context.getAutoCloseScheduler();
        blockDataFactory = context.getBlockDataFactory();
        locationFactory = context.getLocationFactory();
        fallingBlockFactory = context.getFallingBlockFactory();
        soundEngine = context.getSoundEngine();

        if (!context.getExecutor().isMainThread(Thread.currentThread().getId()))
            throw new Exception("BlockMovers must be called on the main thread!");

        autoCloseScheduler.unscheduleAutoClose(door.getDoorUID());
        world = door.getWorld();
        this.door = door;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.openDirection = openDirection;
        this.player = player;
        savedBlocks = new ArrayList<>();
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

    /**
     * Plays a sound at the rotation point of a door.
     *
     * @param soundDescription
     *     The {@link PSoundDescription} containing all the properties of the sound to play.
     */
    protected void playSound(PSoundDescription soundDescription)
    {
        soundEngine.playSound(door.getRotationPoint(), door.getWorld(), soundDescription.sound(),
                              soundDescription.volume(), soundDescription.pitch());
    }

    @Override
    public void restart()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        abort();
    }

    public void abort()
    {
        if (moverTask != null)
            executor.cancel(moverTask, moverTaskID);
        putBlocks(true);
    }

    /**
     * Respawns a {@link ICustomCraftFallingBlock}.
     *
     * @param blockData
     *     The {@link PBlockData} containing the {@link ICustomCraftFallingBlock} that will be respawned.
     * @param newBlock
     *     The new {@link INMSBlock} to use for the {@link ICustomCraftFallingBlock}.
     * @return True if respawning was successful.
     */
    private boolean respawnBlock(PBlockData blockData, INMSBlock newBlock)
    {
        final IPLocation loc = blockData.getFBlock().getPosition().toLocation(locationFactory, world);
        final Vector3Dd pVelocity = blockData.getFBlock().getPVelocity();

        try
        {
            final var fBlock = fallingBlockFactory.fallingBlockFactory(loc, newBlock);
            blockData.getFBlock().remove();
            blockData.setFBlock(fBlock);

            blockData.getFBlock().setVelocity(pVelocity);
            return true;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            return false;
        }
    }

    /**
     * Rotates in the {@link #openDirection} and then respawns a {@link ICustomCraftFallingBlock} of a {@link
     * PBlockData}. Note that this is executed on the thread it was called from, which MUST BE the main thread!
     */
    private void applyRotationOnCurrentThread()
    {
        final ListIterator<PBlockData> blockDataIterator = savedBlocks.listIterator();
        while (blockDataIterator.hasNext())
        {
            final var blockData = blockDataIterator.next();
            final INMSBlock newBlock = blockData.getBlock();
            newBlock.rotateBlock(openDirection);
            if (!respawnBlock(blockData, newBlock))
                blockDataIterator.remove();
        }
    }

    /**
     * Rotates in the {@link #openDirection} and then respawns a {@link ICustomCraftFallingBlock} of a {@link
     * PBlockData}. This is executed on the main thread.
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
        savedBlocks.removeIf(blockData -> !respawnBlock(blockData, blockData.getBlock()));
    }

    /**
     * Respawns all blocks. This is executed on the main thread.
     */
    protected void respawnBlocks()
    {
        executor.runSync(this::respawnBlocksOnCurrentThread);
    }

    /**
     * Replaces all blocks of the {@link AbstractDoor} by Falling Blocks and starts the animation.
     * <p>
     * Note that if {@link #skipAnimation} is true, the blocks will be placed in the new position immediately without
     * any animations.
     */
    protected synchronized void startAnimation()
    {
        try
        {
            for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                    for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                        blockDataFactory
                            .create(locationFactory.create(world, xAxis + 0.5, yAxis, zAxis + 0.5), (yAxis == yMin),
                                    getRadius(xAxis, yAxis, zAxis), getStartAngle(xAxis, yAxis, zAxis))
                            .ifPresent(savedBlocks::add);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            doorActivityManager.processFinishedBlockMover(this, false);
            return;
        }

        for (final PBlockData mbd : savedBlocks)
            mbd.getBlock().deleteOriginalBlock();

        if (skipAnimation || savedBlocks.isEmpty())
            putBlocks(false);
        else
            animateEntities();
    }

    /**
     * Gets the final position of a {@link PBlockData}.
     *
     * @param block
     *     The {@link PBlockData}.
     * @return The final position of a {@link PBlockData}.
     */
    @SuppressWarnings("unused")
    protected abstract Vector3Dd getFinalPosition(PBlockData block);

    /**
     * Runs a single step of the animation.
     *
     * @param ticks
     *     The number of ticks that have passed since the start of the animation.
     */
    protected abstract void executeAnimationStep(int ticks);

    /**
     * Gracefully stops the animation: Freeze any animated blocks, kill the animation task and place the blocks in their
     * new location.
     */
    private synchronized void stopAnimation()
    {
        if (soundFinish != null)
            playSound(soundFinish);

        for (final PBlockData savedBlock : savedBlocks)
            savedBlock.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

        executor.runSync(() -> putBlocks(false));
        if (moverTask == null)
        {
            log.at(Level.WARNING).log("MoverTask unexpectedly null for BlockMover:\n%s", this);
            return;
        }
        executor.cancel(moverTask, moverTaskID);
    }

    /**
     * This method is called right before the animation is started (and after all variables have been initialized).
     * <p>
     * It does not do anything by default.
     */
    protected void prepareAnimation()
    {
    }

    /**
     * Runs the animation of the animated blocks.
     */
    private synchronized void animateEntities()
    {
        prepareAnimation();

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
                ++counter;

                if (soundActive != null && counter % PSound.getDuration(soundActive.sound()) == 0)
                    playSound(soundActive);

                final long lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                // After about 12620 ticks, the blocks will disappear.
                // Respawning them before this happens, fixes the issue.
                // TODO: Check if just resetting the tick value of the blocks works as well.
                if (counter % 12_500 == 0)
                    respawnBlocks();

                if (counter > endCount)
                    stopAnimation();
                else
                    executeAnimationStep(counter);
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

    /**
     * Places the block of a {@link PBlockData}.
     *
     * @param pBlockData
     *     The {@link PBlockData}.
     * @param firstPass
     *     Whether this is the first pass. See {@link PBlockData#isPlacementDeferred()};
     */
    private void putSavedBlock(PBlockData pBlockData, boolean firstPass)
    {
        if (pBlockData.isPlacementDeferred() && firstPass)
            return;

        pBlockData.killFBlock();
        pBlockData.getBlock().putBlock(getNewLocation(pBlockData.getRadius(), pBlockData.getStartX(),
                                                      pBlockData.getStartY(), pBlockData.getStartZ()));
    }

    /**
     * Places all the blocks of the door in their final position and kills all the animated blocks.
     * <p>
     * When the plugin is currently not in the process of disabling, it also schedules the auto close.
     *
     * @param onDisable
     *     Whether the plugin is currently being disabled.
     */
    public final synchronized void putBlocks(boolean onDisable)
    {
        // Only allow this method to be run once! If it can be run multiple times, it'll cause door corruption because
        // While the blocks have already been placed, the coordinates can still be toggled!
        if (isFinished.getAndSet(true))
            return;

        // First do the first pass, placing all blocks such as stone, dirt, etc.
        for (final PBlockData savedBlock : savedBlocks)
            putSavedBlock(savedBlock, true);

        // Then do the second pass, placing all blocks such as torches, etc.
        for (final PBlockData savedBlock : savedBlocks)
            putSavedBlock(savedBlock, false);

        // Tell the door object it has been opened and what its new coordinates are.
        updateCoords(door);

        savedBlocks.clear();

        if (onDisable)
            return;

        doorActivityManager.processFinishedBlockMover(this, true);
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
     * Gets the new location of a block from its old coordinates.
     *
     * @param radius
     *     The radius of the block.
     * @param xAxis
     *     The old x-coordinate of the block.
     * @param yAxis
     *     The old y-coordinate of the block.
     * @param zAxis
     *     The old z-coordinate of the block.
     * @return The new Location of the block.
     */
    protected abstract IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis);

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

    @Getter(AccessLevel.PACKAGE)
    public static final class Context
    {
        private final DoorActivityManager doorActivityManager;
        private final AutoCloseScheduler autoCloseScheduler;
        private final IPLocationFactory locationFactory;
        private final IPBlockDataFactory blockDataFactory;
        private final ISoundEngine soundEngine;
        private final IPExecutor executor;
        private final IFallingBlockFactory fallingBlockFactory;

        @Inject
        public Context(DoorActivityManager doorActivityManager, AutoCloseScheduler autoCloseScheduler,
                       IPLocationFactory locationFactory, IPBlockDataFactory blockDataFactory, ISoundEngine soundEngine,
                       IPExecutor executor, IFallingBlockFactory fallingBlockFactory)
        {
            this.doorActivityManager = doorActivityManager;
            this.autoCloseScheduler = autoCloseScheduler;
            this.locationFactory = locationFactory;
            this.blockDataFactory = blockDataFactory;
            this.soundEngine = soundEngine;
            this.executor = executor;
            this.fallingBlockFactory = fallingBlockFactory;
        }
    }
}
