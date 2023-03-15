package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IAnimatedBlockSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.LocationSpigot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.concurrent.GuardedBy;

public class AnimatedBlockDisplay implements IAnimatedBlockSpigot
{
    private static final Vector3f ZERO_VECTOR = new Vector3f(0, 0, 0);
    private static final Vector3f ONE_VECTOR = new Vector3f(1F, 1F, 1F);
    private static final Vector3f HALF_VECTOR_POSITIVE = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final Vector3f HALF_VECTOR_NEGATIVE = new Vector3f(-0.5F, -0.5F, -0.5F);

    private final IExecutor executor;
    private final RotatedPosition startRotatedPosition;
    private final IWorld world;
    private final World bukkitWorld;
    private final SimpleBlockData blockData;

    private final Vector3Dd rotationPoint;
    private final RotatedPosition startPosition;
    private final RotatedPosition finalPosition;
    private final float startAngle;
    private final float radius;

    @GuardedBy("this")
    private RotatedPosition previousTarget;

    @GuardedBy("this")
    private RotatedPosition currentTarget;

    private volatile @Nullable BlockDisplay blockDisplay;

    public AnimatedBlockDisplay(
        IExecutor executor, RotatedPosition startRotatedPosition, IWorld world, Vector3Dd rotationPoint,
        RotatedPosition startPosition, RotatedPosition finalPosition, float startAngle, float radius)
    {
        this.executor = executor;
        this.startRotatedPosition = startRotatedPosition;
        this.world = world;
        this.bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "Bukkit World");
        this.rotationPoint = rotationPoint;

        this.startPosition = startPosition;
        this.finalPosition = finalPosition;
        this.startAngle = startAngle;
        this.radius = radius;

        this.currentTarget = startPosition;

        final Vector3Dd pos = startPosition.position();
        this.blockData = new SimpleBlockData(
            executor, bukkitWorld, new Vector3Di((int) pos.x(), (int) pos.y(), (int) pos.z()));
    }

    @Override
    public void spawn()
    {
        final Vector3Dd pos = currentTarget.position();
        final Location loc = new Location(bukkitWorld, pos.x() - 0.5, pos.y(), pos.z() - 0.5);
        final BlockDisplay newEntity = bukkitWorld.spawn(loc, BlockDisplay.class);
        blockDisplay = newEntity;

        newEntity.setBlock(blockData.getBlockData());
        newEntity.setCustomName(Constants.ANIMATED_ARCHITECTURE_ENTITY_NAME);
        newEntity.setCustomNameVisible(false);
//        newEntity.setBrightness(blockData.getBrightness());
    }

    @Override
    public void respawn()
    {
        kill();
        spawn();
    }

    @Override
    public void kill()
    {
        final BlockDisplay entity = blockDisplay;
        if (entity != null)
            entity.remove();
    }

    @Override
    public boolean isAlive()
    {
        return blockDisplay != null;
    }

    @Override
    public IAnimatedBlockData getAnimatedBlockData()
    {
        return this.blockData;
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

    @Override
    public IWorld getWorld()
    {
        return world;
    }

    private void cycleTargets(RotatedPosition newTarget)
    {
        this.previousTarget = currentTarget;
        this.currentTarget = newTarget;
    }

    @Override
    public void moveToTarget(RotatedPosition target, int ticksRemaining)
    {
        updateTransformation(target);
        cycleTargets(target);
    }

    private void updateTransformation(RotatedPosition target)
    {
        final @Nullable BlockDisplay entity = this.blockDisplay;
        if (entity == null)
            return;

        final Vector3Dd delta = target.position().subtract(startPosition.position());
        entity.setTransformation(getTransformation(target.rotation(), delta));
    }

    private Transformation getTransformation(Vector3Dd rotation, Vector3Dd delta)
    {
        final Vector3Dd rads = rotation.subtract(startRotatedPosition.rotation()).toRadians();
        final float roll = (float) rads.x();
        final float pitch = (float) rads.y();
        final float yaw = (float) rads.z();

        Matrix4f transformation = new Matrix4f()
            .translate(HALF_VECTOR_NEGATIVE)
            .rotate(fromRollPitchYaw(roll, pitch, yaw))
            .translate(HALF_VECTOR_POSITIVE);

        final Quaternionf leftRotation = transformation.getUnnormalizedRotation(new Quaternionf());
        final Vector3f translation = to3f(delta).sub(transformation.getTranslation(new Vector3f()));

        return new Transformation(translation, leftRotation, ONE_VECTOR, new Quaternionf());
    }

    private static Vector3f to3f(IVector3D vec)
    {
        return new Vector3f((float) vec.xD(), (float) vec.yD(), (float) vec.zD());
    }

    public static Quaternionf fromRollPitchYaw(float roll, float pitch, float yaw)
    {
        return new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
    }

    @Override
    public boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, TeleportMode teleportMode)
    {
        return false;
    }

    @Override
    public void setVelocity(Vector3Dd vector)
    {
    }

    @Override
    public int getTicksLived()
    {
        final @Nullable Entity entity = this.blockDisplay;
        return entity == null ? -1 : entity.getTicksLived();
    }

    @Override
    public ILocation getLocation()
    {
        final Vector3Dd positionSnapshot = getCurrentPosition();
        return new LocationSpigot(bukkitWorld, positionSnapshot.x(), positionSnapshot.y(), positionSnapshot.z());
    }

    @Override
    public Vector3Dd getPosition()
    {
        return getCurrentPosition();
    }

    @Override
    public RotatedPosition getStartPosition()
    {
        return startPosition;
    }

    @Override
    public RotatedPosition getFinalPosition()
    {
        return finalPosition;
    }

    @Override
    public float getStartAngle()
    {
        return startAngle;
    }

    @Override
    public float getRadius()
    {
        return radius;
    }

    @Override
    public boolean isOnEdge()
    {
        return false;
    }

    @Override
    public World getBukkitWorld()
    {
        return bukkitWorld;
    }

    @Override
    public Material getMaterial()
    {
        return this.blockData.getBlockData().getMaterial();
    }
}
