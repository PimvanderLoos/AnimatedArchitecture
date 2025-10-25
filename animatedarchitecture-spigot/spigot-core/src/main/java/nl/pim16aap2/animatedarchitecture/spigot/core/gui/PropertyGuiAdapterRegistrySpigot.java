package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.api.PropertyGuiAdapterRegistry;

/**
 * Spigot-specific implementation of the property GUI adapter registry.
 */
@Singleton
@NoArgsConstructor(onConstructor_ = @Inject)
public final class PropertyGuiAdapterRegistrySpigot extends PropertyGuiAdapterRegistry<IPropertyGuiAdapterSpigot<?>>
{
}
