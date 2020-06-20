package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.events.IPCancellable;

/**
 * Represents a toggle action that might be applied to a door. Note that not cancelling this event does not mean that
 * action is guaranteed to take place, as other factors might prevent that from happening (e.g. the door being locked).
 * <p>
 * If you are looking for a guaranteed action, use {@link IDoorEventToggleStart} instead.
 *
 * @author Pim
 */
public interface IDoorEventTogglePrepare extends IDoorEvent, IPCancellable
{
}
