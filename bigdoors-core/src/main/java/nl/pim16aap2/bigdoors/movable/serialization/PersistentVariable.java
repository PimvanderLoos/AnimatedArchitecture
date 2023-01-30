package nl.pim16aap2.bigdoors.movable.serialization;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field in an {@link AbstractMovable} subclass should be persistent.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PersistentVariable
{
    String value() default "";
}
