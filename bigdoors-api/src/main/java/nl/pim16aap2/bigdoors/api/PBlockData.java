package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;

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
     * Constructor of {@link PBlockData}.
     *
     * @param newFBlock     The {@link ICustomCraftFallingBlock} that will be
     *                      animated.
     * @param radius        The number of blocks between this block and the rotation
     *                      point.
     * @param newBlock      If this block can be rotated, this contains the rotated
     *                      {@link INMSBlock}.
     * @param canRot        True if this block can rotate.
     * @param startLocation The location the block was spawned at initially.
     * @param startAngle    The angle the block had in regards to the rotation point when it was first spawned.
     */
    public PBlockData(ICustomCraftFallingBlock newFBlock, float radius, INMSBlock newBlock, boolean canRot,
                      Location startLocation, float startAngle)
    {
        block = newBlock;
        fBlock = newFBlock;
        this.radius = radius;
        this.canRot = canRot;
        this.startLocation = startLocation;
        this.startAngle = startAngle;
    }

    /**
     * Changes the {@link ICustomCraftFallingBlock} that is being be animated.
     * 
     * @param block The new {@link ICustomCraftFallingBlock} that will be
     *              animated.
     */
    public void setFBlock(ICustomCraftFallingBlock block)
    {
        fBlock = block;
    }

    /**
     * Get the {@link ICustomCraftFallingBlock} that is being be animated.
     */
    public ICustomCraftFallingBlock getFBlock()
    {
        return fBlock;
    }

    /**
     * Kill the {@link ICustomCraftFallingBlock} that is being be animated.
     */
    public void killFBlock()
    {
        if (fBlock != null)
            fBlock.remove();
    }

    /**
     * Get the number of blocks between this block and the rotation point.
     * 
     * @return The number of blocks between this block and the rotation point.
     */
    public float getRadius()
    {
        return radius;
    }

    /**
     * If it exist, get the rotated {@link INMSBlock}. If this block cannot
     * rotate, this value does not exist.
     * 
     * @return The rotated {@link INMSBlock} if it exists.
     */
    public INMSBlock getBlock()
    {
        return block;
    }

    /**
     * Check if this block can rotate.
     * 
     * @return True if this block can rotate.
     */
    public boolean canRot()
    {
        return canRot;
    }

    /**
     * Get the location the block was first spawned at.
     * 
     * @return The location the block was first spawned at.
     */
    public Location getStartLocation()
    {
        // block.getStartLocation() acts as a reference. I don't want that, so return a
        // copy instead.
        return new Location(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ());
    }

    /**
     * Get the x-coordinate of the location the block was first spawned at.
     * 
     * @return The x-coordinate of the location the block was first spawned at.
     */
    public double getStartX()
    {
        return startLocation.getX();
    }

    /**
     * Get the y-coordinate of the location the block was first spawned at.
     * 
     * @return The y-coordinate of the location the block was first spawned at.
     */
    public double getStartY()
    {
        return startLocation.getY();
    }

    /**
     * Get the z-coordinate of the location the block was first spawned at.
     * 
     * @return The 1-coordinate of the location the block was first spawned at.
     */
    public double getStartZ()
    {
        return startLocation.getZ();
    }

    /**
     * Get the angle the block had in regards to the rotation point when it was first spawned.
     *
     * @return The angle the block had in regards to the rotation point when it was first spawned.
     */
    public float getStartAngle()
    {
        return startAngle;
    }

}
