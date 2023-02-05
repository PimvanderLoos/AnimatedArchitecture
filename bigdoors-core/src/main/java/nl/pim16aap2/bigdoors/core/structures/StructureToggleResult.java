package nl.pim16aap2.bigdoors.core.structures;

import lombok.Getter;

/**
 * Represent the possible outcomes of trying to toggle a structure.
 *
 * @author Pim
 */
public enum StructureToggleResult
{
    /**
     * No issues were encountered; everything went fine.
     */
    SUCCESS("constants.structure_toggle_result.success"),

    /**
     * No {@link StructureBase}s were found, so none were toggled.
     */
    NO_STRUCTURES_FOUND("constants.structure_toggle_result.no_structures_found"),

    /**
     * The {@link StructureBase} could not be toggled because it is already 'busy': i.e. it is currently moving.
     */
    BUSY("constants.structure_toggle_result.structure_is_busy"),

    /**
     * The {@link StructureBase} could not be toggled because it is locked.
     */
    LOCKED("constants.structure_toggle_result.structure_is_locked"),

    /**
     * Some undefined error occurred while attempting to toggle this {@link StructureBase}.
     */
    ERROR("constants.structure_toggle_result.generic_toggle_failure"),

    /**
     * Called when trying to skip an animation for a type of structure that does not support skipping animations.
     */
    CANNOT_SKIP("constants.structure_toggle_result.cannot_skip"),

    /**
     * The exact instance of the {@link StructureBase} that is to be toggled isn't registered in the
     * {@link StructureRegistry}.
     */
    INSTANCE_UNREGISTERED("constants.structure_toggle_result.invalid_state"),

    /**
     * The {@link StructureBase} could not be toggled because it was cancelled.
     */
    CANCELLED("constants.structure_toggle_result.cancelled"),

    /**
     * The {@link StructureBase} exceeded the size limit.
     */
    TOO_BIG("constants.structure_toggle_result.too_big"),

    /**
     * The player who tried to toggle it or, if not present (e.g. when toggled via redstone"), the original creator does
     * not have permission to open to toggle the {@link StructureBase} because they are not allowed to break blocks in
     * the new location. This happens when a compatibility hook interferes (e.g. WorldGuard).
     */
    NO_PERMISSION("constants.structure_toggle_result.no_permission_for_location"),

    /**
     * An attempt to toggle (or open/close) a {@link StructureBase} failed because it was obstructed.
     */
    OBSTRUCTED("constants.structure_toggle_result.obstructed"),

    /**
     * The {@link StructureBase} did not have enough space to move.
     */
    NO_DIRECTION("constants.structure_toggle_result.no_direction"),

    /**
     * The {@link StructureBase} could not be opened because it is already open.
     */
    ALREADY_OPEN("constants.structure_toggle_result.already_open"),

    /**
     * The {@link StructureBase} could not be closed because it is already closed.
     */
    ALREADY_CLOSED("constants.structure_toggle_result.already_closed"),

    /**
     * The {@link StructureBase} could not be toggled because its type was disabled at compile time.
     */
    TYPE_DISABLED("constants.structure_toggle_result.type_disabled"),
    ;

    @Getter
    private final String localizationKey;

    StructureToggleResult(String localizationKey)
    {
        this.localizationKey = localizationKey;
    }
}
