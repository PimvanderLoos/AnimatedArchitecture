package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

/**
 * Provides runtime metadata for newly started plugin sessions.
 */
public interface IPluginSessionMetadataProvider
{
    /**
     * Gets the metadata to store for a newly started plugin session.
     *
     * @return The plugin session metadata.
     */
    PluginSessionMetadata getMetadata();
}
