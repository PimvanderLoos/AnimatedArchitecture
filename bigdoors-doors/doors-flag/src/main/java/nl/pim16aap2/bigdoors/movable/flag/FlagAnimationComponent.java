package nl.pim16aap2.bigdoors.movable.flag;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.jcalculator.JCalculator;

import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link Flag}s.
 *
 * @author Pim
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "squid:S1172", "PMD"})
@Flogger
public final class FlagAnimationComponent implements IAnimationComponent
{
    private final IConfigLoader config;
    private final BiFunction<IAnimatedBlock, Integer, Vector3Dd> getGoalPos;
    private final MovableSnapshot snapshot;
    private final boolean isNorthSouthAligned;
    private final int length;
    private final int minY;
    private final Cuboid oldCuboid;

    public FlagAnimationComponent(MovementRequestData data, boolean isNorthSouthAligned)
    {
        this.snapshot = data.getSnapshotOfMovable();
        this.oldCuboid = snapshot.getCuboid();
        this.config = data.getConfig();

        final Vector3Di dims = oldCuboid.getDimensions();
        minY = snapshot.getMinimum().y();

        this.isNorthSouthAligned = isNorthSouthAligned;
        getGoalPos = this.isNorthSouthAligned ? this::getGoalPosNS : this::getGoalPosEW;

        length = this.isNorthSouthAligned ? dims.z() : dims.x();
    }

    @Override
    public BlockMover.MovementMethod getMovementMethod()
    {
        return BlockMover.MovementMethod.TELEPORT;
    }

    private double getOffset(int counter, IAnimatedBlock animatedBlock)
    {
        final String formula = config.flagMovementFormula();
        try
        {
            return JCalculator
                .getResult(formula,
                           new String[]{"radius", "counter", "length", "height"},
                           new double[]{animatedBlock.getRadius(), counter, length,
                                        Math.round(animatedBlock.getStartY() - minY)});
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to parse flag formula: '%s'", formula);
            return 0.0D;
        }
    }

    private Vector3Dd getGoalPosNS(IAnimatedBlock animatedBlock, int counter)
    {
        double xOff = 0;
        if (animatedBlock.getRadius() > 0)
            xOff = getOffset(counter, animatedBlock);
        return new Vector3Dd(animatedBlock.getStartX() + xOff, animatedBlock.getStartY(), animatedBlock.getStartZ());
    }

    private Vector3Dd getGoalPosEW(IAnimatedBlock animatedBlock, int counter)
    {
        double zOff = 0;
        if (animatedBlock.getRadius() > 0)
            zOff = getOffset(counter, animatedBlock);
        return new Vector3Dd(animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ() + zOff);
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos.apply(animatedBlock, ticks), ticksRemaining);
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (isNorthSouthAligned)
            return Math.abs((float) zAxis - snapshot.getRotationPoint().z());
        return Math.abs((float) xAxis - snapshot.getRotationPoint().x());
    }
}
