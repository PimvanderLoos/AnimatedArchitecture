package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IHighlightedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

public class AnimatedHighlightedBlock implements IAnimatedBlock
{
    private static final IAnimatedBlockData ANIMATED_BLOCK_DATA = new PreviewAnimatedBlockData();

    private volatile int processedTicks = 0;
    private volatile @Nullable IHighlightedBlock highlightedBlock;

    private final ILocationFactory locationFactory;
    private final HighlightedBlockSpawner highlightedBlockSpawner;
    @Getter
    private final IWorld world;
    private final float startRadius;
    private final Color color;
    private final IPlayer player;
    private final RotatedPosition startPosition;

    private volatile RotatedPosition previousTarget;
    private volatile RotatedPosition currentTarget;

    public AnimatedHighlightedBlock(
        ILocationFactory locationFactory, HighlightedBlockSpawner highlightedBlockSpawner, IWorld world, IPlayer player,
        RotatedPosition position, float startRadius, Color color)
    {
        this.locationFactory = locationFactory;
        this.highlightedBlockSpawner = highlightedBlockSpawner;
        this.world = world;
        this.player = player;
        this.startPosition = position;
        this.currentTarget = previousTarget = startPosition;
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

        final IHighlightedBlock currentBlock = this.highlightedBlock;
        if (currentBlock != null)
            currentBlock.moveToTarget(target);
    }

    @Override
    public void spawn()
    {
        this.highlightedBlock = highlightedBlockSpawner
            .builder()
            .forPlayer(player)
            .inWorld(world)
            .atPosition(currentTarget)
            .withColor(color)
            .spawn()
            .orElse(null);
    }

    @Override
    public void kill()
    {
        final @Nullable IHighlightedBlock currentBlock = this.highlightedBlock;
        if (currentBlock != null)
        {
            currentBlock.kill();
            highlightedBlock = null;
        }
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
        return startPosition;
    }

    @Override
    public RotatedPosition getFinalPosition()
    {
        return startPosition;
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
