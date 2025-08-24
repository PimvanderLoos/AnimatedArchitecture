package nl.pim16aap2.animatedarchitecture.core.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
@CustomLog
public class StructureDeletionManager implements IDebuggable
{
    /**
     * The listeners that will be called when a structure is deleted.
     */
    private final List<IDeletionListener> deletionListeners = new CopyOnWriteArrayList<>();

    @Inject
    StructureDeletionManager(DebuggableRegistry registry)
    {
        registry.registerDebuggable(this);
    }

    @Override
    public @Nullable String getDebugInformation()
    {
        return "Registered deletion listeners: " + StringUtil.formatCollection(deletionListeners);
    }

    /**
     * Handles the deletion of a structure.
     *
     * @param structure
     *     The structure that is deleted.
     */
    void onStructureDeletion(IStructureConst structure)
    {
        IDeletionListener.callListeners(deletionListeners, structure);
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
     * Represents a listener for structure deletion events.
     */
    public interface IDeletionListener
    {
        /**
         * Called when a structure is deleted.
         *
         * @param structure
         *     The structure that was deleted.
         */
        void onStructureDeletion(IStructureConst structure);

        /**
         * Safely calls all listeners for a structure that has been deleted.
         *
         * @param listeners
         *     The listeners to call.
         * @param structure
         *     The structure that was deleted.
         */
        static void callListeners(Collection<IDeletionListener> listeners, IStructureConst structure)
        {
            listeners.forEach(listener -> callStructureDeletionListener(listener, structure));
        }

        private static void callStructureDeletionListener(IDeletionListener listener, IStructureConst structure)
        {
            try
            {
                listener.onStructureDeletion(structure);
            }
            catch (Exception exception)
            {
                log.atError().withCause(exception).log(
                    "Failed to call structure deletion listener '%s' for structure %s!",
                    listener.getClass().getName(),
                    structure
                );
            }
        }
    }
}
