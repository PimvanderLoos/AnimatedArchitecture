package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyGuiAdapterRegistry;

/**
 * Initializer that registers default GUI adapters for the built-in properties.
 * <p>
 * This class should be instantiated only once at application startup.
 * <p>
 * Use the {@link PropertyGuiAdapterRegistry} directly to register additional adapters.
 */
@Singleton
public final class PropertyGuiAdapterInitializer
{
    @Inject
    PropertyGuiAdapterInitializer(
        PropertyGuiAdapterRegistry registry,
        PropertyGuiAdapterAnimationSpeedMultiplier adapterAnimationSpeedMultiplier,
        PropertyGuiAdapterBlocksToMove adapterBlocksToMove,
        PropertyGuiAdapterOpenStatus adapterOpenStatus,
        PropertyGuiAdapterQuarterCircles adapterQuarterCircles,
        PropertyGuiAdapterRedstoneMode adapterRedstoneMode,
        PropertyGuiAdapterRotationPoint adapterRotationPoint
    )
    {
        registry.registerGuiAdapters(
            adapterAnimationSpeedMultiplier,
            adapterBlocksToMove,
            adapterOpenStatus,
            adapterQuarterCircles,
            adapterRedstoneMode,
            adapterRotationPoint
        );
    }
}
