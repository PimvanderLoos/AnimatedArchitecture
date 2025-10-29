package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyAdapterRegistry;

/**
 * Initializer that registers default GUI adapters for the built-in properties.
 */
@Singleton
public final class PropertyAdapterInitializer
{
    @Inject
    public PropertyAdapterInitializer(
        PropertyAdapterRegistry registry,
        PropertyAdapterOpenStatus adapterOpenStatus
    )
    {
        registry.registerAdapters(
            adapterOpenStatus
        );
    }


//    private void registerOpenStatus()
//    {
//        registry.registerAdapter(
//            PropertyAdapterSpigotOld.<Boolean>builder()
//                .property(Property.OPEN_STATUS)
//                .materialFunction(value ->
//                {
//                    final boolean isOpen = Boolean.TRUE.equals(value.value());
//
//
//                    Boolean.TRUE.equals(value.value()) ? materialTrue : materialFalse
//                })
//                .localizer(localizer)
//                .build()
//        );
//    }
//
//    public void initialize()
//    {
//        registerOpenStatus();
//
//        registerBooleanProperty(Property.OPEN_STATUS, Material.OAK_DOOR, Material.DARK_OAK_DOOR);
//        registerIntegerProperty(Property.BLOCKS_TO_MOVE, Material.PISTON);
//        registerIntegerProperty(Property.QUARTER_CIRCLES, Material.COMPASS);
//        registerDoubleProperty(Property.ANIMATION_SPEED_MULTIPLIER, Material.CLOCK);
//        registerVector3DiProperty(Property.ROTATION_POINT, Material.ENDER_EYE);
//        registerRedstoneModeProperty(Property.REDSTONE_MODE, Material.REDSTONE);
//    }
//
//    private void registerBooleanProperty(Property<Boolean> property, Material materialTrue, Material materialFalse)
//    {
//        registry.registerAdapter(
//            PropertyAdapterSpigotOld.<Boolean>builder()
//                .property(property)
//                .materialFunction(value -> Boolean.TRUE.equals(value.value()) ? materialTrue : materialFalse)
//                .localizer(localizer)
//                .build()
//        );
//    }
//
//    private void registerIntegerProperty(Property<Integer> property, Material material)
//    {
//        registry.registerAdapter(
//            PropertyAdapterSpigotOld.<Integer>builder()
//                .property(property)
//                .material(material)
//                .localizer(localizer)
//                .build()
//        );
//    }
//
//    private void registerDoubleProperty(Property<Double> property, Material material)
//    {
//        registry.registerAdapter(
//            PropertyAdapterSpigotOld.<Double>builder()
//                .property(property)
//                .material(material)
//                .localizer(localizer)
//                .build()
//        );
//    }
//
//    private void registerVector3DiProperty(
//        Property<nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di> property,
//        Material material)
//    {
//        registry.registerAdapter(
//            PropertyAdapterSpigotOld.<nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di>builder()
//                .property(property)
//                .material(material)
//                .localizer(localizer)
//                .build()
//        );
//    }
//
//    private void registerRedstoneModeProperty(
//        Property<nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode> property,
//        Material material)
//    {
//        registry.registerAdapter(
//            PropertyAdapterSpigotOld.<nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode>builder()
//                .property(property)
//                .material(material)
//                .localizer(localizer)
//                .build()
//        );
//    }
}
