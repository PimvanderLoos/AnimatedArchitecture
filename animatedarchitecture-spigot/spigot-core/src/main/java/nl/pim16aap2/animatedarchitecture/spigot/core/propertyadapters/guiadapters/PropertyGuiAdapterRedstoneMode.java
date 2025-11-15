package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.bukkit.Material;

/**
 * Property adapter for {@link Property#REDSTONE_MODE}.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
@EqualsAndHashCode(callSuper = true)
public final class PropertyGuiAdapterRedstoneMode extends AbstractStaticPropertyGuiAdapter<RedstoneMode>
{
    public static final Property<RedstoneMode> PROPERTY = Property.REDSTONE_MODE;
    public static final Material MATERIAL = Material.COMPARATOR;

    @Inject
    PropertyGuiAdapterRedstoneMode()
    {
        super(
            PROPERTY,
            MATERIAL,
            "gui.property.redstone_mode.title",
            "gui.property.redstone_mode.lore.editable",
            "gui.property.redstone_mode.lore.readonly"
        );
    }
}
