package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.factories.IGuiFactory;

import javax.inject.Singleton;

@Module
public interface GuiFactorySpigotModule
{
    @Binds
    @Singleton
    IGuiFactory getGuiFactory(GuiFactory factory);
}
