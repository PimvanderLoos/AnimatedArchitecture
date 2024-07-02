package nl.pim16aap2.animatedarchitecture.core.animation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Represents an implementation of {@link IAnimatedBlockContainer} for animated blocks that affect the world.
 */
@Flogger
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AnimatedBlockContainer implements IAnimatedBlockContainer
{
    private final IAnimatedBlockFactory animatedBlockFactory;
    private final IExecutor executor;

    /**
     * The modifiable list of animated blocks.
     */
    private final List<IAnimatedBlock> privateAnimatedBlocks;

    /**
     * The (unmodifiable) list of animated blocks.
     */
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    private final List<IAnimatedBlock> animatedBlocks;

    @Getter
    private volatile @Nullable AnimationRegion animationRegion;

    AnimatedBlockContainer(IAnimatedBlockFactory animatedBlockFactory, IExecutor executor)
    {
        this.animatedBlockFactory = animatedBlockFactory;
        this.executor = executor;

        privateAnimatedBlocks = new CopyOnWriteArrayList<>();
        animatedBlocks = Collections.unmodifiableList(privateAnimatedBlocks);
    }

    @Override
    public boolean createAnimatedBlocks(StructureSnapshot snapshot, IAnimationComponent animationComponent)
    {
        final List<IAnimatedBlock> animatedBlocksTmp = new ArrayList<>(snapshot.getBlockCount());

        int posX = Integer.MAX_VALUE;
        int posY = Integer.MAX_VALUE;
        int posZ = Integer.MAX_VALUE;

        try
        {
            final int xMin = snapshot.getCuboid().getMin().x();
            final int yMin = snapshot.getCuboid().getMin().y();
            final int zMin = snapshot.getCuboid().getMin().z();

            final int xMax = snapshot.getCuboid().getMax().x();
            final int yMax = snapshot.getCuboid().getMax().y();
            final int zMax = snapshot.getCuboid().getMax().z();

            for (posX = xMin; posX <= xMax; ++posX)
                for (posY = yMax; posY >= yMin; --posY)
                    for (posZ = zMin; posZ <= zMax; ++posZ)
                    {
                        final boolean onEdge =
                            posX == xMin ||
                                posX == xMax ||
                                posY == yMin ||
                                posY == yMax ||
                                posZ == zMin ||
                                posZ == zMax;

                        final float radius = animationComponent.getRadius(posX, posY, posZ);
                        final var blockDataRotator = animationComponent.getBlockDataRotator();
                        final RotatedPosition startPosition = animationComponent.getStartPosition(posX, posY, posZ);
                        final RotatedPosition finalPosition = animationComponent.getFinalPosition(posX, posY, posZ);

                        animatedBlockFactory.create(
                                snapshot.getWorld(),
                                startPosition,
                                radius,
                                onEdge,
                                finalPosition,
                                blockDataRotator)
                            .ifPresent(animatedBlocksTmp::add);
                    }
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to create animated blocks at position: [%s, %s, %s]", posX, posY, posZ);
            this.privateAnimatedBlocks.addAll(animatedBlocksTmp);
            return false;
        }

        this.privateAnimatedBlocks.addAll(animatedBlocksTmp);

        animationRegion = new AnimationRegion(
            animatedBlocks,
            animationComponent::getRadius,
            animationComponent::getFinalPosition
        );

        return true;
    }

    @Override
    public void spawnAnimatedBlocks()
    {
        executor.assertMainThread("Blocks must be spawned on the main thread!");
        try
        {
            animatedBlocks.forEach(this::spawnAnimatedBlock);
            animatedBlocks.forEach(animatedBlock -> animatedBlock.getAnimatedBlockData().postProcessStructureRemoval());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to spawn animated blocks!", e);
        }
    }

    /**
     * Tries to remove the original blocks of the animated blocks.
     *
     * @return True if the original blocks could be spawned. If something went wrong and the process had to be aborted,
     * false is returned instead.
     *
     * @throws RuntimeException
     *     If the blocks could not be removed or spawned for some reason.
     */
    private void spawnAnimatedBlock(IAnimatedBlock animatedBlock)
    {
        try
        {
            animatedBlock.getAnimatedBlockData().deleteOriginalBlock();
            animatedBlock.spawn();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to spawn animated block: " + animatedBlock, e);
        }
    }

    @Override
    public void removeOriginalBlocks()
    {
        try
        {
            tryRemoveOriginalBlocks(animatedBlocks);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to remove original blocks!", e);
        }
    }

    private void tryRemoveOriginalBlocks(List<IAnimatedBlock> animatedBlocks)
    {
        executor.assertMainThread("Blocks must be removed on the main thread!");
        animatedBlocks.forEach(block -> block.getAnimatedBlockData().deleteOriginalBlock());
        animatedBlocks.forEach(block -> block.getAnimatedBlockData().postProcessStructureRemoval());
    }

    private void putBlocks(Function<IAnimatedBlock, IVector3D> mapper)
    {
        executor.assertMainThread("Blocks cannot be placed asynchronously!");
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
        {
            try
            {
                final IVector3D goalPos = mapper.apply(animatedBlock).toInteger();
                animatedBlock.getAnimatedBlockData().putBlock(goalPos);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to place block: %s", animatedBlock);
            }
            try
            {
                animatedBlock.kill();
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to kill animated block: %s", animatedBlock);
            }
        }
        privateAnimatedBlocks.clear();
    }

    @Override
    public void restoreBlocksOnFailure()
    {
        putBlocks(animatedBlock -> animatedBlock.getStartPosition().position());
    }

    @Override
    public void handleAnimationCompletion()
    {
        putBlocks(animatedBlock -> animatedBlock.getFinalPosition().position());
    }
}
