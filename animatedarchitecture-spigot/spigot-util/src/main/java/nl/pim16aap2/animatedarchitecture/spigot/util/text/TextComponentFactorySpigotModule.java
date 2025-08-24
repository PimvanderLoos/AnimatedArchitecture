package nl.pim16aap2.animatedarchitecture.spigot.util.text;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;

@Module
public interface TextComponentFactorySpigotModule
{
    @Binds
    @Singleton
    ITextComponentFactory getTextComponentFactory(TextComponentFactorySpigot factory);
}
