package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IPropertyGuiAdapter;
import nl.pim16aap2.animatedarchitecture.core.api.IPropertyGuiAdapterRegistry;

@Module
public interface PropertyGuiAdapterRegistrySpigotModule
{
    @Binds
    @Singleton
    IPropertyGuiAdapterRegistry<? extends IPropertyGuiAdapter<?>> getPropertyGuiAdapterRegistry(
        PropertyGuiAdapterRegistrySpigot registry
    );
}
