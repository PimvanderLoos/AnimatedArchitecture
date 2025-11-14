package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.bukkit.Material;

/**
 * Property adapter for {@link Property#ROTATION_POINT}.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
@EqualsAndHashCode(callSuper = true)
public final class PropertyGuiAdapterRotationPoint extends AbstractStaticPropertyGuiAdapter<Vector3Di>
{
    public static final Property<Vector3Di> PROPERTY = Property.ROTATION_POINT;
    public static final Material MATERIAL = Material.BAMBOO;

    @Inject
    PropertyGuiAdapterRotationPoint()
    {
        super(
            PROPERTY,
            MATERIAL,
            "gui.property.rotation_point.title",
            "gui.property.rotation_point.lore.editable",
            "gui.property.rotation_point.lore.readonly"
        );
    }
}
