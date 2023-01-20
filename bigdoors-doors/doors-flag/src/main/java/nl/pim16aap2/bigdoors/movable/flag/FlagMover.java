package nl.pim16aap2.bigdoors.movable.flag;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
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
@SuppressWarnings({"FieldCanBeLocal", "unused", "squid:S1172", "CommentedOutCode", "PMD"})
@Flogger
public class FlagMover extends BlockMover
{
    private final IConfigLoader config;
    private final BiFunction<IAnimatedBlock, Integer, Vector3Dd> getGoalPos;
    private final boolean NS;
    private final int length;
    private final int minY;

    public FlagMover(
        Context context, Flag movable, MovableSnapshot snapshot, double time, IPPlayer player,
        Cuboid animationRange, MovableActionCause cause, MovableActionType actionType)
        throws Exception
    {
        super(context, movable, snapshot, time, false, RotateDirection.NONE, player, snapshot.getCuboid(),
              animationRange, cause, actionType);

        this.config = context.getConfig();

        final Vector3Di dims = oldCuboid.getDimensions();
        minY = snapshot.getMinimum().y();

        NS = movable.isNorthSouthAligned();
        getGoalPos = NS ? this::getGoalPosNS : this::getGoalPosEW;

        length = NS ? dims.z() : dims.x();

        super.movementMethod = MovementMethod.TELEPORT;
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
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation);
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getGoalPos.apply(animatedBlock, ticks), ticksRemaining);
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (NS)
            return Math.abs((float) zAxis - snapshot.getRotationPoint().z());
        return Math.abs((float) xAxis - snapshot.getRotationPoint().x());
    }
}
