package nl.pim16aap2.animatedarchitecture.core.animation;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a region of animated blocks.
 * <p>
 * The region is created by creating a cuboid around the animated blocks, and then creating a marker block for each
 * animated block. The marker blocks do not exist in the world and are only used to update the region during the
 * animation.
 */
public final class AnimationRegion
{
    private final List<MarkerBlock> privateMarkerBlocks = new ArrayList<>(8);

    /**
     * The 8 marker blocks that represent the region of animated blocks as an unmodifiable list.
     * <p>
     * These blocks are simplified versions of the animated blocks, and are used to update the region during the
     * animation. They have limited functionality.
     */
    @Getter
    private final List<IAnimatedBlock> markerBlocks = Collections.unmodifiableList(privateMarkerBlocks);

    /**
     * The cuboid that represents the region of animated blocks at the start of the animation.
     */
    @Getter
    private final Cuboid startCuboid;

    /**
     * Creates a new animation region.
     *
     * @param blocks
     *     The animated blocks that are part of the region. The marker blocks will be created by creating a cuboid
     *     around these blocks.
     * @param radiusSupplier
     *     The radius supplier. This is used to determine the radius of the marker blocks.
     * @param finalPositionSupplier
     *     The final position supplier. This is used to determine the final position of the marker blocks.
     */
    public AnimationRegion(
        List<IAnimatedBlock> blocks,
        TriFunction<Integer, Integer, Integer, Float> radiusSupplier,
        TriFunction<Integer, Integer, Integer, RotatedPosition> finalPositionSupplier)
    {
        this.startCuboid = createMarkerBlocks(blocks, privateMarkerBlocks, radiusSupplier, finalPositionSupplier);
    }

    /**
     * Calculates the current region of the animation.
     *
     * @return The cuboid that represents the current region of the animation.
     */
    public Cuboid getRegion()
    {
        return getRegion(markerBlocks);
    }

    private Cuboid createMarkerBlocks(
        List<IAnimatedBlock> blocks,
        List<MarkerBlock> target,
        TriFunction<Integer, Integer, Integer, Float> radiusSupplier,
        TriFunction<Integer, Integer, Integer, RotatedPosition> finalPositionSupplier)
    {
        final Cuboid cuboid = getRegion(blocks);
        createMarkerBlocks(cuboid, target, radiusSupplier, finalPositionSupplier);
        return cuboid;
    }

    private void createMarkerBlocks(
        Cuboid cuboid,
        List<MarkerBlock> target,
        TriFunction<Integer, Integer, Integer, Float> radiusSupplier,
        TriFunction<Integer, Integer, Integer, RotatedPosition> finalPositionSupplier)
    {
        for (final Vector3Di corner : cuboid.getCorners())
        {
            final float radius = radiusSupplier.apply(corner.x(), corner.y(), corner.z());
            final RotatedPosition finalPosition = finalPositionSupplier.apply(corner.x(), corner.y(), corner.z());
            target.add(new MarkerBlock(radius, finalPosition, corner));
        }
    }

    private static Cuboid getRegion(List<IAnimatedBlock> blocks)
    {
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double zMin = Double.MAX_VALUE;

        double xMax = Double.MIN_VALUE;
        double yMax = Double.MIN_VALUE;
        double zMax = Double.MIN_VALUE;

        for (final IAnimatedBlock animatedBlock : blocks)
        {
            final Vector3Dd pos = animatedBlock.getCurrentPosition();
            final double x = pos.x();
            final double y = pos.y();
            final double z = pos.z();

            if (x < xMin)
                xMin = pos.x();
            else if (x > xMax)
                xMax = pos.x();

            if (y < yMin)
                yMin = pos.y();
            else if (y > yMax)
                yMax = pos.y();

            if (z < zMin)
                zMin = pos.z();
            else if (z > zMax)
                zMax = pos.z();
        }

        return Cuboid.of(
            new Vector3Dd(xMin, yMin, zMin),
            new Vector3Dd(xMax, yMax, zMax), Cuboid.RoundingMode.NEAREST);
    }

    private static final class MarkerBlock implements IAnimatedBlock
    {
        @Getter
        private final float radius;

        @Getter
        private final RotatedPosition startPosition;

        @Getter
        private final RotatedPosition finalPosition;

        private volatile RotatedPosition currentTarget;
        private volatile RotatedPosition previousTarget;

        MarkerBlock(float radius, RotatedPosition finalPosition, Vector3Di position)
        {
            this.radius = radius;
            this.finalPosition = finalPosition;
            this.startPosition = new RotatedPosition(position);
            this.currentTarget = this.startPosition;
            this.previousTarget = this.currentTarget;
        }

        @Override
        public Vector3Dd getCurrentPosition()
        {
            return currentTarget.position();
        }

        @Override
        public Vector3Dd getPreviousPosition()
        {
            return previousTarget.position();
        }

        @Override
        public Vector3Dd getPreviousTarget()
        {
            return previousTarget.position();
        }

        private void cycleTargets(RotatedPosition newTarget)
        {
            previousTarget = currentTarget;
            currentTarget = newTarget;
        }

        @Override
        public void moveToTarget(RotatedPosition target)
        {
            cycleTargets(target);
        }

        @Override
        public synchronized Vector3Dd getPosition()
        {
            return getCurrentPosition();
        }

        @Override
        public boolean isAlive()
        {
            return false;
        }

        @Override
        public int getTicksLived()
        {
            return 0;
        }

        @Override
        public IWorld getWorld()
        {
            throw new UnsupportedOperationException("Marker blocks do not have a world!");
        }

        @Override
        public IAnimatedBlockData getAnimatedBlockData()
        {
            throw new UnsupportedOperationException("Marker blocks do not have animated block data!");
        }

        @Override
        public void spawn()
        {
            throw new UnsupportedOperationException("Marker blocks cannot be spawned!");
        }

        @Override
        public void respawn()
        {
            throw new UnsupportedOperationException("Marker blocks cannot be respawned!");
        }

        @Override
        public void kill()
        {
            throw new UnsupportedOperationException("Marker blocks cannot be killed!");
        }

        @Override
        public ILocation getLocation()
        {
            throw new UnsupportedOperationException("Marker blocks do not have a world!");
        }
    }
}
