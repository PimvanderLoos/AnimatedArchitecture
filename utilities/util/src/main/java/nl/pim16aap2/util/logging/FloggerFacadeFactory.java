package nl.pim16aap2.util.logging;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Platform;
import nl.pim16aap2.util.reflection.ReflectionBuilder;

import java.lang.reflect.Constructor;

/**
 * A factory class for creating instances of {@link FloggerFacade} based on the class name.
 */
public final class FloggerFacadeFactory
{
    private static final Constructor<FluentLogger> FLUENT_LOGGER_CONSTRUCTOR = ReflectionBuilder
        .findConstructor()
        .inClass(FluentLogger.class)
        .withParameters(LoggerBackend.class)
        .setAccessible()
        .get();

    public static FloggerFacade getLogger(String className)
    {
        final LoggerBackend backend = Platform.getBackend(className);
        try
        {
            return new FloggerFacade(FLUENT_LOGGER_CONSTRUCTOR.newInstance(backend));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
