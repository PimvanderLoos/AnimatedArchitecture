package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.commands.IServer;

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
