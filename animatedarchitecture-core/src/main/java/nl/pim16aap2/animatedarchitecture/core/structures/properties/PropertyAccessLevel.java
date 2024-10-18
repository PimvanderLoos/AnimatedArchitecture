package nl.pim16aap2.animatedarchitecture.core.structures.properties;

/**
 * Represents the access level of a property.
 * <p>
 * This determines to what extent a user can interact with the property.
 */
public enum PropertyAccessLevel
{
    /**
     * The property is hidden from the user.
     * <p>
     * It will not be shown in the GUI or in any other user-facing interface.
     * <p>
     * This is useful for properties that are only used strictly internally.
     */
    HIDDEN,

    /**
     * The property is visible to the user but not directly editable.
     * <p>
     * It may be shown in the GUI and in any other user-facing interface.
     * <p>
     * This is useful for properties that the user should be able to see but not change directly.
     */
    VISIBLE,

    /**
     * The property is editable by the user.
     * <p>
     * The user can directly change the value of this property.
     */
    USER_EDITABLE,
}
