package nl.pim16aap2.bigdoors.core.moveblocks;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.core.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Flogger
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AnimationBlockManager implements IAnimationBlockManager
{
    private final IPLocationFactory locationFactory;
    private final IAnimatedBlockFactory animatedBlockFactory;
    private final IPExecutor executor;

    /**
     * The modifiable list of animated blocks.
     */
    private final List<IAnimatedBlock> privateAnimatedBlocks;

    /**
     * The (unmodifiable) list of animated blocks.
     */
    @Getter
    @ToString.Include @EqualsAndHashCode.Include
    private final List<IAnimatedBlock> animatedBlocks;

    AnimationBlockManager(
        IPLocationFactory locationFactory, IAnimatedBlockFactory animatedBlockFactory, IPExecutor executor)
    {
        this.locationFactory = locationFactory;
        this.animatedBlockFactory = animatedBlockFactory;
        this.executor = executor;

        privateAnimatedBlocks = new CopyOnWriteArrayList<>();
        animatedBlocks = Collections.unmodifiableList(privateAnimatedBlocks);
    }

    @Override
    public boolean createAnimatedBlocks(
        StructureSnapshot snapshot, IAnimationComponent animationComponent, AnimationContext animationContext,
        Animator.MovementMethod movementMethod)
    {
        final List<IAnimatedBlock> animatedBlocksTmp = new ArrayList<>(snapshot.getBlockCount());

        try
        {
            final int xMin = snapshot.getCuboid().getMin().x();
            final int yMin = snapshot.getCuboid().getMin().y();
            final int zMin = snapshot.getCuboid().getMin().z();

            final int xMax = snapshot.getCuboid().getMax().x();
            final int yMax = snapshot.getCuboid().getMax().y();
            final int zMax = snapshot.getCuboid().getMax().z();

            for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (int yAxis = yMax; yAxis >= yMin; --yAxis)
                    for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    {
                        final boolean onEdge =
                            xAxis == xMin || xAxis == xMax ||
                                yAxis == yMin || yAxis == yMax ||
                                zAxis == zMin || zAxis == zMax;

                        final IPLocation location =
                            locationFactory.create(snapshot.getWorld(), xAxis + 0.5, yAxis, zAxis + 0.5);
                        final boolean bottom = (yAxis == yMin);
                        final float radius = animationComponent.getRadius(xAxis, yAxis, zAxis);
                        final float startAngle = animationComponent.getStartAngle(xAxis, yAxis, zAxis);
                        final Vector3Dd startPosition = new Vector3Dd(xAxis + 0.5, yAxis, zAxis + 0.5);
                        final Vector3Dd finalPosition = animationComponent.getFinalPosition(startPosition, radius);

                        animatedBlockFactory
                            .create(location, radius, startAngle, bottom, onEdge, animationContext, finalPosition,
                                    movementMethod)
                            .ifPresent(animatedBlocksTmp::add);
                    }

            tryRemoveOriginalBlocks(animatedBlocksTmp, false);
            tryRemoveOriginalBlocks(animatedBlocksTmp, true);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
            this.privateAnimatedBlocks.addAll(animatedBlocksTmp);
            return false;
        }

        this.privateAnimatedBlocks.addAll(animatedBlocksTmp);
        return true;
    }

    /**
     * Tries to remove the original blocks of a list of animated blocks.
     *
     * @param animatedBlocks
     *     The animated blocks to process.
     * @param edgePass
     *     True to do a pass over the edges specifically.
     * @return True if the original blocks could be spawned. If something went wrong and the process had to be aborted,
     * false is returned instead.
     */
    private void tryRemoveOriginalBlocks(List<IAnimatedBlock> animatedBlocks, boolean edgePass)
    {
        executor.assertMainThread("Blocks must be removed on the main thread!");

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
        {
            if (edgePass && !animatedBlock.isOnEdge())
                continue;
            animatedBlock.getAnimatedBlockData().deleteOriginalBlock(edgePass);
        }
    }

    @Override
    public void restoreBlocksOnFailure()
    {
        executor.assertMainThread("Blocks cannot be placed asynchronously!");
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
        {
            try
            {
                animatedBlock.kill();
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to kill animated block: %s", animatedBlock);
            }
            try
            {
                final Vector3Dd startPos = animatedBlock.getStartPosition();
                final Vector3Di goalPos = new Vector3Di((int) startPos.x(),
                                                        (int) Math.round(startPos.y()),
                                                        (int) startPos.z());
                animatedBlock.getAnimatedBlockData().putBlock(goalPos);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to restore block: %s", animatedBlock);
            }
        }
        privateAnimatedBlocks.clear();
    }

    @Override
    public void handleAnimationCompletion()
    {
        executor.assertMainThread("Blocks cannot be placed asynchronously!");
        for (final IAnimatedBlock animatedBlock : privateAnimatedBlocks)
        {
            animatedBlock.kill();
            animatedBlock.getAnimatedBlockData().putBlock(animatedBlock.getFinalPosition());
        }
        privateAnimatedBlocks.clear();
    }
}
