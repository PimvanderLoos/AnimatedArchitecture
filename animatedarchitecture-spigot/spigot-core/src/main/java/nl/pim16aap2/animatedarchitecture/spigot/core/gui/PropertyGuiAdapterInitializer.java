package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.bukkit.Material;

/**
 * Initializer that registers default GUI adapters for the built-in properties.
 */
@Singleton
public final class PropertyGuiAdapterInitializer
{
    private final PropertyGuiAdapterRegistrySpigot registry;
    private final ILocalizer localizer;

    @Inject
    public PropertyGuiAdapterInitializer(
        PropertyGuiAdapterRegistrySpigot registry,
        ILocalizer localizer
    )
    {
        this.registry = registry;
        this.localizer = localizer;
    }

    public void initialize()
    {
        registerBooleanProperty(Property.OPEN_STATUS, Material.OAK_DOOR);
        registerIntegerProperty(Property.BLOCKS_TO_MOVE, Material.PISTON);
        registerIntegerProperty(Property.QUARTER_CIRCLES, Material.COMPASS);
        registerDoubleProperty(Property.ANIMATION_SPEED_MULTIPLIER, Material.CLOCK);
        registerVector3DiProperty(Property.ROTATION_POINT, Material.ENDER_EYE);
        registerRedstoneModeProperty(Property.REDSTONE_MODE, Material.REDSTONE);
    }

    private void registerBooleanProperty(Property<Boolean> property, Material material)
    {
        registry.registerAdapter(
            PropertyGuiAdapterSpigot.<Boolean>builder()
                .property(property)
                .materialFunction(value ->
                {
                    if (!value.isSet())
                    {
                        return material;
                    }
                    return Boolean.TRUE.equals(value.value()) ? Material.LIME_DYE : Material.RED_DYE;
                })
                .localizer(localizer)
                .build()
        );
    }

    private void registerIntegerProperty(Property<Integer> property, Material material)
    {
        registry.registerAdapter(
            PropertyGuiAdapterSpigot.<Integer>builder()
                .property(property)
                .material(material)
                .localizer(localizer)
                .build()
        );
    }

    private void registerDoubleProperty(Property<Double> property, Material material)
    {
        registry.registerAdapter(
            PropertyGuiAdapterSpigot.<Double>builder()
                .property(property)
                .material(material)
                .localizer(localizer)
                .build()
        );
    }

    private void registerVector3DiProperty(
        Property<nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di> property,
        Material material)
    {
        registry.registerAdapter(
            PropertyGuiAdapterSpigot.<nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di>builder()
                .property(property)
                .material(material)
                .localizer(localizer)
                .build()
        );
    }

    private void registerRedstoneModeProperty(
        Property<nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode> property,
        Material material)
    {
        registry.registerAdapter(
            PropertyGuiAdapterSpigot.<nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode>builder()
                .property(property)
                .material(material)
                .localizer(localizer)
                .build()
        );
    }
}
