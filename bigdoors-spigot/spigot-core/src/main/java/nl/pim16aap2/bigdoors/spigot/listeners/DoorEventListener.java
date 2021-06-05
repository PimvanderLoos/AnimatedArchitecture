package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a listener that keeps track of BigDoors related events.
 *
 * @author Pim
 */
public class DoorEventListener implements Listener
{
    private static @Nullable DoorEventListener INSTANCE;
    private final @NotNull DoorOpener doorOpener;

    private DoorEventListener(final @NotNull DoorOpener doorOpener)
    {
        this.doorOpener = doorOpener;
    }

    /**
     * Initializes the {@link DoorEventListener}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param doorOpener The {@link DoorOpener} used to open, close, and toggle doors.
     * @return The instance of this {@link DoorEventListener}.
     */
    public static @NotNull DoorEventListener init(final @NotNull DoorOpener doorOpener)
    {
        return (INSTANCE == null) ? INSTANCE = new DoorEventListener(doorOpener) : INSTANCE;
    }

    /**
     * Gets the instance of the {@link DoorEventListener} if it exists.
     *
     * @return The instance of the {@link DoorEventListener}.
     */
    public static @NotNull DoorEventListener get()
    {
        return Util.requireNonNull(INSTANCE, "Instance");
    }
}
