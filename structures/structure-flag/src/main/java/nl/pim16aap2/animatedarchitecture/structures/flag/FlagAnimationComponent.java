package nl.pim16aap2.animatedarchitecture.structures.flag;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.Animator;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.jcalculator.JCalculator;

import java.util.function.BiFunction;

/**
 * Represents a {@link Animator} for {@link Flag}s.
 *
 * @author Pim
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "squid:S1172", "PMD"})
@Flogger
public final class FlagAnimationComponent implements IAnimationComponent
{
    private final IConfig config;
    private final BiFunction<IAnimatedBlock, Integer, RotatedPosition> getGoalPos;
    private final StructureSnapshot snapshot;
    private final boolean isNorthSouthAligned;
    private final int length;
    private final int minY;
    private final Cuboid oldCuboid;

    public FlagAnimationComponent(AnimationRequestData data, boolean isNorthSouthAligned)
    {
        this.snapshot = data.getStructureSnapshot();
        this.oldCuboid = snapshot.getCuboid();
        this.config = data.getConfig();

        final Vector3Di dims = oldCuboid.getDimensions();
        minY = snapshot.getMinimum().y();

        this.isNorthSouthAligned = isNorthSouthAligned;
        getGoalPos = this.isNorthSouthAligned ? this::getGoalPosNS : this::getGoalPosEW;

        length = this.isNorthSouthAligned ? dims.z() : dims.x();
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

    private RotatedPosition getGoalPosNS(IAnimatedBlock animatedBlock, int counter)
    {
        double xOff = 0;
        if (animatedBlock.getRadius() > 0)
            xOff = getOffset(counter, animatedBlock);
        return new RotatedPosition(
            new Vector3Dd(animatedBlock.getStartX() + xOff, animatedBlock.getStartY(), animatedBlock.getStartZ()));
    }

    private RotatedPosition getGoalPosEW(IAnimatedBlock animatedBlock, int counter)
    {
        double zOff = 0;
        if (animatedBlock.getRadius() > 0)
            zOff = getOffset(counter, animatedBlock);
        return new RotatedPosition(
            new Vector3Dd(animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ() + zOff));
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return new RotatedPosition(new Vector3Dd(xAxis, yAxis, zAxis));
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks)
    {
        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos.apply(animatedBlock, ticks));
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (isNorthSouthAligned)
            return Math.abs((float) zAxis - snapshot.getRotationPoint().z());
        return Math.abs((float) xAxis - snapshot.getRotationPoint().x());
    }
}
