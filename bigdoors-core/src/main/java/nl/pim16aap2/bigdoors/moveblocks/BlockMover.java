package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.IRestartable;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
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
    protected final BigDoors plugin;
    protected final World world;
    protected final DoorBase door;
    @Nullable
    protected final UUID playerUUID;
    protected final IFallingBlockFactory fabf;
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

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param plugin           The {@link BigDoors}.
     * @param world            The {@link World} in which the blocks will be moved.
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
    protected BlockMover(final @NotNull BigDoors plugin, final @NotNull World world, final @NotNull DoorBase door,
                         final double time, final boolean instantOpen, final @NotNull PBlockFace currentDirection,
                         final @NotNull RotateDirection openDirection, final int blocksMoved,
                         @Nullable final UUID playerUUID, final @Nullable Location finalMin,
                         final @Nullable Location finalMax)
    {
        plugin.getAutoCloseScheduler().unscheduleAutoClose(door.getDoorUID());
        this.plugin = plugin;
        this.world = world;
        this.door = door;
        this.time = time;
        this.instantOpen = instantOpen;
        this.currentDirection = currentDirection;
        this.openDirection = openDirection;
        this.blocksMoved = blocksMoved;
        this.playerUUID = playerUUID;
        fabf = plugin.getFABF();
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
     * Constructs a {@link BlockMover}.
     *
     * @param plugin           The {@link BigDoors}.
     * @param world            The {@link World} in which the blocks will be moved.
     * @param door             The {@link DoorBase}.
     * @param time             The amount of time (in seconds) the door will try to toggle itself in.
     * @param instantOpen      If the door should be opened instantly (i.e. skip animation) or not.
     * @param currentDirection The current direction of the door.
     * @param openDirection    The direction the {@link DoorBase} will move.
     * @param blocksMoved      The number of blocks the {@link DoorBase} will move.
     * @param playerUUID       The {@link UUID} of the player who opened this door.
     * @deprecated All BlockMovers should use {@link #BlockMover(BigDoors, World, DoorBase, double, boolean, PBlockFace,
     * RotateDirection, int, UUID, Location, Location)} instead. That constructor accepts the final locations so they
     * don't have to be calculated more than once.
     */
    @Deprecated
    protected BlockMover(final @NotNull BigDoors plugin, final @NotNull World world, final @NotNull DoorBase door,
                         final double time, final boolean instantOpen, final @NotNull PBlockFace currentDirection,
                         final @NotNull RotateDirection openDirection, final int blocksMoved,
                         @Nullable final UUID playerUUID)
    {
        this(plugin, world, door, time, instantOpen, currentDirection, openDirection, blocksMoved, playerUUID, null,
             null);
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
     * Replaces all blocks of the {@link DoorBase} by Falling Blocks.
     */
    protected void constructFBlocks()
    {
        for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
            for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Block vBlock = world.getBlockAt(xAxis, yAxis, zAxis);

                    if (SpigotUtil.isAllowedBlock(vBlock))
                    {
                        Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
                        // Move the lowest blocks up a little, so the client won't predict they're
                        // touching through the ground, which would make them slower than the rest.
                        if (yAxis == yMin)
                            newFBlockLocation.setY(newFBlockLocation.getY() + .010001);

                        INMSBlock block = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                        INMSBlock block2 = null;
                        boolean canRotate = false;

                        if (openDirection != null && !openDirection.equals(RotateDirection.NONE) && block.canRotate())
                        {
                            block2 = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                            block2.rotateBlock(openDirection);
                            canRotate = true;
                        }

                        float radius = getRadius(xAxis, yAxis, zAxis);
                        float startAngle = getStartAngle(xAxis, yAxis, zAxis);

//                        Bukkit.broadcastMessage("Block at: " + SpigotUtil.locIntToString(startLocation) + " (" +
//                                                    vBlock.getType().toString() + "). Radius: " + radius +
//                                                    ", startAngle: " + startAngle);

                        ICustomCraftFallingBlock fBlock = instantOpen ? null :
                                                          fallingBlockFactory(newFBlockLocation, block);
                        savedBlocks.add(new PBlockData(fBlock, radius, block2 == null ? block : block2, canRotate,
                                                       startLocation, startAngle));
                    }
                }
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
     * Gets the starting angle of a block at the given coordinates.
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

        removeSolidBlocks();
        for (PBlockData savedBlock : savedBlocks)
        {
            Location newPos = getNewLocation(savedBlock.getRadius(), savedBlock.getStartX(), savedBlock.getStartY(),
                                             savedBlock.getStartZ());
//            Location newPos = savedBlock.getStartLocation();
            savedBlock.killFBlock();
            savedBlock.getBlock().putBlock(newPos);
            Block b = world.getBlockAt(newPos);
            BlockState bs = b.getState();
            bs.update();
        }

        // Tell the door object it has been opened and what its new coordinates are.
        updateCoords(door, currentDirection, openDirection, blocksMoved);

        savedBlocks.clear();

        if (!onDisable)
        {
            int delay = Math.max(plugin.getMinimumDoorDelay(), plugin.getConfigLoader().coolDown() * 20);
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

    private Vector3D oldMin = null;
    private Vector3D oldMax = null;

    /**
     * Kills all barrier blocks.
     */
    protected final void removeSolidBlocks()
    {
        if (oldMin == null || oldMax == null)
            return;

        for (int x = oldMin.getX(); x <= oldMax.getX(); ++x)
            for (int y = oldMin.getY(); y <= oldMax.getY(); ++y)
                for (int z = oldMin.getZ(); z <= oldMax.getZ(); ++z)
                {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().equals(Material.BARRIER))
                        block.setType(Material.AIR);
                }
    }

    /**
     * Updates all the barriers from the old locations to the new locations.
     *
     * @param newMin New minimum coordinates.
     * @param newMax New maximum coordinates.
     */
    protected final void updateSolidBlocks(final @NotNull Vector3D newMin, final @NotNull Vector3D newMax)
    {
        if (newMin.equals(oldMin) && newMax.equals(oldMax))
            return;

        removeSolidBlocks();
        for (int x = newMin.getX(); x <= newMax.getX(); ++x)
            for (int y = newMin.getY(); y <= newMax.getY(); ++y)
                for (int z = newMin.getZ(); z <= newMax.getZ(); ++z)
                {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(Material.BARRIER);
                    plugin.getGlowingBlockSpawner()
                          .spawnGlowinBlock(UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"), world.getName(), 1,
                                            x, y, z, ChatColor.LIGHT_PURPLE);
                }
        oldMin = newMin;
        oldMax = newMax;
    }

    /**
     * Updates the coordinates of a {@link DoorBase} and toggles its open status.
     *
     * @param door            The {@link DoorBase}.
     * @param openDirection   The direction this {@link DoorBase} was opened in.
     * @param rotateDirection The direction this {@link DoorBase} rotated.
     * @param moved           How many blocks this {@link DoorBase} moved.
     */
    private void updateCoords(final @NotNull DoorBase door, final @NotNull PBlockFace openDirection,
                              final @NotNull RotateDirection rotateDirection, final int moved)
    {
        Location newMin = new Location(world, 0, 0, 0);
        Location newMax = new Location(world, 0, 0, 0);
        Mutable<PBlockFace> newEngineSide = new Mutable<>(null);

        if (finalMin == null || finalMax == null)
            door.getNewLocations(openDirection, rotateDirection, newMin, newMax, moved, newEngineSide);
        else
        {
            newMin = finalMin;
            newMax = finalMax;
        }

        if (newMin.equals(door.getMinimum()) && newMax.equals(door.getMaximum()))
            return;

        door.setMaximum(newMax);
        door.setMinimum(newMin);

        if (!newEngineSide.isEmpty())
            door.setEngineSide(newEngineSide.getVal());

        toggleOpen(door);
        plugin.getDatabaseManager().updateDoorCoords(door.getDoorUID(), door.isOpen(), newMin.getBlockX(),
                                                     newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(),
                                                     newMax.getBlockY(), newMax.getBlockZ(), newEngineSide.getVal());
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
    protected abstract Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis);

    /**
     * Toggles the open status of a {@link DoorBase}.
     *
     * @param door The {@link DoorBase}.
     */
    private void toggleOpen(DoorBase door)
    {
        door.setOpenStatus(!door.isOpen());
    }

    /**
     * Constructs a new {@link ICustomCraftFallingBlock}.
     *
     * @param loc   The {@link Location} where the {@link ICustomCraftFallingBlock} will be created.
     * @param block The block of the {@link ICustomCraftFallingBlock}.
     * @return A new {@link ICustomCraftFallingBlock}.
     */
    @NotNull
    protected final ICustomCraftFallingBlock fallingBlockFactory(final @NotNull Location loc,
                                                                 final @NotNull INMSBlock block)
    {
        ICustomCraftFallingBlock entity = fabf.fallingBlockFactory(loc, block);
        Entity bukkitEntity = (Entity) entity;
        bukkitEntity.setCustomName("BigDoorsEntity");
        bukkitEntity.setCustomNameVisible(false);
        return entity;
    }

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
