package nl.pim16aap2.bigdoors.api;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents the data of an animated block.
 *
 * @author Pim
 */
public final class PBlockData
{
    /**
     * Gets the rotated {@link INMSBlock} if it exists. If this block cannot rotate, this value does not exist.
     *
     * @return The rotated {@link INMSBlock} if it exists.
     */
    @Getter
    private final INMSBlock block;

    /**
     * Checks if this block can rotate.
     *
     * @return True if this block can rotate.
     */
    @Getter
    private final boolean rotatable;
    /**
     * The number of blocks between this block and the rotation point.
     */
    @Getter
    private final float radius;

    /**
     * The {@link ICustomCraftFallingBlock} that is being be animated.
     *
     * @param fBlock The new {@link ICustomCraftFallingBlock} that will be animated. Note that just updating the value
     * won't change anything in-game.
     * @return Gets the {@link ICustomCraftFallingBlock} that is being be animated.
     */
    @Getter
    @Setter
    private ICustomCraftFallingBlock fBlock;

    /**
     * Gets the location the block was first spawned at.
     *
     * @return The location the block was first spawned at.
     */
    @Getter
    private final IPLocation startLocation;

    /**
     * The angle the block had in regards to the rotation point when it was first spawned.
     */
    @Getter
    private final float startAngle;

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
    @Getter
    private final boolean placementDeferred;

    /**
     * Constructs of {@link PBlockData}.
     *
     * @param newFBlock
     *     The {@link ICustomCraftFallingBlock} that will be animated.
     * @param radius
     *     The number of blocks between this block and the rotation point.
     * @param newBlock
     *     If this block can be rotated, this contains the rotated {@link INMSBlock}.
     * @param startLocation
     *     The location the block was spawned at initially.
     * @param startAngle
     *     The angle the block had in regards to the rotation point when it was first spawned.
     * @param placementDeferred
     *     Whether or not placement should be deferred until all standalone blocks are placed. Useful for torches, for
     *     example (so they don't fall off immediately).
     */
    public PBlockData(ICustomCraftFallingBlock newFBlock, float radius, INMSBlock newBlock, IPLocation startLocation,
                      float startAngle, boolean placementDeferred)
    {
        block = newBlock;
        fBlock = newFBlock;
        this.radius = radius;
        rotatable = newBlock.canRotate();
        this.startLocation = startLocation;
        this.startAngle = startAngle;
        this.placementDeferred = placementDeferred;
    }

    /**
     * Kills the {@link ICustomCraftFallingBlock} that is being be animated.
     */
    public void killFBlock()
    {
        fBlock.remove();
    }

    /**
     * Gets the starting position of this {@link PBlockData}.
     *
     * @return The starting position of this {@link PBlockData}.
     */
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
     * @return The z-coordinate of the location the block was first spawned at.
     */
    public double getStartZ()
    {
        return startLocation.getZ();
    }
}
