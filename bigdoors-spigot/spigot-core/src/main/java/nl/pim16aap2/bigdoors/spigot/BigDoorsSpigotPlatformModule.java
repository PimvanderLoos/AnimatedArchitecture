package nl.pim16aap2.bigdoors.spigot;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;

import javax.inject.Singleton;
import java.util.Objects;

@Module
public class BigDoorsSpigotPlatformModule
{
    @Provides
    @Singleton
    IBigDoorsPlatform getBigDoorsPlatform(BigDoorsPlugin bigDoorsPlugin)
    {
        //noinspection ConstantConditions
        return Objects.requireNonNull(bigDoorsPlugin.getBigDoorsSpigotPlatform(), "No platform registered!");
    }
}
