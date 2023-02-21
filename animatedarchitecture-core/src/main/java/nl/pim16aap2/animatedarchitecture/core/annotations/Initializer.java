package nl.pim16aap2.animatedarchitecture.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is meant to be used on initialization methods to ensure NullAway understands that those methods are
 * used to initialize variables.
 *
 * @author Pim
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Initializer
{
}
