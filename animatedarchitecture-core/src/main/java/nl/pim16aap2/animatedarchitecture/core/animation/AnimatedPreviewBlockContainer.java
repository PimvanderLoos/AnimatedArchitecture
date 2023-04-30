package nl.pim16aap2.animatedarchitecture.core.animation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.AnimatedHighlightedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A manager for {@link AnimatedHighlightedBlock}s.
 */
@Flogger
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AnimatedPreviewBlockContainer implements IAnimatedBlockContainer
{
    private final ILocationFactory locationFactory;
    private final HighlightedBlockSpawner glowingBlockSpawner;
    private final IPlayer player;

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

    @Getter
    private volatile @Nullable AnimationRegion animationRegion;

    AnimatedPreviewBlockContainer(
        ILocationFactory locationFactory, HighlightedBlockSpawner glowingBlockSpawner, IPlayer player)
    {
        this.locationFactory = locationFactory;
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.player = player;

        privateAnimatedBlocks = new CopyOnWriteArrayList<>();
        animatedBlocks = Collections.unmodifiableList(privateAnimatedBlocks);
    }

    @Override
    public boolean createAnimatedBlocks(StructureSnapshot snapshot, IAnimationComponent animationComponent)
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
                        final Vector3Di position = new Vector3Di(xAxis, yAxis, zAxis);
                        final float radius = animationComponent.getRadius(xAxis, yAxis, zAxis);
                        final Color color = getColor(cuboid, position);

                        final RotatedPosition startPosition = animationComponent.getStartPosition(xAxis, yAxis, zAxis);

                        animatedBlocksTmp.add(
                            new AnimatedHighlightedBlock(
                                locationFactory, glowingBlockSpawner, snapshot.getWorld(),
                                player, startPosition, radius, color));
                    }
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
            this.privateAnimatedBlocks.addAll(animatedBlocksTmp);
            return false;
        }

        this.privateAnimatedBlocks.addAll(animatedBlocksTmp);

        animationRegion = new AnimationRegion(
            animatedBlocks, animationComponent::getRadius, animationComponent::getFinalPosition);

        return true;
    }

    private Color getColor(Cuboid cuboid, Vector3Di position)
    {
        if (position.equals(cuboid.getMin()))
            return Color.RED;
        if (position.equals(cuboid.getMax()))
            return Color.GREEN;
        return Color.BLUE;
    }

    @Override
    public void restoreBlocksOnFailure()
    {
        privateAnimatedBlocks.forEach(IAnimatedBlock::kill);
        privateAnimatedBlocks.clear();
    }

    @Override
    public void handleAnimationCompletion()
    {
        privateAnimatedBlocks.forEach(IAnimatedBlock::kill);
        privateAnimatedBlocks.clear();
    }
}
