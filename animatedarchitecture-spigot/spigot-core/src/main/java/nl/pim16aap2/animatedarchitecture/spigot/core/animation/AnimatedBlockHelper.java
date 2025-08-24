package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import com.google.common.flogger.LazyArgs;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.AnimatedBlockRecoveryDataType;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.IAnimatedBlockRecoveryData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Helper class for animated blocks.
 */
@CustomLog
@Singleton
public final class AnimatedBlockHelper
{
    /**
     * The key used to store the recovery data in the entity's persistent data container.
     */
    private final NamespacedKey recoveryKey;

    @Inject
    AnimatedBlockHelper(JavaPlugin plugin)
    {
        recoveryKey = new NamespacedKey(plugin, Constants.ANIMATED_ARCHITECTURE_ENTITY_RECOVERY_KEY);
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
     *     The entity for which to attempt recovery.
     */
    public void recoverAnimatedBlock(@Nullable Entity entity)
    {
        if (entity == null)
            return;

        final IAnimatedBlockRecoveryData recoveryData = entity.getPersistentDataContainer().get(
            recoveryKey,
            AnimatedBlockRecoveryDataType.INSTANCE);

        if (recoveryData == null)
            return;

        log.atTrace().log(
            "Attempting to recover animated block with recovery data '%s'",
            LazyArgs.lazy(
                () -> entity.getPersistentDataContainer().get(recoveryKey, AnimatedBlockRecoveryDataType.STRING))
        );

        try
        {
            if (recoveryData.recover())
                log.atWarn().log(
                    "Recovered animated block with recovery data '%s'! " +
                        "This is not intended behavior, please contact the author(s) of this plugin!",
                    recoveryData
                );
            else
                log.atDebug().log("No recovery action required for data '%s'", recoveryData);

            entity.remove();
        }
        catch (Exception e)
        {
            log.atError().withCause(e).log(
                "Failed to recover animated block '%s' from recovery: '%s'",
                entity,
                recoveryData
            );
        }
    }

    /**
     * Sets the recovery data for an animated block entity.
     *
     * @param entity
     *     The entity to set the recovery data for.
     * @param recoveryData
     *     The recovery data to set for the entity.
     */
    public void setRecoveryData(BlockDisplay entity, @Nullable IAnimatedBlockRecoveryData recoveryData)
    {
        entity.getPersistentDataContainer().set(
            recoveryKey,
            AnimatedBlockRecoveryDataType.INSTANCE,
            Objects.requireNonNullElse(recoveryData, IAnimatedBlockRecoveryData.EMPTY)
        );
    }
}
