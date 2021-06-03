package nl.pim16aap2.bigdoors.api.restartable;


import org.jetbrains.annotations.NotNull;

/**
 * Represents an object that can issue a restart or shutdown to {@link IRestartable} objects.
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
    void registerRestartable(@NotNull IRestartable restartable);

    /**
     * Checks if a {@link IRestartable} has been registered with this object.
     *
     * @param restartable The {@link IRestartable} to check.
     * @return True if the {@link IRestartable} has been registered with this object.
     */
    boolean isRestartableRegistered(@NotNull IRestartable restartable);

    /**
     * Deregisters an {@link IRestartable} if it is currently registered.
     *
     * @param restartable The {@link IRestartable} to deregister.
     */
    void deregisterRestartable(@NotNull IRestartable restartable);
}
