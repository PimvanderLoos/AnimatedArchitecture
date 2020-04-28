package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.SlidingDoor;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link BlockMover} for {@link SlidingDoor}.
 *
 * @author Pim
 */
public class SlidingMover extends BlockMover
{
    private boolean NS;
    private int moveX, moveZ;

    private double step;

    @Nullable
    private PBlockData firstBlockData = null;

    public SlidingMover(final double time, final @NotNull AbstractDoorBase door, final boolean skipAnimation,
                        final int blocksToMove, final @NotNull RotateDirection openDirection, final double multiplier,
                        final @Nullable IPPlayer player, final @NotNull Vector3Di finalMin,
                        final @NotNull Vector3Di finalMax)
    {
        super(door, time, skipAnimation, PBlockFace.UP, openDirection, blocksToMove, player, finalMin, finalMax);

        NS = openDirection.equals(RotateDirection.NORTH) || openDirection.equals(RotateDirection.SOUTH);

        moveX = NS ? 0 : blocksToMove;
        moveZ = NS ? blocksToMove : 0;

        double speed = 1;
        double pcMult = multiplier;
        pcMult = pcMult == 0.0 ? 1.0 : pcMult;
        final int maxSpeed = 6;

        // If the time isn't default, calculate speed.
        if (time != 0.0)
        {
            speed = Math.abs(blocksToMove) / time;
            super.time = time;
        }

        // If the non-default exceeds the max-speed or isn't set, calculate default speed.
        if (time == 0.0 || speed > maxSpeed)
        {
            speed = 1.4 * pcMult;
            speed = speed > maxSpeed ? maxSpeed : speed;
            super.time = Math.abs(blocksToMove) / speed;
        }

        tickRate = Util.tickRateFromSpeed(speed);

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = 20 * (int) super.time;
        step = ((double) getBlocksMoved()) / ((double) super.endCount);
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return locationFactory.create(world, xAxis + moveX, yAxis, zAxis + moveZ);
    }

    private int getBlocksMoved()
    {
        return super.blocksMoved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull Vector3Dd startLocation = block.getStartPosition();
        final @NotNull IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                            startLocation.getY(), startLocation.getZ());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareAnimation()
    {
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = savedBlocks.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeAnimationStep(final int ticks)
    {
        if (firstBlockData == null)
            return;

        final Vector3Dd pos = firstBlockData.getStartPosition();
        final double stepSum = step * ticks;

        if (NS)
            pos.setZ(pos.getZ() + stepSum);
        else
            pos.setX(pos.getX() + stepSum);

        if (firstBlockData.getStartLocation().getY() != yMin)
            pos.setY(pos.getY() - .010001);
        final Vector3Dd vec = pos.subtract(firstBlockData.getFBlock().getPosition());
        vec.multiply(0.101);

        for (final PBlockData block : savedBlocks)
            block.getFBlock().setVelocity(vec);
    }
}
