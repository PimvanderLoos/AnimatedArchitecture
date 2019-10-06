package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an object that can issue a restart to {@link IRestartable} objects.
 *
 * @author Pim
 */
public interface IRestartableHolder
{
    /**
     * Register a {@link IRestartable} object with this object, so this object can restart the provided
     * object.
     *
     * @param restartable A {@link IRestartable} object that can be restarted by this object.
     */
    void registerRestartable(final @NotNull IRestartable restartable);

    /**
     * Checks if a {@link IRestartable} has been registered with this object.
     *
     * @param restartable The {@link IRestartable} to check.
     * @return True if the {@link IRestartable} has been registered with this object.
     */
    boolean isRestartableRegistered(final @NotNull IRestartable restartable);
}
