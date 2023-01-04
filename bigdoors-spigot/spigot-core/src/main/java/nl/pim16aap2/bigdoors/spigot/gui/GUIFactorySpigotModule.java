package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.factories.IGUIFactory;

import javax.inject.Singleton;

@Module
public interface GUIFactorySpigotModule
{
    @Binds
    @Singleton
    IGUIFactory getGUIFactory(GUIFactory factory);
}
