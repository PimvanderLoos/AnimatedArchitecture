package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.Flag;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link Flag}s.
 *
 * @author Pim
 */
public class FlagMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final BiFunction<PBlockData, Integer, Vector3Dd> getGoalPos;
    private final boolean NS;
    private final double period;
    private final double amplitude;
    private final double waveSpeed;

    public FlagMover(final double time, final @NotNull Flag door, final double multiplier,
                     final @NotNull IPPlayer player, final @NotNull DoorActionCause cause,
                     final @NotNull DoorActionType actionType)
    {
        super(door, time, false, PBlockFace.UP, RotateDirection.NONE, -1, player, door.getMinimum(),
              door.getMaximum(), cause, actionType);

        final int xLen = Math.abs(xMax - xMin) + 1;
        final int zLen = Math.abs(zMax - zMin) + 1;
        NS = zLen > xLen;
        getGoalPos = NS ? this::getGoalPosNS : this::getGoalPosEW;

        final int length = NS ? zLen : xLen;
        period = length * 2.0f;
        amplitude = length / 4.0;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : Math.max(speed, minSpeed);
        this.time = time;

        super.tickRate = Util.tickRateFromSpeed(speed);
        super.tickRate = 3;

        waveSpeed = super.tickRate * 10.0f;

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = 200;
    }

    /**
     * Gets the maximum offset of a block.
     *
     * @param counter
     * @param radius
     * @return
     */
    private double getOffset(final int counter, final float radius)
    {
//        double baseOffset = Math.sin(0.5 * Math.PI * (counter * tickRate / 20) + radius);
//        double maxVal = 0.25 * radius;
//        maxVal = Math.min(maxVal, 0.75);
//        return Math.min(baseOffset, maxVal);

//        // The idea here is that blocks should never loose contact with other blocks.
//        // Especially the blocks with radius 1 should never loose contact with the pole.
//        double maxAmplitude = radius * 0.4;


        return Math.min(0.3 * radius, 3.2) * Math.sin(radius / 3.0 + ((double) counter / 4.0));

//        double offset;
//        try
//        {
//            offset = JCalculator
//                .getResult(BigDoors.get().getPlatform().getConfigLoader().flagFormula(),
//                           new String[]{"radius", "counter"},
//                           new double[]{radius, counter});
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            offset = 0;
//        }
//        return offset;
    }

    private Vector3Dd getGoalPosNS(final @NotNull PBlockData block, final int counter)
    {
        double xOff = 0;
        if (block.getRadius() > 0)
            xOff = getOffset(counter, block.getRadius());
        return new Vector3Dd(block.getStartX() + xOff, block.getStartY(), block.getStartZ());
    }

    private Vector3Dd getGoalPosEW(final @NotNull PBlockData block, final int counter)
    {
        double zOff = 0;
        if (block.getRadius() > 0)
            zOff = getOffset(counter, block.getRadius());
        return new Vector3Dd(block.getStartX(), block.getStartY(), block.getStartZ() + zOff);
    }

    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis, zAxis);
    }

    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        return block.getStartPosition();
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        final int counter = ticks / tickRate;
        for (final PBlockData block : savedBlocks)
        {
            Vector3Dd vec = getGoalPos.apply(block, counter).subtract(block.getFBlock().getPosition());
            block.getFBlock().setVelocity(vec.multiply(0.101));
        }
    }

    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        if (NS)
            return Math.abs(zAxis - door.getEngine().getZ());
        return Math.abs(xAxis - door.getEngine().getX());
    }
}
