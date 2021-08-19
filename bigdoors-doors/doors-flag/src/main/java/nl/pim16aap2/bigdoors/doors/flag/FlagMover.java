package nl.pim16aap2.bigdoors.doors.flag;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link Flag}s.
 *
 * @author Pim
 */
public class FlagMover extends BlockMover
{
    private final BiFunction<PBlockData, Integer, Vector3Dd> getGoalPos;
    private final boolean NS;
    private final double period;
    private final double amplitude;
    private final double waveSpeed;

    public FlagMover(final double time, final Flag door, final double multiplier,
                     final IPPlayer player, final DoorActionCause cause,
                     final DoorActionType actionType)
        throws Exception
    {
        super(door, time, false, RotateDirection.NONE, player, door.getCuboid(), cause, actionType);

        final int xLen = Math.abs(xMax - xMin) + 1;
        final int zLen = Math.abs(zMax - zMin) + 1;
        NS = door.isNorthSouthAligned();
        getGoalPos = NS ? this::getGoalPosNS : this::getGoalPosEW;

        final int length = NS ? zLen : xLen;
        period = length * 2.0f;
        amplitude = length / 4.0;

        this.time = time;
        waveSpeed = 10.0f;

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

    private Vector3Dd getGoalPosNS(final PBlockData block, final int counter)
    {
        double xOff = 0;
        if (block.getRadius() > 0)
            xOff = getOffset(counter, block.getRadius());
        return new Vector3Dd(block.getStartX() + xOff, block.getStartY(), block.getStartZ());
    }

    private Vector3Dd getGoalPosEW(final PBlockData block, final int counter)
    {
        double zOff = 0;
        if (block.getRadius() > 0)
            zOff = getOffset(counter, block.getRadius());
        return new Vector3Dd(block.getStartX(), block.getStartY(), block.getStartZ() + zOff);
    }

    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                        final double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis, zAxis);
    }

    @Override
    protected Vector3Dd getFinalPosition(final PBlockData block)
    {
        return block.getStartPosition();
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        for (final PBlockData block : savedBlocks)
            block.getFBlock().teleport(getGoalPos.apply(block, ticks));
    }

    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        if (NS)
            return Math.abs((float) zAxis - door.getEngine().z());
        return Math.abs((float) xAxis - door.getEngine().x());
    }
}
