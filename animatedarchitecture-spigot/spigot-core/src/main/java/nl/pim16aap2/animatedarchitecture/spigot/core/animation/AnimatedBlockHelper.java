package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.animation.recovery.AnimationRunManager;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.AnimatedBlockRecoveryDataType;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.IAnimatedBlockRecoveryData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    private final AnimationRunManager animationRunManager;

    @Inject
    AnimatedBlockHelper(JavaPlugin plugin, AnimationRunManager animationRunManager)
    {
        recoveryKey = new NamespacedKey(plugin, Constants.ANIMATED_ARCHITECTURE_ENTITY_RECOVERY_KEY);
        this.animationRunManager = animationRunManager;
    }

    /**
     * Attempts to recover animated blocks from a bounded set of entities.
     * <p>
     * Each entity is handled independently so one failed recovery cannot prevent recovery of other entities in the same
     * chunk.
     *
     * @param entities
     *     The entities to inspect.
     */
    public void recoverAnimatedBlocks(Iterable<Entity> entities)
    {
        final Map<UUID, Integer> recoveredBlocks = new HashMap<>();
        for (final Entity entity : entities)
            recoverAnimatedBlock(entity).ifPresent(uuid -> recoveredBlocks.merge(uuid, 1, Integer::sum));

        recoveredBlocks.forEach((uuid, count) -> animationRunManager.recordRecoveredBlocks(
            uuid,
            count,
            "Recovered orphaned animated block entity/entities."
        ));
    }

    private Optional<UUID> recoverAnimatedBlock(@Nullable Entity entity)
    {
        if (entity == null)
            return Optional.empty();

        final IAnimatedBlockRecoveryData recoveryData = entity.getPersistentDataContainer().get(
            recoveryKey,
            AnimatedBlockRecoveryDataType.INSTANCE
        );

        if (recoveryData == null)
            return Optional.empty();

        try
        {
            if (!recoveryData.recover())
            {
                log.atDebug().log("No recovery action required for data '%s'", recoveryData);
                entity.remove();
                return Optional.empty();
            }

            entity.remove();
            final Optional<UUID> animationRunUuid = getAnimationRunUuid(recoveryData);
            if (animationRunUuid.isEmpty())
            {
                log.atWarn().log(
                    "Recovered animated block without animation-run context. Recovery data: '%s'",
                    recoveryData
                );
            }
            return animationRunUuid;
        }
        catch (Exception e)
        {
            log.atError().withCause(e).log(
                "Failed to recover animated block '%s' from recovery: '%s'",
                entity,
                recoveryData
            );
            return Optional.empty();
        }
    }

    private Optional<UUID> getAnimationRunUuid(IAnimatedBlockRecoveryData recoveryData)
    {
        if (recoveryData instanceof IAnimatedBlockRecoveryData.AnimatedBlockRecoveryData data)
            return Optional.ofNullable(data.animationRunUuid());
        return Optional.empty();
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
