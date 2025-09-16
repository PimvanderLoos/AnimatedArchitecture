package nl.pim16aap2.util.logging;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Platform;
import com.google.common.flogger.backend.system.SimpleLoggerBackend;
import nl.pim16aap2.util.LazyValue;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.jspecify.annotations.Nullable;

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

    public static FloggerFacade getLogger(Class<?> clz)
    {
        final var backend = createBackend(clz);
        try
        {
            return new FloggerFacade(FLUENT_LOGGER_CONSTRUCTOR.get().newInstance(backend));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static LoggerBackend createBackend(Class<?> clz)
    {
        final var backend = Platform.getBackend(clz.getName());

        final var customBackend = asCustomLog4j2Backend(clz, backend);
        if (customBackend != null)
            return customBackend;

        return backend;
    }

    private static @Nullable CustomLog4j2Backend asCustomLog4j2Backend(Class<?> clz, LoggerBackend backend)
    {
        if (backend instanceof SimpleLoggerBackend)
            return CustomLog4j2Backend.tryCreate(clz);

        if (CustomLog4j2Backend.canWrap(backend))
            return CustomLog4j2Backend.wrap(backend);

        return null;
    }
}
