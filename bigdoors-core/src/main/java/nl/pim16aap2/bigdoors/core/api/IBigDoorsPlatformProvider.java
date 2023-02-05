package nl.pim16aap2.bigdoors.core.api;

import java.util.Optional;

public interface IBigDoorsPlatformProvider
{
    Optional<IBigDoorsPlatform> getPlatform();
}
