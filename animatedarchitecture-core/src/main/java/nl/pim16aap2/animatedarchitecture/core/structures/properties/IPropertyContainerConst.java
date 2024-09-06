package nl.pim16aap2.animatedarchitecture.core.structures.properties;

/**
 * Represents a read-only property container.
 * <p>
 * This interface is used to get the values of properties from a property container.
 * <p>
 * This is a specialization of {@link IPropertyHolderConst} that is used specifically for property containers.
 */
public sealed interface IPropertyContainerConst
    extends IPropertyHolderConst
    permits PropertyContainer, PropertyContainerSnapshot
{
}
