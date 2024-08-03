package nl.pim16aap2.animatedarchitecture.core.structures.properties;

/**
 * Represents a read-only property manager.
 * <p>
 * This interface is used to get the values of properties from a property manager.
 * <p>
 * This is a specialization of {@link IPropertyHolderConst} that is used specifically for property managers.
 */
public sealed interface IPropertyManagerConst
    extends IPropertyHolderConst
    permits PropertyManager, PropertyManagerSnapshot
{
}
