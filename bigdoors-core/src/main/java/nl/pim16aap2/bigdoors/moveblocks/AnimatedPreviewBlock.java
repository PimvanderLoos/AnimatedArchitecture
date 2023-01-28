package nl.pim16aap2.bigdoors.moveblocks;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.time.Duration;

public class AnimatedPreviewBlock implements IAnimatedBlock
{
    /**
     * We do not want to move this type every tick, so we only update it once in every MOVEMENT_RATE ticks.
     */
    private static final int MOVEMENT_RATE = 5;

    private static final IAnimatedBlockData ANIMATED_BLOCK_DATA = new PreviewAnimatedBlockData();

    private int movementTicks = -1;
    private final IPLocationFactory locationFactory;
    private final GlowingBlockSpawner glowingBlockSpawner;
    @Getter
    private final IPWorld world;
    private final float startAngle;
    private final float startRadius;
    private final PColor color;
    private final IPPlayer player;
    private final Vector3Dd startPosition;
    private final Vector3Dd finalPosition;

    private volatile Vector3Dd previousTarget;
    private volatile Vector3Dd currentTarget;

    public AnimatedPreviewBlock(
        IPLocationFactory locationFactory, GlowingBlockSpawner glowingBlockSpawner, IPWorld world, IPPlayer player,
        Vector3Dd position, Vector3Dd finalPosition, float startAngle, float startRadius, PColor color)
    {
        this.locationFactory = locationFactory;
        this.glowingBlockSpawner = glowingBlockSpawner;
        this.world = world;
        this.player = player;
        this.startPosition = position;
        this.finalPosition = finalPosition;
        this.currentTarget = previousTarget = position;
        this.startAngle = startAngle;
        this.startRadius = startRadius;
        this.color = color;
    }

    private synchronized void cycleTargets(Vector3Dd newTarget)
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
        return currentTarget;
    }

    @Override
    public synchronized Vector3Dd getPreviousPosition()
    {
        return previousTarget;
    }

    @Override
    public synchronized Vector3Dd getPreviousTarget()
    {
        return previousTarget;
    }

    @Override
    public IPLocation getPLocation()
    {
        return getCurrentPosition().toLocation(locationFactory, getWorld());
    }

    @Override
    public synchronized Vector3Dd getPosition()
    {
        return currentTarget;
    }

    @Override
    public void moveToTarget(Vector3Dd target, int ticksRemaining)
    {
        if (++movementTicks % MOVEMENT_RATE != 0)
            return;

        cycleTargets(target);

        glowingBlockSpawner
            .builder()
            .forPlayer(player)
            .forDuration(Duration.ofMillis(500))
            .inWorld(world)
            .atPosition(target)
            .withColor(color)
            .build();
    }

    @Override
    public boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, IAnimatedBlock.TeleportMode teleportMode)
    {
        return false;
    }

    @Override
    public void setVelocity(Vector3Dd vector)
    {
    }

    @Override
    public void spawn()
    {
    }

    @Override
    public void respawn()
    {
    }

    @Override
    public void kill()
    {
    }

    @Override
    public int getTicksLived()
    {
        return movementTicks;
    }

    @Override
    public Vector3Dd getStartPosition()
    {
        return startPosition;
    }

    @Override
    public Vector3Dd getFinalPosition()
    {
        return finalPosition;
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
    public float getStartAngle()
    {
        return startAngle;
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
        public boolean rotateBlock(MovementDirection movementDirection)
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
