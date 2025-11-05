package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyAdapterRegistry;

/**
 * Initializer that registers default GUI adapters for the built-in properties.
 * <p>
 * This class should be instantiated only once at application startup.
 * <p>
 * Use the {@link PropertyAdapterRegistry} directly to register additional adapters.
 */
@Singleton
public final class PropertyAdapterInitializer
{
    @Inject
    PropertyAdapterInitializer(
        PropertyAdapterRegistry registry,
        PropertyAdapterAnimationSpeedMultiplier adapterAnimationSpeedMultiplier,
        PropertyAdapterBlocksToMove adapterBlocksToMove,
        PropertyAdapterOpenStatus adapterOpenStatus,
        PropertyAdapterQuarterCircles adapterQuarterCircles,
        PropertyAdapterRedstoneMode adapterRedstoneMode,
        PropertyAdapterRotationPoint adapterRotationPoint
    )
    {
        registry.registerAdapters(
            adapterAnimationSpeedMultiplier,
            adapterBlocksToMove,
            adapterOpenStatus,
            adapterQuarterCircles,
            adapterRedstoneMode,
            adapterRotationPoint
        );
    }
}
