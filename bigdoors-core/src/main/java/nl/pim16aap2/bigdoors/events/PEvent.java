package nl.pim16aap2.bigdoors.events;

/**
 * Represents a BigDoors event.
 *
 * @author Pim
 */
public abstract class PEvent
{
    private String name = null;

    /**
     * Gets the name of this event.
     *
     * @return The name of this event.
     */
    public String getEventName()
    {
        if (name == null)
            name = getClass().getName();
        return name;
    }
}
