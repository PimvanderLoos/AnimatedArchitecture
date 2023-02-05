package nl.pim16aap2.bigdoors.core.events;

/**
 * Represents an object that can call BigDoors events.
 *
 * @author Pim
 */
public interface IBigDoorsEventCaller
{
    /**
     * Calls a {@link IBigDoorsEvent}.
     *
     * @param bigDoorsEvent
     *     The {@link IBigDoorsEvent} to call.
     */
    <T extends IBigDoorsEvent> void callBigDoorsEvent(T bigDoorsEvent);
}
