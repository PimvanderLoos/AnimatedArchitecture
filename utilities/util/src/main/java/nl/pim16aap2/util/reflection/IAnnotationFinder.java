package nl.pim16aap2.util.reflection;

import java.lang.annotation.Annotation;

/**
 * Allows specifying a set of annotations that a reflection object should have.
 */
public interface IAnnotationFinder<T>
{
    /**
     * Adds a requirement that the target constructor should contain at least a set of given annotations.
     *
     * @param annotations
     *     The annotations the constructor should have.
     * @return The current object.
     */
    T withAnnotations(Class<? extends Annotation>... annotations);
}
