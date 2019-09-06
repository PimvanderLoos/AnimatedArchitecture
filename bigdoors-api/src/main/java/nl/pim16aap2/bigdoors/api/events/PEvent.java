package nl.pim16aap2.bigdoors.api.events;

/**
 * Represents a BigDoors event.
 *
 * @author Pim
 */
public interface PEvent
{
//    private String name = null;
//
//    /**
//     * Gets the name of this event.
//     *
//     * @return The name of this event.
//     */
//    public String getEventName()
//    {
//        if (name == null)
//            name = getClass().getName();
//        return name;
//    }

    /**
     * Gets the name of this event.
     *
     * @return The name of this event.
     */
    String getEventName();
}
