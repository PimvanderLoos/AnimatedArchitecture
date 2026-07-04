package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import com.google.common.flogger.StackSize;
import lombok.CustomLog;

/**
 * The database-backed context for recovering an orphaned animated block.
 *
 * @param pluginSession
 *     The plugin session that created the animation run.
 * @param animationRun
 *     The animation run that created the animated block.
 */
@CustomLog
public record AnimationRecoveryContext(
    PluginSession pluginSession,
    AnimationRun animationRun
)
{
    public AnimationRecoveryContext
    {
        if (!animationRun.sessionUuid().equals(pluginSession.uuid()))
        {
            log.atError().withStackTrace(StackSize.FULL).log(
                "Session mismatch for animation run '%s' and plugin session '%s'.",
                animationRun,
                pluginSession
            );
        }
    }
}
