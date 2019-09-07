package nl.pim16aap2.bigdoors.util;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an object that can issue a restart to {@link IRestartable} objects.
 *
 * @author Pim
 */
public interface IRestartableHolder
{
    /**
     * Register a {@link IRestartable} object with this object, so this object can restart the provided object.
     *
     * @param restartable A {@link IRestartable} object that can be restarted by this object.
     */
    void registerRestartable(final @NotNull IRestartable restartable);
}
