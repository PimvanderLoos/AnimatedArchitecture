package nl.pim16aap2.bigdoors.spigot.core.factories.bigdoorseventfactory;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.core.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.spigot.core.events.BigDoorsEventCallerSpigot;

import javax.inject.Singleton;

@Module
public interface BigDoorsEventsSpigotModule
{
    @Binds
    @Singleton
    IBigDoorsEventFactory getBigDoorsEventFactory(BigDoorsEventFactorySpigot factory);

    @Binds
    @Singleton
    IBigDoorsEventCaller getDoorEventCaller(BigDoorsEventCallerSpigot caller);
}
