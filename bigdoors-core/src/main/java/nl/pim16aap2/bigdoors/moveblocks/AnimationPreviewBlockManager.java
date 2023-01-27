package nl.pim16aap2.bigdoors.moveblocks;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Flogger
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AnimationPreviewBlockManager implements IAnimationBlockManager
{
    private final IPLocationFactory locationFactory;
    private final GlowingBlockSpawner glowingBlockSpawner;
    private final IPPlayer player;

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

    AnimationPreviewBlockManager(
        IPLocationFactory locationFactory, GlowingBlockSpawner glowingBlockSpawner, IPPlayer player)
    {
        this.locationFactory = locationFactory;
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.player = player;

        privateAnimatedBlocks = new CopyOnWriteArrayList<>();
        animatedBlocks = Collections.unmodifiableList(privateAnimatedBlocks);
    }

    @Override
    public boolean createAnimatedBlocks(
        MovableSnapshot snapshot, IAnimationComponent animationComponent, AnimationContext animationContext,
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
                        final float radius = animationComponent.getRadius(xAxis, yAxis, zAxis);
                        final float startAngle = animationComponent.getStartAngle(xAxis, yAxis, zAxis);
                        final Vector3Dd startPosition = new Vector3Dd(xAxis + 0.5, yAxis, zAxis + 0.5);
                        final Vector3Dd finalPosition = animationComponent.getFinalPosition(startPosition, radius);

                        animatedBlocksTmp.add(
                            new AnimatedPreviewBlock(
                                locationFactory, glowingBlockSpawner, snapshot.getWorld(), player, startPosition,
                                finalPosition, startAngle, radius, PColor.AQUA));
                    }
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

    @Override
    public void restoreBlocksOnFailure()
    {
        // No need to do anything; just wait a sec.
    }

    @Override
    public void handleAnimationCompletion()
    {
        // No need to do anything; just wait a sec.
    }
}
