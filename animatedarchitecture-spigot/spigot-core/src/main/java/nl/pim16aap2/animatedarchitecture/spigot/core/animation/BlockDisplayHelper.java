package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class BlockDisplayHelper
{
    private static final Vector3f ONE_VECTOR = new Vector3f(1F, 1F, 1F);
    private static final Vector3f HALF_VECTOR_POSITIVE = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final Vector3f HALF_VECTOR_NEGATIVE = new Vector3f(-0.5F, -0.5F, -0.5F);

    private BlockDisplayHelper()
    {
    }

    static BlockDisplay spawn(IExecutor executor, World bukkitWorld, RotatedPosition spawnPose, BlockData blockData)
    {
        executor.assertMainThread("Animated blocks must be spawned on the main thread!");
        final Vector3Dd pos = spawnPose.position().floor();
        final Location loc = new Location(bukkitWorld, pos.x(), pos.y(), pos.z());

        final BlockDisplay newEntity = bukkitWorld.spawn(loc, BlockDisplay.class);
        newEntity.setBlock(blockData);
        newEntity.setCustomName(Constants.ANIMATED_ARCHITECTURE_ENTITY_NAME);
        newEntity.setCustomNameVisible(false);
        newEntity.setInterpolationDuration(1);
        return newEntity;
    }

    public static void moveToTarget(
        @Nullable BlockDisplay entity, RotatedPosition startPosition, RotatedPosition target)
    {
        if (entity == null)
            return;
        updateTransformation(entity, startPosition, target);
    }

    private static void updateTransformation(BlockDisplay entity, RotatedPosition startPosition, RotatedPosition target)
    {
        final Vector3Dd delta = target.position().subtract(startPosition.position());
        entity.setTransformation(getTransformation(startPosition, target.rotation(), delta));
    }

    private static Transformation getTransformation(RotatedPosition startPosition, Vector3Dd rotation, Vector3Dd delta)
    {
        final Vector3Dd rads = rotation.subtract(startPosition.rotation()).toRadians();
        final float roll = (float) rads.x();
        final float pitch = (float) rads.y();
        final float yaw = (float) rads.z();

        final Matrix4f transformation = new Matrix4f()
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
}
