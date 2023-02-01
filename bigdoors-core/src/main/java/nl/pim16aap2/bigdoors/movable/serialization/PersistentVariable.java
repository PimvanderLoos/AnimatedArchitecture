package nl.pim16aap2.bigdoors.movable.serialization;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that a field in an {@link AbstractMovable} subclass should be persistent.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface PersistentVariable
{
    /**
     * @return The name of the variable. The name does not need to match the name of the variable in the source file.
     */
    String value() default "";
}
