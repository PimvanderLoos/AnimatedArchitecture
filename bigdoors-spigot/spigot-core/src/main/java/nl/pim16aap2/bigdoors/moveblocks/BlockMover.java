package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.IRestartable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a class that animates blocks.
 */
public abstract class BlockMover implements IRestartable
{
    protected final BigDoorsSpigot plugin;
    protected final IPWorld world;
    protected final DoorBase door;
    @Nullable
    protected final UUID playerUUID;
    protected final IFallingBlockFactory fallingBlockFactory;
    protected double time;
    protected boolean instantOpen;
    protected RotateDirection openDirection;
    protected List<PBlockData> savedBlocks;
    protected AtomicBoolean isAborted = new AtomicBoolean(false);
    protected PBlockFace currentDirection;
    protected int blocksMoved;
    protected int xMin, xMax, yMin;
    protected int yMax, zMin, zMax;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final Location finalMin, finalMax;
    protected final IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();
    protected final IPBlockDataFactory blockDataFactory = BigDoors.get().getPlatform().getPBlockDataFactory();


    /**
     * Constructs a {@link BlockMover}.
     *
     * @param door             The {@link DoorBase}.
     * @param time             The amount of time (in seconds) the door will try to toggle itself in.
     * @param instantOpen      If the door should be opened instantly (i.e. skip animation) or not.
     * @param currentDirection The current direction of the door.
     * @param openDirection    The direction the {@link DoorBase} will move.
     * @param blocksMoved      The number of blocks the {@link DoorBase} will move.
     * @param playerUUID       The {@link UUID} of the player who opened this door.
     * @param finalMin         The resulting minimum coordinates.
     * @param finalMax         The resulting maximum coordinates.
     */
    protected BlockMover(final @NotNull DoorBase door, final double time, final boolean instantOpen,
                         final @NotNull PBlockFace currentDirection, final @NotNull RotateDirection openDirection,
                         final int blocksMoved, @Nullable final UUID playerUUID, final @NotNull Location finalMin,
                         final @NotNull Location finalMax)
    {
        plugin = BigDoorsSpigot.get();
        plugin.getAutoCloseScheduler().unscheduleAutoClose(door.getDoorUID());
        world = BigDoors.get().getPlatform().getPWorldFactory().create(door.getWorld().getUID());
        this.door = door;
        this.time = time;
        this.instantOpen = instantOpen;
        this.currentDirection = currentDirection;
        this.openDirection = openDirection;
        this.blocksMoved = blocksMoved;
        this.playerUUID = playerUUID;
        fallingBlockFactory = plugin.getFABF();
        savedBlocks = new ArrayList<>();
        this.finalMin = finalMin;
        this.finalMax = finalMax;

        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();
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
     * Aborts the animation.
     */
    public void abort()
    {
        isAborted.set(true);
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
     * Replaces all blocks of the {@link DoorBase} by Falling Blocks.
     */
    protected void constructFBlocks()
    {
        for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
            for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    blockDataFactory.create(locationFactory.create(world, xAxis, yAxis, zAxis), (yAxis == yMin),
                                            getRadius(xAxis, yAxis, zAxis), getStartAngle(xAxis, yAxis, zAxis))
                                    .ifPresent(savedBlocks::add);

        for (PBlockData mbd : savedBlocks)
            mbd.getBlock().deleteOriginalBlock();

        if (instantOpen)
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
            int delay = Math.max(Constants.MINIMUMDOORDELAY, plugin.getConfigLoader().coolDown() * 20);
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    plugin.getDoorManager().setDoorAvailable(door.getDoorUID());
                    plugin.getAutoCloseScheduler().scheduleAutoClose(playerUUID, door, time, instantOpen);
                }
            }.runTaskLater(plugin, delay);
        }
    }

    /**
     * Updates the coordinates of a {@link DoorBase} and toggles its open status.
     *
     * @param door The {@link DoorBase}.
     */
    private void updateCoords(final @NotNull DoorBase door)
    {
        if (finalMin.equals(door.getMinimum()) && finalMax.equals(door.getMaximum()))
            return;

        door.setMinimum(finalMin);
        door.setMaximum(finalMax);

        toggleOpen(door);
        plugin.getDatabaseManager().updateDoorCoords(door.getDoorUID(), door.isOpen(), finalMin.getBlockX(),
                                                     finalMin.getBlockY(), finalMin.getBlockZ(), finalMax.getBlockX(),
                                                     finalMax.getBlockY(), finalMax.getBlockZ());
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
     * Toggles the open status of a {@link DoorBase}.
     *
     * @param door The {@link DoorBase}.
     */
    private void toggleOpen(DoorBase door)
    {
        door.setOpenStatus(!door.isOpen());
    }

//    /**
//     * Constructs a new {@link ICustomCraftFallingBlock}.
//     *
//     * @param loc   The {@link IPLocation} where the {@link ICustomCraftFallingBlock} will be created.
//     * @param block The block of the {@link ICustomCraftFallingBlock}.
//     * @return A new {@link ICustomCraftFallingBlock}.
//     */
//    @NotNull
//    protected final ICustomCraftFallingBlock fallingBlockFactory(final @NotNull IPLocation loc,
//                                                                 final @NotNull INMSBlock block)
//    {
//        ICustomCraftFallingBlock entity = fabf.fallingBlockFactory(loc, block);
//        Entity bukkitEntity = (Entity) entity;
//        bukkitEntity.setCustomName("BigDoorsEntity");
//        bukkitEntity.setCustomNameVisible(false);
//        return entity;
//    }

    /**
     * Gets the UID of the {@link DoorBase} being moved.
     *
     * @return The UID of the {@link DoorBase} being moved.
     */
    public final long getDoorUID()
    {
        return door.getDoorUID();
    }

    /**
     * Gets the {@link DoorBase} being moved.
     *
     * @return The {@link DoorBase} being moved.
     */
    public final DoorBase getDoor()
    {
        return door;
    }
}
