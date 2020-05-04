package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.IDoorBase;

/**
 * Represents a type of door that can be toggled via a timer. E.g. autoCloseTimer.
 *
 * @author Pim
 */
public interface ITimerToggleableArchetype extends IDoorBase
{
    /**
     * Changes the autoCloseTimer value. See {@link #getAutoCloseTimer()}.
     *
     * @param newValue The new value of the autoCloseTimer.
     */
    void setAutoCloseTimer(final int newValue);

    /**
     * Gets the value of the autoCloseTimer. This value describes the number of seconds after a door was opened that a
     * door will wait before automatically closing again. Negative values disable auto-closing altogether.
     *
     * @return The number of seconds the door will wait before automatically closing.
     */
    int getAutoCloseTimer();

    /**
     * Changes the autoCloseTimer value. See {@link #getAutoOpenTimer()}.
     *
     * @param newValue The new value of the autoOpenTimer.
     */
    void setAutoOpenTimer(final int newValue);

    /**
     * Gets the value of the autoOpenTimer. This value describes the number of seconds after a door was closed that a
     * door will wait before automatically opening again. Negative values disable auto-opening altogether.
     *
     * @return The number of seconds the door will wait before automatically opening.
     */
    int getAutoOpenTimer();
}
