package nl.pim16aap2.bigdoors.spigot.core.gui;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.factories.IGuiFactory;

import javax.inject.Singleton;

@Module
public interface GuiFactorySpigotModule
{
    @Binds
    @Singleton
    IGuiFactory getGuiFactory(GuiFactory factory);
}
