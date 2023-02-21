package nl.pim16aap2.animatedarchitecture.core.api;

import java.util.Optional;

public interface IAnimatedArchitecturePlatformProvider
{
    Optional<IAnimatedArchitecturePlatform> getPlatform();
}
