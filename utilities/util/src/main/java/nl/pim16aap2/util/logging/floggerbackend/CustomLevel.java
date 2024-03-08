package nl.pim16aap2.util.logging.floggerbackend;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;

/**
 * This class contains custom log levels.
 * <p>
 * These levels are used by the custom Flogger backend.
 */
public final class CustomLevel
{
    /**
     * A custom level that lies between {@link Level#TRACE} and {@link Level#DEBUG}.
     */
    public static final Level FINER = Level.forName("FINER", StandardLevel.TRACE.intLevel() - 50);

    /**
     * A custom level that lies between {@link Level#DEBUG} and {@link Level#INFO}.
     */
    public static final Level CONF = Level.forName("CONF", StandardLevel.DEBUG.intLevel() - 50);

    private CustomLevel()
    {
    }
}
