package nl.pim16aap2.bigdoors.spigot.factories.bigdoorseventfactory;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;

import javax.inject.Singleton;

@Module
public interface BigDoorsEventFactorySpigotModule
{
    @Binds
    @Singleton
    IBigDoorsEventFactory getBigDoorsEventFactory(BigDoorsEventFactorySpigot factory);
}
