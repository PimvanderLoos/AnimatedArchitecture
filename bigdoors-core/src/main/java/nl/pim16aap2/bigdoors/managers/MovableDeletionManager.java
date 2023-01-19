package nl.pim16aap2.bigdoors.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
@Flogger
public class MovableDeletionManager implements IDebuggable
{
    /**
     * The listeners that will be called when a movable is deleted.
     */
    private final List<IDeletionListener> deletionListeners = new CopyOnWriteArrayList<>();

    @Inject MovableDeletionManager(DebuggableRegistry registry)
    {
        registry.registerDebuggable(this);
    }

    @Override
    public @Nullable String getDebugInformation()
    {
        return "MovableDeletionManager: Registered listeners: " + deletionListeners;
    }

    /**
     * Handles the deletion of a movable.
     *
     * @param movable
     *     The movable that is deleted.
     */
    void onMovableDeletion(IMovableConst movable)
    {
        IDeletionListener.callListeners(deletionListeners, movable);
    }

    /**
     * Registers a deletion listener which will be used when a door is deleted.
     *
     * @param listener
     *     The listener to register.
     */
    public void registerDeletionListener(IDeletionListener listener)
    {
        this.deletionListeners.add(listener);
    }

    /**
     * Unregisters a deletion listener.
     *
     * @param listener
     *     The listener to unregister.
     * @return True if the listener was previously registered.
     */
    public boolean unregisterDeletionListener(IDeletionListener listener)
    {
        boolean unregistered = false;
        while (this.deletionListeners.remove(listener))
            unregistered = true;
        return unregistered;
    }

    /**
     * Represents a listener for movable deletion events.
     */
    public interface IDeletionListener
    {
        /**
         * Called when a movable is deleted.
         *
         * @param movable
         *     The movable that was deleted.
         */
        void onMovableDeletion(IMovableConst movable);

        /**
         * Safely calls all listeners for a movable that has been deleted.
         *
         * @param listeners
         *     The listeners to call.
         * @param movable
         *     The movable that was deleted.
         */
        static void callListeners(Collection<IDeletionListener> listeners, IMovableConst movable)
        {
            listeners.forEach(listener -> callMovableDeletionListener(listener, movable));
        }

        private static void callMovableDeletionListener(IDeletionListener listener, IMovableConst movable)
        {
            try
            {
                listener.onMovableDeletion(movable);
            }
            catch (Exception exception)
            {
                log.atSevere().withCause(exception)
                   .log("Failed to call movable deletion listener '%s' for movable %s!",
                        listener.getClass().getName(), movable);
            }
        }
    }
}
