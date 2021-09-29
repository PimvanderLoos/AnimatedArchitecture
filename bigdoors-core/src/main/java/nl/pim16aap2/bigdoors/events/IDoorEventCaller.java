package nl.pim16aap2.bigdoors.events;

/**
 * Represents an object that can call door events.
 *
 * @author Pim
 */
public interface IDoorEventCaller
{
    /**
     * Calls a {@link IBigDoorsEvent}.
     *
     * @param doorActionEvent
     *     The {@link IBigDoorsEvent} to call.
     */
    void callDoorEvent(IBigDoorsEvent doorActionEvent);
}
