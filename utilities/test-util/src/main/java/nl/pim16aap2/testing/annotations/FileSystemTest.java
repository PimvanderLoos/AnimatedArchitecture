package nl.pim16aap2.testing.annotations;

import nl.pim16aap2.testing.extensions.FilesystemExtension;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to be used on test methods to run a test multiple times, each time with a different temporary
 * filesystem configuration (Unix, Windows, macOS).
 * <p>
 * This is shorthand for <code>@ExtendWith(FilesystemExtension.class)</code>.
 * <p>
 * See {@link FilesystemExtension} for more information.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(FilesystemExtension.class)
public @interface FileSystemTest
{}
