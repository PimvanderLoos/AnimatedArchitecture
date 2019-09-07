package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the data of an animated block.
 *
 * @author Pim
 */
public final class PBlockData
{
    private INMSBlock block;
    private final boolean canRot;
    private final float radius;
    private ICustomCraftFallingBlock fBlock;
    private final IPLocation startLocation;
    private float startAngle;
    private final boolean deferredPlacement;

    /**
     * Constructs of {@link PBlockData}.
     *
     * @param newFBlock         The {@link ICustomCraftFallingBlock} that will be animated.
     * @param radius            The number of blocks between this block and the rotation point.
     * @param newBlock          If this block can be rotated, this contains the rotated {@link INMSBlock}.
     * @param startLocation     The location the block was spawned at initially.
     * @param startAngle        The angle the block had in regards to the rotation point when it was first spawned.
     * @param deferredPlacement Whether or not placement should be deferred until all standalone blocks are placed.
     *                          Useful for torches, for example (so they don't fall off immediately).
     */
    public PBlockData(final @NotNull ICustomCraftFallingBlock newFBlock, final float radius,
                      final @NotNull INMSBlock newBlock, final @NotNull IPLocation startLocation,
                      final float startAngle, final boolean deferredPlacement)
    {
        block = newBlock;
        fBlock = newFBlock;
        this.radius = radius;
        canRot = newBlock.canRotate();
        this.startLocation = startLocation;
        this.startAngle = startAngle;
        this.deferredPlacement = deferredPlacement;
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
    public IPLocation getStartLocation()
    {
        // Return a new object, so it's not a reference.
        return BigDoors.get().getPlatform().getPLocationFactory()
                       .create(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
                               startLocation.getZ());
    }

    /**
     * Checks if placement of this block should be deferred to the second pass or not.
     * <p>
     * On the first pass, "standalone" blocks such as stone will be placed, while other blocks such as torches, will be
     * skipped.
     * <p>
     * On the second pass, all the other blocks will be placed. This makes sure that torches aren't just dropped.
     *
     * @return True if this block should be placed on the second pass, otherwise false.
     */
    public boolean deferPlacement()
    {
        return deferredPlacement;
    }

    /**
     * Gets the starting position of this {@link PBlockData}.
     *
     * @return The starting position of this {@link PBlockData}.
     */
    @NotNull
    public Vector3Dd getStartPosition()
    {
        return new Vector3Dd(startLocation.getX(), startLocation.getY(), startLocation.getZ());
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
