package nl.pim16aap2.bigdoors.spigot.util.implementations.pserver;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.commands.IPServer;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public interface PServerModule
{
    @Binds
    @Singleton
    IPServer getServer(PServer server);

    @Binds
    @Singleton
    @Named("MessageableServer")
    IMessageable getServerMessageable(PServer server);
}
