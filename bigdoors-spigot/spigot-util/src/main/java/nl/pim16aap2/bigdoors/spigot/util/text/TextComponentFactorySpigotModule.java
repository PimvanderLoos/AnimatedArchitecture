package nl.pim16aap2.bigdoors.spigot.util.text;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.text.ITextComponentFactory;

import javax.inject.Singleton;

@Module
public interface TextComponentFactorySpigotModule
{
    @Binds
    @Singleton
    ITextComponentFactory getTextComponentFactory(TextComponentFactorySpigot factory);
}
