package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;

/**
 * Represent the possible outcomes of trying to toggle a door.
 *
 * @author Pim
 */
public enum DoorToggleResult
{
    /**
     * No issues were encountered; everything went fine.
     */
    SUCCESS("constants.door_toggle_result.success"),

    /**
     * No {@link DoorBase}s were found, so none were toggled.
     */
    NO_DOORS_FOUND("constants.door_toggle_result.no_doors_found"),

    /**
     * The {@link DoorBase} could not be toggled because it is already 'busy': i.e. it is currently moving.
     */
    BUSY("constants.door_toggle_result.door_is_busy"),

    /**
     * The {@link DoorBase} could not be toggled because it is locked.
     */
    LOCKED("constants.door_toggle_result.door_is_locked"),

    /**
     * Some undefined error occurred while attempting to toggle this {@link DoorBase}.
     */
    ERROR("constants.door_toggle_result.generic_toggle_failure"),

    /**
     * The exact instance of the {@link DoorBase} that is to be toggled isn't registered in the {@link DoorRegistry}.
     */
    INSTANCE_UNREGISTERED("constants.door_toggle_result.invalid_state"),

    /**
     * The {@link DoorBase} could not be toggled because it was cancelled.
     */
    CANCELLED("constants.door_toggle_result.cancelled"),

    /**
     * The {@link DoorBase} exceeded the size limit.
     */
    TOO_BIG("constants.door_toggle_result.too_big"),

    /**
     * The player who tried to toggle it or, if not present (e.g. when toggled via redstone"), the original creator does
     * not have permission to open to toggle the {@link DoorBase} because they are not allowed to break blocks in the
     * new location. This happens when a compatibility hook interferes (e.g. WorldGuard).
     */
    NO_PERMISSION("constants.door_toggle_result.no_permission_for_location"),

    /**
     * An attempt to toggle (or open/close) a {@link DoorBase} failed because it was obstructed.
     */
    OBSTRUCTED("constants.door_toggle_result.obstructed"),

    /**
     * The {@link DoorBase} did not have enough space to move.
     */
    NO_DIRECTION("constants.door_toggle_result.no_direction"),

    /**
     * The {@link DoorBase} could not be opened because it is already open.
     */
    ALREADY_OPEN("constants.door_toggle_result.already_open"),

    /**
     * The {@link DoorBase} could not be closed because it is already closed.
     */
    ALREADY_CLOSED("constants.door_toggle_result.already_closed"),

    /**
     * The {@link DoorBase} could not be toggled because its type was disabled at compile time.
     */
    TYPE_DISABLED("constants.door_toggle_result.type_disabled"),
    ;

    @Getter
    private final String localizationKey;

    DoorToggleResult(String localizationKey)
    {
        this.localizationKey = localizationKey;
    }
}
