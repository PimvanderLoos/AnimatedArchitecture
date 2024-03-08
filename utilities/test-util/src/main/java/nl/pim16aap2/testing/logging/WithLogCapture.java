package nl.pim16aap2.testing.logging;


import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to be used on test classes to capture logs.
 * <p>
 * This is shorthand for <code>@ExtendWith(LogCaptorExtension.class)</code>.
 * <p>
 * See {@link LogCaptorExtension} for more information.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(LogCaptorExtension.class)
public @interface WithLogCapture
{
}
