package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Represents a manager for door deletions specifically for the GUI classes.
 * <p>
 * The listeners registered with the StructureRegistry have slightly different requirements than the general listeners
 * of the StructureRegistry's events.
 * <p>
 * For one, this class will call listeners in reverse order, as new GUI pages are opened over top of the older ones and
 * need to be processed in LIFO ordering.
 * <p>
 * Secondly, the holder for the GUI listeners will receive a lot of writes, whereas the main holder does not.
 */
@Singleton
@Flogger
class GuiStructureDeletionManager implements StructureDeletionManager.IDeletionListener, IDebuggable
{
    @GuardedBy("this")
    private final Deque<IGuiPage.IGuiStructureDeletionListener> listeners = new ArrayDeque<>();
    private final IExecutor executor;

    @Inject
    GuiStructureDeletionManager(
        StructureDeletionManager structureDeletionManager,
        IExecutor executor,
        DebuggableRegistry debuggableRegistry)
    {
        this.executor = executor;

        structureDeletionManager.registerDeletionListener(this);
        debuggableRegistry.registerDebuggable(this);
    }

    synchronized void registerDeletionListener(IGuiPage.IGuiStructureDeletionListener listener)
    {
        listeners.addFirst(listener);
    }

    synchronized void unregisterDeletionListener(IGuiPage.IGuiStructureDeletionListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void onStructureDeletion(IStructureConst structure)
    {
        final List<StructureDeletionManager.IDeletionListener> copy;
        synchronized (this)
        {
            copy = new ArrayList<>(listeners);
        }
        executor.scheduleOnMainThread(() -> StructureDeletionManager.IDeletionListener.callListeners(copy, structure));
    }

    @Override
    public String getDebugInformation()
    {
        return "Deletion listeners registered with GuiStructureDeletionManager:" + formatListenersForDebug();
    }

    private String formatListenersForDebug()
    {
        final List<IGuiPage.IGuiStructureDeletionListener> copy;
        synchronized (this)
        {
            if (listeners.isEmpty())
                return "\n  []";
            copy = new ArrayList<>(listeners);
        }

        final StringBuilder sb = new StringBuilder("\n");
        for (final var listener : copy)
            sb.append("  * ").append(formatListenerForDebug(listener)).append('\n');
        return sb.toString();
    }

    private String formatListenerForDebug(IGuiPage.IGuiStructureDeletionListener listener)
    {
        return String.format("Gui Page: '%s' for player %s", listener.getPageName(), listener.getInventoryHolder());
    }
}
