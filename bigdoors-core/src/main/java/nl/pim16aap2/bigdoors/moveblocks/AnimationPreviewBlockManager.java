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
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

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
            final Cuboid cuboid = snapshot.getCuboid();
            final int xMin = cuboid.getMin().x();
            final int yMin = cuboid.getMin().y();
            final int zMin = cuboid.getMin().z();

            final int xMax = cuboid.getMax().x();
            final int yMax = cuboid.getMax().y();
            final int zMax = cuboid.getMax().z();

            for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (int yAxis = yMax; yAxis >= yMin; --yAxis)
                    for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                    {
                        if ((xAxis + yAxis + zAxis) % 2 == 0)
                            continue;

                        final Vector3Di position = new Vector3Di(xAxis, yAxis, zAxis);

                        final float radius = animationComponent.getRadius(xAxis, yAxis, zAxis);
                        final float startAngle = animationComponent.getStartAngle(xAxis, yAxis, zAxis);
                        final PColor color = getColor(cuboid, position);
                        final Vector3Dd startPosition = new Vector3Dd(xAxis + 0.5, yAxis, zAxis + 0.5);
                        final Vector3Dd finalPosition = animationComponent.getFinalPosition(startPosition, radius);

                        animatedBlocksTmp.add(
                            new AnimatedPreviewBlock(
                                locationFactory, glowingBlockSpawner, snapshot.getWorld(), player, startPosition,
                                finalPosition, startAngle, radius, color));
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

    private PColor getColor(Cuboid cuboid, Vector3Di position)
    {
        if (position.equals(cuboid.getMin()))
            return PColor.RED;
        if (position.equals(cuboid.getMax()))
            return PColor.GREEN;
        return PColor.BLUE;
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
