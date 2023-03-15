package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
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
    private static final Vector3f ONE_VECTOR = new Vector3f(1F, 1F, 1F);
    private static final Vector3f HALF_VECTOR_POSITIVE = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final Vector3f HALF_VECTOR_NEGATIVE = new Vector3f(-0.5F, -0.5F, -0.5F);

    @Getter
    private final IWorld world;
    @Getter
    private final World bukkitWorld;
    private final SimpleBlockData blockData;

    @Getter
    private final RotatedPosition startPosition;
    @Getter
    private final RotatedPosition finalPosition;
    @Getter
    private final float radius;
    @Getter
    private final boolean onEdge;

    @GuardedBy("this")
    private RotatedPosition previousTarget;

    @GuardedBy("this")
    private RotatedPosition currentTarget;

    private volatile @Nullable BlockDisplay blockDisplay;

    public AnimatedBlockDisplay(
        IExecutor executor, RotatedPosition startPosition, IWorld world,
        RotatedPosition finalPosition, boolean onEdge, float radius)
    {
        this.world = world;
        this.bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "Bukkit World");
        this.onEdge = onEdge;

        this.startPosition = startPosition;
        this.finalPosition = finalPosition;
        this.radius = radius;

        this.currentTarget = this.startPosition;

        final Vector3Dd pos = this.startPosition.position();
        this.blockData = new SimpleBlockData(executor, bukkitWorld, pos.floor().toInteger());
    }

    @Override
    public void spawn()
    {
        final Vector3Dd pos = currentTarget.position().floor();
        final Location loc = new Location(bukkitWorld, pos.x(), pos.y(), pos.z());
        final BlockDisplay newEntity = bukkitWorld.spawn(loc, BlockDisplay.class);
        blockDisplay = newEntity;

        newEntity.setBlock(blockData.getBlockData());
        newEntity.setCustomName(Constants.ANIMATED_ARCHITECTURE_ENTITY_NAME);
        newEntity.setCustomNameVisible(false);
        newEntity.setInterpolationDuration(1);
        newEntity.setViewRange(2.5F);
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
        final Vector3Dd rads = rotation.subtract(startPosition.rotation()).toRadians();
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

    private void cycleTargets(RotatedPosition newTarget)
    {
        this.previousTarget = currentTarget;
        this.currentTarget = newTarget;
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
    public Material getMaterial()
    {
        return this.blockData.getBlockData().getMaterial();
    }
}
