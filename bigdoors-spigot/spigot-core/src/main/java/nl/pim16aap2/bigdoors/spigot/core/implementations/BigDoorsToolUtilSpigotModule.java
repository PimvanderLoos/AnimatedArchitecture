package nl.pim16aap2.bigdoors.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsToolUtil;

import javax.inject.Singleton;

@Module
public interface BigDoorsToolUtilSpigotModule
{
    @Binds
    @Singleton
    IBigDoorsToolUtil provideBigDoorsUtil(BigDoorsToolUtilSpigot bigDoorsToolUtilSpigot);
}
