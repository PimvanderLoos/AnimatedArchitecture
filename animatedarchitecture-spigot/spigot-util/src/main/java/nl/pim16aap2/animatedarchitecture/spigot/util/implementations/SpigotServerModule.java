package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.commands.IServer;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public interface SpigotServerModule
{
    @Binds
    @Singleton
    IServer getServer(SpigotServer server);

    @Binds
    @Singleton
    @Named("MessageableServer")
    IMessageable getServerMessageable(SpigotServer server);
}
