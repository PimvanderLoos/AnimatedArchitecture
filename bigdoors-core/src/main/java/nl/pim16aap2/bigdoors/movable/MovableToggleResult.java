package nl.pim16aap2.bigdoors.movable;

import lombok.Getter;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;

/**
 * Represent the possible outcomes of trying to toggle a movable.
 *
 * @author Pim
 */
public enum MovableToggleResult
{
    /**
     * No issues were encountered; everything went fine.
     */
    SUCCESS("constants.movable_toggle_result.success"),

    /**
     * No {@link MovableBase}s were found, so none were toggled.
     */
    NO_MOVABLES_FOUND("constants.movable_toggle_result.no_movables_found"),

    /**
     * The {@link MovableBase} could not be toggled because it is already 'busy': i.e. it is currently moving.
     */
    BUSY("constants.movable_toggle_result.movable_is_busy"),

    /**
     * The {@link MovableBase} could not be toggled because it is locked.
     */
    LOCKED("constants.movable_toggle_result.movable_is_locked"),

    /**
     * Some undefined error occurred while attempting to toggle this {@link MovableBase}.
     */
    ERROR("constants.movable_toggle_result.generic_toggle_failure"),

    /**
     * Called when trying to skip an animation for a type of movable that does not support skipping animations.
     */
    CANNOT_SKIP("constants.movable_toggle_result.cannot_skip"),

    /**
     * The exact instance of the {@link MovableBase} that is to be toggled isn't registered in the
     * {@link MovableRegistry}.
     */
    INSTANCE_UNREGISTERED("constants.movable_toggle_result.invalid_state"),

    /**
     * The {@link MovableBase} could not be toggled because it was cancelled.
     */
    CANCELLED("constants.movable_toggle_result.cancelled"),

    /**
     * The {@link MovableBase} exceeded the size limit.
     */
    TOO_BIG("constants.movable_toggle_result.too_big"),

    /**
     * The player who tried to toggle it or, if not present (e.g. when toggled via redstone"), the original creator does
     * not have permission to open to toggle the {@link MovableBase} because they are not allowed to break blocks in the
     * new location. This happens when a compatibility hook interferes (e.g. WorldGuard).
     */
    NO_PERMISSION("constants.movable_toggle_result.no_permission_for_location"),

    /**
     * An attempt to toggle (or open/close) a {@link MovableBase} failed because it was obstructed.
     */
    OBSTRUCTED("constants.movable_toggle_result.obstructed"),

    /**
     * The {@link MovableBase} did not have enough space to move.
     */
    NO_DIRECTION("constants.movable_toggle_result.no_direction"),

    /**
     * The {@link MovableBase} could not be opened because it is already open.
     */
    ALREADY_OPEN("constants.movable_toggle_result.already_open"),

    /**
     * The {@link MovableBase} could not be closed because it is already closed.
     */
    ALREADY_CLOSED("constants.movable_toggle_result.already_closed"),

    /**
     * The {@link MovableBase} could not be toggled because its type was disabled at compile time.
     */
    TYPE_DISABLED("constants.movable_toggle_result.type_disabled"),
    ;

    @Getter
    private final String localizationKey;

    MovableToggleResult(String localizationKey)
    {
        this.localizationKey = localizationKey;
    }
}
