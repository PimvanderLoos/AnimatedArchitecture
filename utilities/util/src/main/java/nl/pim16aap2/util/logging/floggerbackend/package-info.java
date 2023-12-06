/**
 * This package contains a custom backend for Flogger.
 * <p>
 * The backend is based on Flogger's default Log4J2 backend, but with some changes to make it more suitable for our
 * needs.
 * <p>
 * Currently, the only changes have been made to {@link nl.pim16aap2.util.logging.floggerbackend.Log4j2LogEventUtil}.
 * This class has been changed to use our own custom log levels, as defined in
 * {@link nl.pim16aap2.util.logging.floggerbackend.CustomLevel}, in addition to the default Log4J2 levels.
 */
@NonNullByDefault
package nl.pim16aap2.util.logging.floggerbackend;

import org.eclipse.jdt.annotation.NonNullByDefault;
