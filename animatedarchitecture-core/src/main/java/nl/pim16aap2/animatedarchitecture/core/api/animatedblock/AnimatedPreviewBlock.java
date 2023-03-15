package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.api.GlowingBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.IGlowingBlock;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class AnimatedPreviewBlock implements IAnimatedBlock
{
    private static final IAnimatedBlockData ANIMATED_BLOCK_DATA = new PreviewAnimatedBlockData();

    /**
     * The lifetime of a glowing block (in milliseconds).
     * <p>
     * Glowing blocks will be respawned after e
     */
    private static final int LIFETIME = 60_000;

    private volatile int processedTicks = 0;
    private volatile @Nullable IGlowingBlock glowingBlock;

    private final ILocationFactory locationFactory;
    private final GlowingBlockSpawner glowingBlockSpawner;
    @Getter
    private final IWorld world;
    private final float startRadius;
    private final Color color;
    private final IPlayer player;
    private final Vector3Dd startPosition;
    private final Vector3Dd finalPosition;

    private volatile RotatedPosition previousTarget;
    private volatile RotatedPosition currentTarget;

    public AnimatedPreviewBlock(
        ILocationFactory locationFactory, GlowingBlockSpawner glowingBlockSpawner, IWorld world, IPlayer player,
        Vector3Dd position, Vector3Dd finalPosition, float startRadius, Color color)
    {
        this.locationFactory = locationFactory;
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.world = world;
        this.player = player;
        this.startPosition = position;
        this.finalPosition = finalPosition;
        this.currentTarget = previousTarget = new RotatedPosition(position);
        this.startRadius = startRadius;
        this.color = color;
    }

    private synchronized void cycleTargets(RotatedPosition newTarget)
    {
        previousTarget = currentTarget;
        currentTarget = newTarget;
    }

    @Override
    public boolean isAlive()
    {
        return true;
    }

    @Override
    public IAnimatedBlockData getAnimatedBlockData()
    {
        return ANIMATED_BLOCK_DATA;
    }

    @Override
    public synchronized Vector3Dd getCurrentPosition()
    {
        return currentTarget.position();
    }

    @Override
    public synchronized Vector3Dd getPreviousPosition()
    {
        return previousTarget.position();
    }

    @Override
    public synchronized Vector3Dd getPreviousTarget()
    {
        return previousTarget.position();
    }

    @Override
    public ILocation getLocation()
    {
        return getCurrentPosition().toLocation(locationFactory, getWorld());
    }

    @Override
    public synchronized Vector3Dd getPosition()
    {
        return currentTarget.position();
    }

    @Override
    public void moveToTarget(RotatedPosition target)
    {
        final int currentTicks = processedTicks;
        processedTicks = currentTicks + 1;

        cycleTargets(target);

        if (currentTicks % AnimatedPreviewBlock.LIFETIME == 0)
            respawn();

        final IGlowingBlock currentBlock = this.glowingBlock;
        if (currentBlock != null)
            currentBlock.teleport(target);
    }

    @Override
    public void spawn()
    {
        this.glowingBlock = glowingBlockSpawner
            .builder()
            .forPlayer(player)
            // add 100ms to have a small overlap when resetting the glowing block.
            .forDuration(Duration.ofMillis(LIFETIME + 100))
            .inWorld(world)
            .atPosition(currentTarget)
            .withColor(color)
            .spawn()
            .orElse(null);
    }

    @Override
    public void kill()
    {
        final @Nullable IGlowingBlock currentBlock = this.glowingBlock;
        if (currentBlock != null)
            currentBlock.kill();
    }

    @Override
    public void respawn()
    {
        kill();
        spawn();
    }

    @Override
    public int getTicksLived()
    {
        return processedTicks;
    }

    @Override
    public RotatedPosition getStartPosition()
    {
        return new RotatedPosition(startPosition);
    }

    @Override
    public RotatedPosition getFinalPosition()
    {
        return new RotatedPosition(finalPosition);
    }

    @Override
    public double getStartX()
    {
        return startPosition.x();
    }

    @Override
    public double getStartY()
    {
        return startPosition.y();
    }

    @Override
    public double getStartZ()
    {
        return startPosition.z();
    }

    @Override
    public float getRadius()
    {
        return startRadius;
    }

    @Override
    public boolean isOnEdge()
    {
        return false;
    }

    private static final class PreviewAnimatedBlockData implements IAnimatedBlockData
    {
        @Override
        public boolean canRotate()
        {
            return false;
        }

        @Override
        public boolean rotateBlock(MovementDirection movementDirection, int times)
        {
            return false;
        }

        @Override
        public void putBlock(IVector3D loc)
        {
        }

        @Override
        public void deleteOriginalBlock(boolean applyPhysics)
        {
        }
    }
}
