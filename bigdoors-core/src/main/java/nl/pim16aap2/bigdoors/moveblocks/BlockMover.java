package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
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
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PBlockFace;
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
                         final @NotNull Vector3Di finalMax)
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
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    protected void playSound(final @NotNull PSound sound, final float volume, final float pitch)
    {
        BigDoors.get().getPlatform().getSoundEngine()
                .playSound(door.getEngine(), door.getWorld(), sound, volume, pitch);
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
            moverTask.cancel();
        putBlocks(true);
    }

    /**
     * Rotates (in the {@link #openDirection} and then respawns a {@link ICustomCraftFallingBlock} of a {@link
     * PBlockData}.
     */
    protected void respawnBlocks()
    {
        for (PBlockData block : savedBlocks)
            if (block.canRot())
            {
                IPLocation loc = block.getFBlock().getPLocation();
                Vector3Dd veloc = block.getFBlock().getPVelocity();

                ICustomCraftFallingBlock fBlock;
                // Because the block in savedBlocks is already rotated where applicable, just
                // use that block now.
                INMSBlock newBlock = block.getBlock();
                newBlock.rotateBlock(openDirection);
                fBlock = fallingBlockFactory.fallingBlockFactory(loc, newBlock);

                block.getFBlock().remove();
                block.setFBlock(fBlock);

                block.getFBlock().setVelocity(veloc);
            }
    }

    /**
     * Replaces all blocks of the {@link AbstractDoorBase} by Falling Blocks.
     */
    protected void constructFBlocks()
    {
        for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
            for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    blockDataFactory
                        .create(locationFactory.create(world, xAxis + 0.5, yAxis, zAxis + 0.5), (yAxis == yMin),
                                getRadius(xAxis, yAxis, zAxis), getStartAngle(xAxis, yAxis, zAxis))
                        .ifPresent(savedBlocks::add);

        for (PBlockData mbd : savedBlocks)
            mbd.getBlock().deleteOriginalBlock();

        if (skipAnimation)
            putBlocks(false);
        else
            animateEntities();
    }

    /**
     * Runs the animation of the animated blocks.
     */
    protected abstract void animateEntities();

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
     * @param firstPass  Whether or not this is the first pass. See {@link PBlockData#deferPlacement()};
     */
    private void putSavedBlock(final @NotNull PBlockData pBlockData, final boolean firstPass)
    {
        if (pBlockData.deferPlacement() && firstPass)
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
                        BigDoors.get().getAutoCloseScheduler().scheduleAutoClose(player, door, time, skipAnimation);
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
        door.setOpenStatus(!door.isOpen());
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
