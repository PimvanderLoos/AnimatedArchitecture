package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;

/**
 * Represents an openable structure.
 * <p>
 * All structures that implement this interface need to register the {@link Property#OPEN_STATUS} property in their
 * structure type.
 */
public interface IStructureWithOpenStatus extends IPropertyHolder
{
    /**
     * Checks if this structure can be opened right now.
     *
     * @return True if this structure can be opened right now.
     */
    default boolean isOpenable()
    {
        return !isOpen();
    }

    /**
     * Checks if this structure can be closed right now.
     *
     * @return True if this structure can be closed right now.
     */
    default boolean isCloseable()
    {
        return isOpen();
    }

    /**
     * Check if the {@link AbstractStructure} is currently open.
     *
     * @return True if the {@link AbstractStructure} is open
     */
    default boolean isOpen()
    {
        return getRequiredPropertyValue(Property.OPEN_STATUS);
    }

    /**
     * Sets the open status of the structure.
     *
     * @param open
     *     The new open status of the structure. True if the structure is open, false if it is closed.
     * @return The previous property value of the open status property.
     */
    default IPropertyValue<Boolean> setOpenStatus(boolean open)
    {
        return setPropertyValue(Property.OPEN_STATUS, open);
    }

    /**
     * Sets the structure to be open.
     *
     * @return The previous property value of the open status property.
     */
    default IPropertyValue<Boolean> setOpen()
    {
        return setOpenStatus(true);
    }

    /**
     * Sets the structure to be closed.
     *
     * @return The previous property value of the open status property.
     */
    default IPropertyValue<Boolean> setClosed()
    {
        return setOpenStatus(false);
    }
}
