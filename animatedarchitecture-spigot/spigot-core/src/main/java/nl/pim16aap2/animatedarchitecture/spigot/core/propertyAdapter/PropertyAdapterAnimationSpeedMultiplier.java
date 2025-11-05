package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.bukkit.Material;

/**
 * Property adapter for {@link Property#ANIMATION_SPEED_MULTIPLIER}.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
@EqualsAndHashCode(callSuper = true)
public final class PropertyAdapterAnimationSpeedMultiplier extends AbstractStaticPropertyAdapter<Double>
{
    public static final Property<Double> PROPERTY = Property.ANIMATION_SPEED_MULTIPLIER;
    public static final Material MATERIAL = Material.CLOCK;

    @Inject
    PropertyAdapterAnimationSpeedMultiplier()
    {
        super(
            PROPERTY,
            MATERIAL,
            "gui.property.animation_speed_multiplier.title",
            "gui.property.animation_speed_multiplier.lore.editable",
            "gui.property.animation_speed_multiplier.lore.readonly"
        );
    }
}
