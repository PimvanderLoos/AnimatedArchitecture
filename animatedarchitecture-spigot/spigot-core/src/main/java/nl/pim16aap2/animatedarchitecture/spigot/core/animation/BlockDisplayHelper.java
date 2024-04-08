package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.AnimatedBlockRecoveryDataType;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.IAnimatedBlockRecoveryData;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.RecoveryFailureException;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper class for BlockDisplay entities.
 */
@Flogger
@Singleton
public final class BlockDisplayHelper
{
    private final Vector3f ONE_VECTOR = new Vector3f(1F, 1F, 1F);
    private final Vector3f HALF_VECTOR_POSITIVE = new Vector3f(0.5F, 0.5F, 0.5F);
    private final Vector3f HALF_VECTOR_NEGATIVE = new Vector3f(-0.5F, -0.5F, -0.5F);

    private final NamespacedKey RECOVERY_KEY;

    @Inject BlockDisplayHelper(JavaPlugin plugin)
    {
        RECOVERY_KEY = new NamespacedKey(plugin, Constants.ANIMATED_ARCHITECTURE_ENTITY_RECOVERY_KEY);
    }

    /**
     * Attempts to recover an animated block from an entity.
     * <p>
     * If the entity is not an animated block (or null), this method does nothing.
     * <p>
     * If the entity is an animated block, this method will attempt to perform a recovery action by calling
     * {@link IAnimatedBlockRecoveryData#recover()}. If the recovery action is successful, the entity will be removed.
     *
     * @param entity
     */
    public void recoverAnimatedBlock(@Nullable Entity entity)
    {
        if (entity == null)
            return;

        final IAnimatedBlockRecoveryData recoveryData = entity.getPersistentDataContainer().get(
            RECOVERY_KEY,
            AnimatedBlockRecoveryDataType.INSTANCE);

        if (recoveryData == null)
            return;

        try
        {
            if (recoveryData.recover())
                log.atWarning().log(
                    "Recovered animated block with recovery data '%s'! " +
                        "This is not intended behavior, please contact the author(s) of this plugin!",
                    recoveryData);
            entity.remove();
        }
        catch (RecoveryFailureException e)
        {
            log.atSevere().withCause(e).log(
                "Failed to recover animated block '%s' from recovery: '%s'",
                entity, recoveryData);
        }
    }

    /**
     * Spawns a new BlockDisplay entity.
     *
     * @param executor
     *     The executor to assert that this method is called on the main thread.
     * @param bukkitWorld
     *     The world to spawn the entity in.
     * @param spawnPose
     *     The pose to use for the entity.
     * @param blockData
     *     The block data of the entity to spawn.
     * @return The spawned entity.
     */
    BlockDisplay spawn(
        IAnimatedBlockRecoveryData recoveryData,
        IExecutor executor,
        World bukkitWorld,
        RotatedPosition spawnPose,
        BlockData blockData)
    {
        executor.assertMainThread("Animated blocks must be spawned on the main thread!");

        final Vector3Dd pos = spawnPose.position().floor();
        final Location loc = new Location(bukkitWorld, pos.x(), pos.y(), pos.z());

        final BlockDisplay newEntity = bukkitWorld.spawn(loc, BlockDisplay.class);
        newEntity.setBlock(blockData);

        newEntity.getPersistentDataContainer().set(
            RECOVERY_KEY,
            AnimatedBlockRecoveryDataType.INSTANCE,
            recoveryData);

        newEntity.setInterpolationDuration(1);
        return newEntity;
    }

    /**
     * Moves an entity from the start position to the target position.
     * <p>
     * This method updates the transformation of the entity to move it from the start position to the target position.
     *
     * @param entity
     *     The entity to move. If null, this method does nothing.
     * @param startPosition
     *     The start position of the entity. This is original position the entity was created at.
     * @param target
     *     The target position of the entity.
     */
    public void moveToTarget(
        @Nullable BlockDisplay entity, RotatedPosition startPosition, RotatedPosition target)
    {
        if (entity == null)
            return;
        updateTransformation(entity, startPosition, target);
    }

    private void updateTransformation(BlockDisplay entity, RotatedPosition startPosition, RotatedPosition target)
    {
        final Vector3Dd delta = target.position().subtract(startPosition.position());
        entity.setTransformation(getTransformation(startPosition, target.rotation(), delta));
    }

    private Transformation getTransformation(RotatedPosition startPosition, Vector3Dd rotation, Vector3Dd delta)
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

    private Vector3f to3f(IVector3D vec)
    {
        return new Vector3f((float) vec.xD(), (float) vec.yD(), (float) vec.zD());
    }

    /**
     * Creates a quaternion from roll, pitch and yaw.
     *
     * @param roll
     *     The roll. Rotation around the z-axis measured in radians.
     * @param pitch
     *     The pitch. Rotation around the x-axis measured in radians.
     * @param yaw
     *     The yaw. Rotation around the y-axis measured in radians.
     * @return The quaternion representing the rotation.
     */
    public Quaternionf fromRollPitchYaw(float roll, float pitch, float yaw)
    {
        return new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
    }
}
