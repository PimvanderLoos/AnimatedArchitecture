package nl.pim16aap2.animatedarchitecture.core.structures.properties;

/**
 * Defines the scope of a property.
 * <p>
 * This is used to determine the post-processing behavior of applying properties.
 * <p>
 * For example, changing a property with the scope {@link PropertyScope#ANIMATION} will cause any cached
 * animation-related data to be invalidated.
 */
public enum PropertyScope
{
    ANIMATION,
    REDSTONE
}
