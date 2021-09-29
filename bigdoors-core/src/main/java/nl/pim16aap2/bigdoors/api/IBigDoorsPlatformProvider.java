package nl.pim16aap2.bigdoors.api;

import java.util.Optional;

public interface IBigDoorsPlatformProvider
{
    Optional<IBigDoorsPlatform> getPlatform();
}
