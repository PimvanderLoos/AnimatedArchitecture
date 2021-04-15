package nl.pim16aap2.bigdoors.spigot.listeners;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a listener that keeps track of BigDoors related events.
 *
 * @author Pim
 */
public class DoorEventListener implements Listener
{
    private static @Nullable DoorEventListener INSTANCE;
    private final @NonNull DoorOpener doorOpener;

    private DoorEventListener(final @NonNull DoorOpener doorOpener)
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
    public static @NonNull DoorEventListener init(final @NonNull DoorOpener doorOpener)
    {
        return (INSTANCE == null) ? INSTANCE = new DoorEventListener(doorOpener) : INSTANCE;
    }

    /**
     * Gets the instance of the {@link DoorEventListener} if it exists.
     *
     * @return The instance of the {@link DoorEventListener}.
     */
    public static @NonNull DoorEventListener get()
    {
        Preconditions.checkState(INSTANCE != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return INSTANCE;
    }
}
