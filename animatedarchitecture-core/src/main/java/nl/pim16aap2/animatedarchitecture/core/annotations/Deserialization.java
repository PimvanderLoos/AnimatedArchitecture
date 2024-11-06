package nl.pim16aap2.animatedarchitecture.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a constructor can be used for deserialization.
 * <p>
 * If the version is specified, the constructor will only be used for that version.
 * <p>
 * If the version is not specified, the constructor will be used for all versions that do not have a specific
 * constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Deserialization
{
    /**
     * Used to specify the exact version of the data being deserialized.
     * <p>
     * If not specified, the constructor will be used for all versions that do not have a specific constructor.
     * <p>
     * This can be used to have multiple constructors for different versions of the data and provide an upgrade path.
     *
     * @return The version of the data being deserialized.
     */
    int version() default -1;
}
