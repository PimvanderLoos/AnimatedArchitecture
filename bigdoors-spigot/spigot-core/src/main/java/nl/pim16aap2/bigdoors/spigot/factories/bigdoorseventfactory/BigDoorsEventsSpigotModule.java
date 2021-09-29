package nl.pim16aap2.bigdoors.spigot.factories.bigdoorseventfactory;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.events.IDoorEventCaller;
import nl.pim16aap2.bigdoors.spigot.events.DoorEventCallerSpigot;

import javax.inject.Singleton;

@Module
public interface BigDoorsEventsSpigotModule
{
    @Binds
    @Singleton
    IBigDoorsEventFactory getBigDoorsEventFactory(BigDoorsEventFactorySpigot factory);

    @Binds
    @Singleton
    IDoorEventCaller getDoorEventCaller(DoorEventCallerSpigot caller);
}
