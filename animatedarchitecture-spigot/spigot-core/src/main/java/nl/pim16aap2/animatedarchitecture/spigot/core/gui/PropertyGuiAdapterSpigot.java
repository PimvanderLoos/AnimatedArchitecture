package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import lombok.Builder;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Simple implementation of a Spigot property GUI adapter.
 * <p>
 * This adapter uses functions to determine the material, display name, and lore for a property.
 *
 * @param <T>
 *     The type of the property value.
 */
@Builder
public final class PropertyGuiAdapterSpigot<T> implements IPropertyGuiAdapterSpigot<T>
{
    private final ILocalizer localizer;

    @Getter
    private final Property<T> property;

    private final @Nullable Material material;

    private final @Nullable Function<IPropertyValue<T>, Material> materialFunction;

    private final @Nullable Function<IPropertyValue<T>, String> displayNameFunction;

    private final @Nullable TriFunction<IPropertyValue<T>, ICommandSender, ILocalizer, List<String>> loreFunction;

    @Override
    public Material getMaterial(IPropertyValue<T> propertyValue)
    {
        if (materialFunction != null)
        {
            return materialFunction.apply(propertyValue);
        }
        return material != null ? material : Material.PAPER;
    }

    @Override
    public String getDisplayName(IPropertyValue<T> propertyValue, ICommandSender viewer)
    {
        if (displayNameFunction != null)
        {
            return displayNameFunction.apply(propertyValue);
        }
        return "§e" + property.getNamespacedKey().getKey();
    }

    @Override
    public List<String> getLore(IPropertyValue<T> propertyValue, ICommandSender viewer)
    {
        if (loreFunction != null)
        {
            return loreFunction.apply(propertyValue, viewer, localizer);
        }
        return createDefaultLore(propertyValue);
    }

    private List<String> createDefaultLore(IPropertyValue<T> propertyValue)
    {
        final List<String> lore = new ArrayList<>();
        lore.add("§7Property: §f" + property.getFullKey());

        if (propertyValue.isSet())
        {
            lore.add("§7Current Value: §f" + formatValue(propertyValue.value()));
        }
        else
        {
            lore.add("§7Current Value: §cNot Set");
        }

        return lore;
    }

    private String formatValue(@Nullable T value)
    {
        return value == null ? "null" : value.toString();
    }
}
