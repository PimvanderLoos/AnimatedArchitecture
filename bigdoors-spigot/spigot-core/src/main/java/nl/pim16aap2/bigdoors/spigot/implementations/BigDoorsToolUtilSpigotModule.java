package nl.pim16aap2.bigdoors.spigot.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;

import javax.inject.Singleton;

@Module
public interface BigDoorsToolUtilSpigotModule
{
    @Binds
    @Singleton
    IBigDoorsToolUtil provideBigDoorsUtil(BigDoorsToolUtilSpigot bigDoorsToolUtilSpigot);
}
