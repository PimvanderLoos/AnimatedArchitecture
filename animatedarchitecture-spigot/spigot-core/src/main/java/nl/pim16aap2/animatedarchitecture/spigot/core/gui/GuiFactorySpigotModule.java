package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IGuiFactory;

@Module
public interface GuiFactorySpigotModule
{
    @Binds
    @Singleton
    IGuiFactory getGuiFactory(GuiFactory factory);
}
