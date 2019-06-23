package nl.pim16aap2.bigdoors.util;

/**
 * Represents an object that can issue a restart to {@link IRestartable}
 * objects.
 *
 * @author Pim
 */
public interface RestartableHolder
{
    /**
     * Register a {@link IRestartable} object with this object, so this object can
     * restart the provided object.
     * 
     * @param restartable A {@link IRestartable} object that can be restarted by
     *                    this object.
     */
    public void registerRestartable(final IRestartable restartable);
}
