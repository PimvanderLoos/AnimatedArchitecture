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
 * Property adapter for {@link Property#QUARTER_CIRCLES}.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
@EqualsAndHashCode(callSuper = true)
public final class PropertyAdapterQuarterCircles extends AbstractStaticPropertyAdapter<Integer>
{
    public static final Property<Integer> PROPERTY = Property.QUARTER_CIRCLES;
    public static final Material MATERIAL = Material.MELON_SLICE;

    @Inject
    PropertyAdapterQuarterCircles()
    {
        super(
            PROPERTY,
            MATERIAL,
            "gui.property.quarter_circles.title",
            "gui.property.quarter_circles.lore.editable",
            "gui.property.quarter_circles.lore.readonly"
        );
    }
}
