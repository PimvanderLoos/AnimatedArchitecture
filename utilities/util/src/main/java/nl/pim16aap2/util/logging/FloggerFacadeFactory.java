package nl.pim16aap2.util.logging;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Platform;
import nl.pim16aap2.util.LazyValue;
import nl.pim16aap2.util.reflection.ReflectionBuilder;

import java.lang.reflect.Constructor;

/**
 * A factory class for creating instances of {@link FloggerFacade} based on the class name.
 */
// This class is used by lombok.config to get logger instances.
@SuppressWarnings("unused")
public final class FloggerFacadeFactory
{
    private static final LazyValue<Constructor<FluentLogger>> FLUENT_LOGGER_CONSTRUCTOR =
        new LazyValue<>(() -> ReflectionBuilder
            .findConstructor()
            .inClass(FluentLogger.class)
            .withParameters(LoggerBackend.class)
            .setAccessible()
            .get()
        );

    public static FloggerFacade getLogger(String className)
    {
        final var backend = Platform.getBackend(className);
        try
        {
            return new FloggerFacade(FLUENT_LOGGER_CONSTRUCTOR.get().newInstance(backend));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
