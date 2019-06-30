package nl.pim16aap2.bigdoors.events;

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
            name = this.getClass().getName();
        return name;
    }
}
