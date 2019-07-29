package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the data of an animated block.
 *
 * @author Pim
 */
public final class PBlockData
{
    private INMSBlock block;
    private boolean canRot;
    private float radius;
    private ICustomCraftFallingBlock fBlock;
    private Location startLocation;
    private float startAngle;

    /**
     * Constructs of {@link PBlockData}.
     *
     * @param newFBlock     The {@link ICustomCraftFallingBlock} that will be animated.
     * @param radius        The number of blocks between this block and the rotation point.
     * @param newBlock      If this block can be rotated, this contains the rotated {@link INMSBlock}.
     * @param canRot        True if this block can rotate.
     * @param startLocation The location the block was spawned at initially.
     * @param startAngle    The angle the block had in regards to the rotation point when it was first spawned.
     */
    public PBlockData(final @NotNull ICustomCraftFallingBlock newFBlock, final float radius,
                      final @NotNull INMSBlock newBlock, final boolean canRot, final @NotNull Location startLocation,
                      final float startAngle)
    {
        block = newBlock;
        fBlock = newFBlock;
        this.radius = radius;
        this.canRot = canRot;
        this.startLocation = startLocation;
        this.startAngle = startAngle;
    }

    /**
     * Gets the {@link ICustomCraftFallingBlock} that is being be animated.
     */
    @NotNull
    public ICustomCraftFallingBlock getFBlock()
    {
        return fBlock;
    }

    /**
     * Changes the {@link ICustomCraftFallingBlock} that is being be animated.
     *
     * @param block The new {@link ICustomCraftFallingBlock} that will be animated.
     */
    public void setFBlock(final @NotNull ICustomCraftFallingBlock block)
    {
        fBlock = block;
    }

    /**
     * Kills the {@link ICustomCraftFallingBlock} that is being be animated.
     */
    public void killFBlock()
    {
        if (fBlock != null)
            fBlock.remove();
    }

    /**
     * Gets the number of blocks between this block and the rotation point.
     *
     * @return The number of blocks between this block and the rotation point.
     */
    public float getRadius()
    {
        return radius;
    }

    /**
     * Gets the rotated {@link INMSBlock} if it exists. If this block cannot rotate, this value does not exist.
     *
     * @return The rotated {@link INMSBlock} if it exists.
     */
    @NotNull
    public INMSBlock getBlock()
    {
        return block;
    }

    /**
     * Checks if this block can rotate.
     *
     * @return True if this block can rotate.
     */
    public boolean canRot()
    {
        return canRot;
    }

    /**
     * Gets the location the block was first spawned at.
     *
     * @return The location the block was first spawned at.
     */
    @NotNull
    public Location getStartLocation()
    {
        // block.getStartLocation() acts as a reference. I don't want that, so return a
        // copy instead.
        return new Location(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ());
    }

    /**
     * Gets the x-coordinate of the location the block was first spawned at.
     *
     * @return The x-coordinate of the location the block was first spawned at.
     */
    public double getStartX()
    {
        return startLocation.getX();
    }

    /**
     * Gets the y-coordinate of the location the block was first spawned at.
     *
     * @return The y-coordinate of the location the block was first spawned at.
     */
    public double getStartY()
    {
        return startLocation.getY();
    }

    /**
     * Gets the z-coordinate of the location the block was first spawned at.
     *
     * @return The 1-coordinate of the location the block was first spawned at.
     */
    public double getStartZ()
    {
        return startLocation.getZ();
    }

    /**
     * Gets the angle the block had in regards to the rotation point when it was first spawned.
     *
     * @return The angle the block had in regards to the rotation point when it was first spawned.
     */
    public float getStartAngle()
    {
        return startAngle;
    }
}
