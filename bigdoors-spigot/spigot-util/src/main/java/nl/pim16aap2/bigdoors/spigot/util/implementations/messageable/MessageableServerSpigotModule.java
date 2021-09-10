package nl.pim16aap2.bigdoors.spigot.util.implementations.messageable;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IMessageable;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public interface MessageableServerSpigotModule
{
    @Binds
    @Singleton
    @Named("MessageableServer")
    IMessageable getMessageableServerSpigot(MessageableServerSpigot messageable);
}
