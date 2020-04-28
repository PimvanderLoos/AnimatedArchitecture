package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.HorizontalAxisAlignedBase;
import nl.pim16aap2.bigdoors.doors.Windmill;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link BlockMover} for {@link Windmill}s.
 *
 * @author Pim
 */
public class WindmillMover extends BridgeMover
{
    protected static final double EPS = 2 * Double.MIN_VALUE;

    private double step;

    public WindmillMover(final @NotNull HorizontalAxisAlignedBase door, final double time, final double multiplier,
                         final @NotNull RotateDirection rotateDirection, final @Nullable IPPlayer player)
    {
        super(time, door, PBlockFace.NONE, rotateDirection, false, multiplier, player, door.getMinimum(),
              door.getMaximum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init()
    {
        super.endCount = 20 * 20 * (int) super.time;
        step = (Math.PI / 2.0) / (20.0f * super.time * 2.0f);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis, zAxis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        return block.getStartPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeAnimationStep(final int ticks)
    {
        double stepSum = step * ticks;
        for (final PBlockData block : savedBlocks)
            // TODO: Store separate list to avoid checking this constantly.
            if (Math.abs(block.getRadius()) > EPS)
            {
                final Vector3Dd vec = getVector.apply(block, stepSum).subtract(block.getFBlock().getPosition());
                block.getFBlock().setVelocity(vec.multiply(0.101));
            }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the X values does not change for this type.
        final double deltaA = (door.getEngine().getY() - yAxis);
        final double deltaB = !NS ? (door.getEngine().getX() - xAxis) : (door.getEngine().getZ() - zAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getStartAngle(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the angle between the used axes (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the X values does not change for this type.
        final float deltaA = !NS ? door.getEngine().getX() - xAxis : door.getEngine().getZ() - zAxis;
        final float deltaB = door.getEngine().getY() - yAxis;

        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
