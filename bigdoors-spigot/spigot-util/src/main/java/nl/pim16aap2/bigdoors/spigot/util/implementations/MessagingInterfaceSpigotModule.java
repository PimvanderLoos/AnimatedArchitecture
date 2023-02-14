package nl.pim16aap2.bigdoors.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.IMessagingInterface;

import javax.inject.Singleton;

@Module
public interface MessagingInterfaceSpigotModule
{
    @Binds
    @Singleton
    IMessagingInterface getMessagingInterface(MessagingInterfaceSpigot messagingInterface);
}
