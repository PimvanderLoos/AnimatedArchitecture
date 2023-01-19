package nl.pim16aap2.bigdoors.spigot.gui;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.movable.IMovableConst;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Represents a manager for door deletions specifically for the GUI classes.
 * <p>
 * The listeners registered with the MovableRegistry have slightly different requirements than the general listeners of
 * the MovableRegistry's events.
 * <p>
 * For one, this class will call listeners in reverse order, as new GUI pages are opened over top of the older ones and
 * need to be processed in LIFO ordering.
 * <p>
 * Secondly, the holder for the GUI listeners will receive a lot of writes, whereas the main holder does not.
 */
@Singleton
@Flogger
class GuiMovableDeletionManager implements MovableRegistry.IDeletionListener, IDebuggable
{
    @GuardedBy("this")
    private final Deque<IGuiPage.IGuiMovableDeletionListener> listeners = new ArrayDeque<>();
    private final IPExecutor executor;

    @Inject GuiMovableDeletionManager(
        MovableRegistry movableRegistry, IPExecutor executor, DebuggableRegistry debuggableRegistry)
    {
        this.executor = executor;

        movableRegistry.registerDeletionListener(this);
        debuggableRegistry.registerDebuggable(this);
    }

    synchronized void registerDeletionListener(IGuiPage.IGuiMovableDeletionListener listener)
    {
        listeners.addFirst(listener);
    }

    synchronized void unregisterDeletionListener(IGuiPage.IGuiMovableDeletionListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void onMovableDeletion(IMovableConst movable)
    {
        final List<MovableRegistry.IDeletionListener> copy;
        synchronized (this)
        {
            copy = new ArrayList<>(listeners);
        }
        executor.scheduleOnMainThread(() -> MovableRegistry.IDeletionListener.callListeners(copy, movable));
    }

    @Override
    public String getDebugInformation()
    {
        return "Deletion listeners registered with GuiMovableDeletionManager:" + formatListenersForDebug();
    }

    private String formatListenersForDebug()
    {
        final List<IGuiPage.IGuiMovableDeletionListener> copy;
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

    private String formatListenerForDebug(IGuiPage.IGuiMovableDeletionListener listener)
    {
        return String.format("Gui Page: '%s' for player %s", listener.getPageName(), listener.getInventoryHolder());
    }
}
