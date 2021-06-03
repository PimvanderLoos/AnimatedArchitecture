package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.IDoorBase;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a type of door that can be toggled via a timer. E.g. autoCloseTimer.
 *
 * @author Pim
 */
public interface ITimerToggleableArchetype extends IDoorBase
{
    /**
     * Changes the autoCloseTimer value. See {@link #getAutoCloseTime()}.
     *
     * @param newValue The new value of the autoCloseTimer (in seconds).
     * @return The instance of this door.
     */
    @NotNull AbstractDoorBase setAutoCloseTime(final int newValue);

    /**
     * Gets the value of the autoCloseTimer. This value describes the number of seconds after a door was opened that a
     * door will wait before automatically closing again. Negative values disable auto-closing altogether.
     *
     * @return The number of seconds the door will wait before automatically closing.
     */
    int getAutoCloseTime();

    /**
     * Changes the autoCloseTimer value. See {@link #getAutoOpenTime()}.
     *
     * @param newValue The new value of the autoOpenTimer.
     * @return The instance of this door.
     */
    @NotNull AbstractDoorBase setAutoOpenTime(final int newValue);

    /**
     * Gets the value of the autoOpenTimer. This value describes the number of seconds after a door was closed that a
     * door will wait before automatically opening again. Negative values disable auto-opening altogether.
     *
     * @return The number of seconds the door will wait before automatically opening.
     */
    int getAutoOpenTime();
}
