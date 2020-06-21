package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a class that animates blocks.
 *
 * @author Pim
 */
public abstract class BlockMover implements IRestartable
{
    protected final IPWorld world;
    protected final AbstractDoorBase door;
    @NotNull
    protected final IPPlayer player;
    @NotNull
    final DoorActionCause cause;
    @NotNull
    final DoorActionType actionType;
    protected final IFallingBlockFactory fallingBlockFactory;
    protected double time;
    protected boolean skipAnimation;
    protected RotateDirection openDirection;
    protected List<PBlockData> savedBlocks;
    protected PBlockFace currentDirection;
    protected int blocksMoved;
    protected int xMin, xMax, yMin;
    protected int yMax, zMin, zMax;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final Vector3Di finalMin, finalMax;
    protected final IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();
    protected final IPBlockDataFactory blockDataFactory = BigDoors.get().getPlatform().getPBlockDataFactory();
    @Nullable
    protected TimerTask moverTask = null;
    protected int moverTaskID = 0;

    /**
     * The tick at which to stop the animation.
     */
    protected int endCount = 0;

    /**
     * The tickrate of the animation.
     */
    protected int tickRate = 1;

    /**
     * The sound to play while the animation is active.
     */
    @Nullable
    protected PSoundDescription soundActive = null;

    /**
     * The sound to play upon finishing the animation.
     */
    @Nullable
    protected PSoundDescription soundFinish = null;

    /**
     * The amount of time (measured in ticks) between the "prepareToEndAnimation" phase (where blocks are moved to their
     * final position while in their animated state.
     */
    protected int stopDelay = 50;


    /**
     * Constructs a {@link BlockMover}.
     *
     * @param door             The {@link AbstractDoorBase}.
     * @param time             The amount of time (in seconds) the door will try to toggle itself in.
     * @param skipAnimation    If the door should be opened instantly (i.e. skip animation) or not.
     * @param currentDirection The current direction of the door.
     * @param openDirection    The direction the {@link AbstractDoorBase} will move.
     * @param blocksMoved      The number of blocks the {@link AbstractDoorBase} will move.
     * @param player           The player who opened this door.
     * @param finalMin         The resulting minimum coordinates.
     * @param finalMax         The resulting maximum coordinates.
     */
    protected BlockMover(final @NotNull AbstractDoorBase door, final double time, final boolean skipAnimation,
                         final @NotNull PBlockFace currentDirection, final @NotNull RotateDirection openDirection,
                         final int blocksMoved, final @NotNull IPPlayer player, final @NotNull Vector3Di finalMin,
                         final @NotNull Vector3Di finalMax, final @NotNull DoorActionCause cause,
                         final @NotNull DoorActionType actionType)
    {
        BigDoors.get().getAutoCloseScheduler().unscheduleAutoClose(door.getDoorUID());
        world = door.getWorld();
        this.door = door;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.currentDirection = currentDirection;
        this.openDirection = openDirection;
        this.blocksMoved = blocksMoved;
        this.player = player;
        fallingBlockFactory = BigDoors.get().getPlatform().getFallingBlockFactory();
        savedBlocks = new ArrayList<>();
        this.finalMin = finalMin;
        this.finalMax = finalMax;
        this.cause = cause;
        this.actionType = actionType;

        xMin = door.getMinimum().getX();
        yMin = door.getMinimum().getY();
        zMin = door.getMinimum().getZ();
        xMax = door.getMaximum().getX();
        yMax = door.getMaximum().getY();
        zMax = door.getMaximum().getZ();
    }

    /**
     * Plays a sound at the engine of a door.
     *
     * @param soundDescription The {@link PSoundDescription} containing all the properties of the sound to play.
     */
    protected void playSound(final @NotNull PSoundDescription soundDescription)
    {
        BigDoors.get().getPlatform().getSoundEngine()
                .playSound(door.getEngine(), door.getWorld(), soundDescription.getSound(), soundDescription.getVolume(),
                           soundDescription.getPitch());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        abort();
    }

    /**
     * {@inheritDoc}
     */
    public void abort()
    {
        if (moverTask != null)
            BigDoors.get().getPlatform().newPExecutor().cancel(moverTask, moverTaskID);
        putBlocks(true);
    }

    /**
     * Respawns a {@link ICustomCraftFallingBlock}.
     *
     * @param blockData The {@link PBlockData} containing the {@link ICustomCraftFallingBlock} that will be respawned.
     * @param newBlock  The new {@link INMSBlock} to use for the {@link ICustomCraftFallingBlock}.
     */
    private void respawnBlock(final @NotNull PBlockData blockData, final @NotNull INMSBlock newBlock)
    {
        final IPLocation loc = blockData.getFBlock().getPLocation();
        final Vector3Dd veloc = blockData.getFBlock().getPVelocity();

        final ICustomCraftFallingBlock fBlock = fallingBlockFactory.fallingBlockFactory(loc, newBlock);
        blockData.getFBlock().remove();
        blockData.setFBlock(fBlock);

        blockData.getFBlock().setVelocity(veloc);
    }

    /**
     * Rotates (in the {@link #openDirection} and then respawns a {@link ICustomCraftFallingBlock} of a {@link
     * PBlockData}. Note that this is executed on the thread it was called from, which MUST BE the main thread!
     */
    private void applyRotationOnCurrentThread()
    {
        for (final PBlockData blockData : savedBlocks)
            if (blockData.isRotatable())
            {
                final INMSBlock newBlock = blockData.getBlock();
                newBlock.rotateBlock(openDirection);
                respawnBlock(blockData, newBlock);
            }
    }

    /**
     * Rotates (in the {@link #openDirection} and then respawns a {@link ICustomCraftFallingBlock} of a {@link
     * PBlockData}. This is executed on the main thread.
     */
    protected void applyRotation()
    {
        BigDoors.get().getPlatform().newPExecutor().runSync(this::applyRotationOnCurrentThread);
    }

    /**
     * Respawns all blocks. Note that this is executed on the thread it was called from, which MUST BE the main thread!
     */
    private void respawnBlocksOnCurrentThread()
    {
        for (final PBlockData blockData : savedBlocks)
            respawnBlock(blockData, blockData.getBlock());
    }

    /**
     * Respawns all blocks. This is executed on the main thread.
     */
    protected void respawnBlocks()
    {
        BigDoors.get().getPlatform().newPExecutor().runSync(this::respawnBlocksOnCurrentThread);
    }

    /**
     * Replaces all blocks of the {@link AbstractDoorBase} by Falling Blocks and starts the animation.
     * <p>
     * Note that if {@link #skipAnimation} is true, the blocks will be placed in the new position immediately without
     * any animations.
     */
    protected void startAnimation()
    {
        for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
            for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    blockDataFactory
                        .create(locationFactory.create(world, xAxis + 0.5, yAxis, zAxis + 0.5), (yAxis == yMin),
                                getRadius(xAxis, yAxis, zAxis), getStartAngle(xAxis, yAxis, zAxis))
                        .ifPresent(savedBlocks::add);

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
     * @param block The {@link PBlockData}.
     * @return The final position of a {@link PBlockData}.
     */
    protected abstract Vector3Dd getFinalPosition(final @NotNull PBlockData block);

    /**
     * Runs a single step of the animation.
     *
     * @param ticks The number of ticks that have passed since the start of the animation.
     */
    protected abstract void executeAnimationStep(final int ticks);

    /**
     * Gracefully stops the animation: Freeze any animated blocks, kill the animation task and place the blocks in their
     * new location.
     */
    private void stopAnimation()
    {
        if (soundFinish != null)
            playSound(soundFinish);

        for (final PBlockData savedBlock : savedBlocks)
            savedBlock.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

        final @NotNull IPExecutor<Object> executor = BigDoors.get().getPlatform().newPExecutor();
        executor.runSync(() -> putBlocks(false));
        executor.cancel(moverTask, moverTaskID);
    }

    /**
     * Executes the final step of the animation: Move all blocks to their final positions while still in their animated
     * state.
     */
    protected void prepareToEndAnimation()
    {
        // Move the blocks to their final positions.
        // TODO: Store the final location in the block or something.
        for (final PBlockData block : savedBlocks)
        {
            Vector3Dd vec = getFinalPosition(block).subtract(block.getFBlock().getPosition());
            block.getFBlock().setVelocity(vec.multiply(0.101));
        }
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
    private void animateEntities()
    {
        prepareAnimation();

        moverTask = new TimerTask()
        {
            int counter = 0;
            Long startTime = null; // Initialize on the first run.
            long lastTime;
            long currentTime = System.nanoTime();

            @Override
            public void run()
            {
                if (startTime == null)
                    startTime = System.nanoTime();
                ++counter;

                if (soundActive != null && counter % PSound.getDuration(soundActive.getSound()) == 0)
                    playSound(soundActive);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                // After about 12620 ticks, the blocks will disappear.
                // Respawning them before this happens, fixes the issue.
                // TODO: Check if just resetting the tick value of the blocks works as well.
                if (counter % 12500 == 0)
                    respawnBlocks();

                if (counter > (endCount + stopDelay))
                    stopAnimation();
                else if (counter > endCount)
                    prepareToEndAnimation();
                else if (counter % tickRate == 0)
                    executeAnimationStep(counter);
            }
        };
        moverTaskID = BigDoors.get().getPlatform().newPExecutor().runAsyncRepeated(moverTask, 14, 1);
    }

    /**
     * Gets the radius of a block at the given coordinates.
     *
     * @param xAxis The x coordinate.
     * @param yAxis The y coordinate.
     * @param zAxis The z coordinate.
     * @return The radius of a block at the given coordinates.
     */
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        return -1;
    }

    /**
     * Gets the starting angle of a block (in rads) at the given coordinates.
     *
     * @param xAxis The x coordinate.
     * @param yAxis The y coordinate.
     * @param zAxis The z coordinate.
     * @return The starting angle of a block at the given coordinates.
     */
    protected float getStartAngle(final int xAxis, final int yAxis, final int zAxis)
    {
        return -1;
    }

    /**
     * Places the block of a {@link PBlockData}.
     *
     * @param pBlockData The {@link PBlockData}.
     * @param firstPass  Whether or not this is the first pass. See {@link PBlockData#isPlacementDeferred()};
     */
    private void putSavedBlock(final @NotNull PBlockData pBlockData, final boolean firstPass)
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
     * @param onDisable Whether or not the plugin is currently being disabled.
     */
    public final void putBlocks(final boolean onDisable)
    {
        // Only allow this method to be run once! If it can be run multiple times, it'll cause door corruption because
        // While the blocks have already been placed, the coordinates can still be toggled!
        if (isFinished.get())
            return;
        isFinished.set(true);

        // First do the first pass, placing all blocks such as stone, dirt, etc.
        for (PBlockData savedBlock : savedBlocks)
            putSavedBlock(savedBlock, true);

        // Then do the second pass, placing all blocks such as torches, etc.
        for (PBlockData savedBlock : savedBlocks)
            putSavedBlock(savedBlock, false);

        // Tell the door object it has been opened and what its new coordinates are.
        updateCoords(door);

        savedBlocks.clear();

        BigDoors.get().getPlatform().callDoorActionEvent(BigDoors.get().getPlatform().getDoorActionEventFactory()
                                                                 .createEndEvent(door, cause, actionType, player,
                                                                                 time, skipAnimation));

        if (!onDisable)
        {
            int delay = Math
                .max(Constants.MINIMUMDOORDELAY, BigDoors.get().getPlatform().getConfigLoader().coolDown() * 20);
            BigDoors.get().getPlatform().newPExecutor().runSyncLater(
                new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        BigDoors.get().getDoorManager().setDoorAvailable(door.getDoorUID());

                        if (door instanceof ITimerToggleableArchetype)
                            BigDoors.get().getAutoCloseScheduler()
                                    .scheduleAutoClose(player, (AbstractDoorBase & ITimerToggleableArchetype) door,
                                                       time, skipAnimation);
                    }
                }, delay);
        }
    }

    /**
     * Updates the coordinates of a {@link AbstractDoorBase} and toggles its open status.
     *
     * @param door The {@link AbstractDoorBase}.
     */
    private void updateCoords(final @NotNull AbstractDoorBase door)
    {
        if (finalMin.equals(door.getMinimum()) && finalMax.equals(door.getMaximum()))
            return;

        door.setMinimum(finalMin);
        door.setMaximum(finalMax);

        toggleOpen(door);
        BigDoors.get().getDatabaseManager()
                .updateDoorCoords(door.getDoorUID(), door.isOpen(), finalMin.getX(), finalMin.getY(),
                                  finalMin.getZ(),
                                  finalMax.getX(), finalMax.getY(), finalMax.getZ());
    }

    /**
     * Gets the new location of a block from its old coordinates.
     *
     * @param radius The radius of the block.
     * @param xAxis  The old x-coordinate of the block.
     * @param yAxis  The old y-coordinate of the block.
     * @param zAxis  The old z-coordinate of the block.
     * @return The new Location of the block.
     */
    protected abstract IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis);

    /**
     * Toggles the open status of a {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase}.
     */
    private void toggleOpen(AbstractDoorBase door)
    {
        door.setOpen(!door.isOpen());
    }

    /**
     * Gets the UID of the {@link AbstractDoorBase} being moved.
     *
     * @return The UID of the {@link AbstractDoorBase} being moved.
     */
    public final long getDoorUID()
    {
        return door.getDoorUID();
    }

    /**
     * Gets the {@link AbstractDoorBase} being moved.
     *
     * @return The {@link AbstractDoorBase} being moved.
     */
    public final AbstractDoorBase getDoor()
    {
        return door;
    }
}
