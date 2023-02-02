package nl.pim16aap2.bigdoors.structures.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a constructor can be used for deserialization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Deserialization
{
    /**
     * @return The version of the data being deserialized.
     */
    int version() default -1;
}
