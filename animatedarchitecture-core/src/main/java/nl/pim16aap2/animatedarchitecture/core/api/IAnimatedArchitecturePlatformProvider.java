package nl.pim16aap2.animatedarchitecture.core.api;

import java.util.Optional;

/**
 * Represents a provider for the {@link IAnimatedArchitecturePlatform}.
 */
public interface IAnimatedArchitecturePlatformProvider
{
    /**
     * Gets the platform loaded on the server.
     * <p>
     * This method may return an empty {@link Optional} if the platform is not available.
     * <p>
     * The platform can be unavailable if it failed to load or if the server is running an unsupported platform.
     *
     * @return An {@link Optional} containing the platform if it is available.
     */
    Optional<IAnimatedArchitecturePlatform> getPlatform();
}
