package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

/**
 * The database-backed context for recovering an orphaned animated block.
 *
 * @param pluginSession
 *     The plugin session that created the animation run.
 * @param animationRun
 *     The animation run that created the animated block.
 */
public record AnimationRecoveryContext(
    PluginSession pluginSession,
    AnimationRun animationRun
)
{
}
