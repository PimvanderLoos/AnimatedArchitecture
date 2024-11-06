package nl.pim16aap2.animatedarchitecture.core.structures.properties;

/**
 * Describes a type of structure that rotates for a certain number of quarter circles. This is used for structures like
 * drawbridges, and big doors.
 */
public interface IStructureWithQuarterCircles extends IPropertyHolder
{
    /**
     * The number of quarter circles (so 90 degree rotations) this structure will make before stopping.
     *
     * @return The number of quarter circles this structure will rotate.
     */
    default int getQuarterCircles()
    {
        return getRequiredPropertyValue(Property.QUARTER_CIRCLES);
    }


    /**
     * Sets the number of quarter circles (so 90 degree rotations) this structure will make before stopping.
     *
     * @param quarterCircles
     *     The number of quarter circles this structure will rotate.
     * @return The previous property value of the open status property.
     */
    default IPropertyValue<Integer> setQuarterCircles(int quarterCircles)
    {
        return setPropertyValue(Property.QUARTER_CIRCLES, quarterCircles);
    }
}
