package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a read-only property container.
 * <p>
 * This interface is used to get the values of properties from a property container.
 * <p>
 * This is a specialization of {@link IPropertyHolderConst} that is used specifically for property containers.
 */
public sealed interface IPropertyContainerConst
    extends IPropertyHolderConst, Iterable<PropertyValuePair<?>>
    permits PropertyContainer, PropertyContainerSnapshot
{
    /**
     * Returns a read-only iterator over the properties in this container.
     * <p>
     * Any properties that are either unmapped or of the wrong type are excluded.
     *
     * @return A read-only iterator over the properties in this container.
     */
    @Override
    Iterator<PropertyValuePair<?>> iterator();

    /**
     * Returns a read-only spliterator over the properties in this container.
     * <p>
     * Any properties that are either unmapped or of the wrong type are excluded.
     *
     * @return A read-only spliterator over the properties in this container.
     */
    @Override
    Spliterator<PropertyValuePair<?>> spliterator();

    /**
     * Returns a {@link Stream} of the properties in this container.
     * <p>
     * This works the same as {@link Collection#stream()}.
     * <p>
     * Any properties that are either unmapped or of the wrong type are excluded.
     *
     * @return A {@link Stream} of the properties in this container.
     */
    default Stream<PropertyValuePair<?>> stream()
    {
        return StreamSupport.stream(spliterator(), false);
    }
}
