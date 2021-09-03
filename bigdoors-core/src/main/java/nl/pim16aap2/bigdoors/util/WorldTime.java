package nl.pim16aap2.bigdoors.util;


/**
 * Represents a moment in time in hours and minutes.
 *
 * @author Pim
 */
public final class WorldTime
{
    private final int hours;
    private final int minutes;

    /**
     * Constructs a new WorldTime.
     *
     * @param hours
     *     The time in hours.
     * @param minutes
     *     The time in minutes.
     */
    public WorldTime(int hours, int minutes)
    {
        this.hours = hours;
        this.minutes = minutes;
    }

    /**
     * Constructs a new WorldTime.
     *
     * @param worldTime
     *     The time in a world in Minecraft time (i.e. 0 to 24000).
     */
    public WorldTime(long worldTime)
    {
        this(WorldTime.calculateHours(worldTime), WorldTime.calculateMinutes(worldTime));
    }

    /**
     * Calculates what hour it is from a given time in Minecraft time.
     *
     * @param worldTime
     *     The current time in Minecraft time (i.e. 0 to 24000).
     * @return The current Minecraft hour.
     */
    private static int calculateHours(long worldTime)
    {
        final int hours = (int) (worldTime % 24_000) / 1_000;
        // Minecraft time starts at 6 hours.
        return (hours + 6) % 24;
    }

    /**
     * Calculates how many minutes since the last full hours have passed from Minecraft time.
     *
     * @param worldTime
     *     The current time in Minecraft time (i.e. 0 to 24000).
     * @return The number of Minecraft minutes since the last hour..
     */
    private static int calculateMinutes(long worldTime)
    {
        final int minutes = (int) worldTime % 1000;
        return (int) (60 * (minutes / 1000F));
    }

    /**
     * Gets the current Minecraft hour.
     *
     * @return The current Minecraft hour.
     */
    public int getHours()
    {
        return hours;
    }

    /**
     * Gets the number of Minecraft minutes since the last full Minecraft hour.
     *
     * @return The number of Minecraft minutes since the last full Minecraft hour.
     */
    public int getMinutes()
    {
        return minutes;
    }

    @Override
    public String toString()
    {
        return hours + ":" + minutes;
    }
}
