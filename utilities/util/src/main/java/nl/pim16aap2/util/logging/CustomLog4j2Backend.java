package nl.pim16aap2.util.logging;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.system.BackendFactory;
import nl.pim16aap2.util.LazyValue;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

final class CustomLog4j2Backend extends LoggerBackend
{
    public static final String ORIGINAL_LOG4J2_BACKEND_CLASS_NAME =
        "com.google.common.flogger.backend.log4j2.Log4j2LoggerBackend";

    private static final LazyValue<BackendFactory> LOG4J2_BACKEND_FACTORY =
        new LazyValue<>(CustomLog4j2Backend::getLog4j2BackendFactory);

    private static final @Nullable Class<?> LOG4J2_LOG_EVENT_UTIL_CLASS =
        ReflectionBuilder.findClass("com.google.common.flogger.backend.log4j2.Log4j2LogEventUtil").getNullable();
    private static final @Nullable Method TO_LOG4J_LOG_EVENT_METHOD =
        LOG4J2_LOG_EVENT_UTIL_CLASS == null ? null :
            ReflectionBuilder
                .findMethod()
                .inClass(LOG4J2_LOG_EVENT_UTIL_CLASS)
                .withName("toLog4jLogEvent")
                .withParameters(String.class, LogData.class)
                .setAccessible()
                .get();
    private static final @Nullable Method TO_LOG4J_LOG_EVENT_WITH_ERROR_METHOD =
        LOG4J2_LOG_EVENT_UTIL_CLASS == null ? null :
            ReflectionBuilder
                .findMethod()
                .inClass(LOG4J2_LOG_EVENT_UTIL_CLASS)
                .withName("toLog4jLogEvent")
                .withParameters(String.class, RuntimeException.class, LogData.class)
                .setAccessible()
                .get();

    private static final @Nullable Class<? extends LoggerBackend> ORIGINAL_BACKEND_CLASS = findLog4j2BackendClass();
    private static final @Nullable Field NESTED_LOGGER_FIELD =
        ORIGINAL_BACKEND_CLASS == null ? null :
            ReflectionBuilder
                .findField()
                .inClass(ORIGINAL_BACKEND_CLASS)
                .ofType(Logger.class)
                .setAccessible()
                .get();

    private static volatile @Nullable Marker marker = null;

    private final LoggerBackend backend;
    private final @Nullable Logger logger;

    private CustomLog4j2Backend(LoggerBackend backend)
    {
        this.backend = backend;
        this.logger = getLogger(backend);
    }

    public static CustomLog4j2Backend wrap(LoggerBackend backend)
    {
        if (!canWrap(backend))
            throw new IllegalArgumentException("Cannot wrap backend of class " + backend.getClass());
        return new CustomLog4j2Backend(backend);
    }

    public static boolean canWrap(LoggerBackend backend)
    {
        return ORIGINAL_BACKEND_CLASS != null && ORIGINAL_BACKEND_CLASS.isInstance(backend);
    }

    public static @Nullable CustomLog4j2Backend tryCreate(Class<?> clz)
    {
        if (ORIGINAL_BACKEND_CLASS == null)
            return null;

        try
        {
            final var backend = LOG4J2_BACKEND_FACTORY.get().create(clz.getName());
            return wrap(backend);
        }
        catch (NoClassDefFoundError | ExceptionInInitializerError | Exception e)
        {
            // Ignore
            return null;
        }
    }

    @Override
    public String getLoggerName()
    {
        return backend.getLoggerName();
    }

    @Override
    public boolean isLoggable(Level lvl)
    {
        return backend.isLoggable(lvl);
    }

    @Override
    public void log(LogData logData)
    {
        if (logger == null)
        {
            backend.log(logData);
            return;
        }
        logger.get().log(toLog4jLogEvent(logData));
    }

    @Override
    public void handleError(RuntimeException error, LogData badData)
    {
        if (logger == null)
        {
            backend.handleError(error, badData);
            return;
        }
        logger.get().log(toLog4jLogEvent(error, badData));
    }

    private @Nullable LogEvent toLog4jLogEvent(LogData logData)
    {
        return toLog4jLogEvent1(TO_LOG4J_LOG_EVENT_METHOD, getLoggerName(), logData);
    }

    private @Nullable LogEvent toLog4jLogEvent(RuntimeException error, LogData logData)
    {
        return toLog4jLogEvent1(TO_LOG4J_LOG_EVENT_WITH_ERROR_METHOD, getLoggerName(), error, logData);
    }

    @SuppressWarnings("CatchAndPrintStackTrace")
    private @Nullable LogEvent toLog4jLogEvent1(@Nullable Method method, Object... args)
    {
        if (method == null)
            return null;

        try
        {
            final var result = method.invoke(null, args);
            return new LogEventWrapper((LogEvent) result, getMarker());
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return null;
    }

    static void setMarker(Marker marker)
    {
        if (CustomLog4j2Backend.marker != null)
            throw new IllegalStateException("Failed to set marker to " + marker + ": already set to " +
                CustomLog4j2Backend.marker);
        CustomLog4j2Backend.marker = marker;
    }

    static void setMarkerName(String name)
    {
        setMarker(MarkerManager.getMarker(name));
    }

    public static @Nullable Marker getMarker()
    {
        final Marker marker0 = CustomLog4j2Backend.marker;
        if (marker0 == null)
            //noinspection CallToPrintStackTrace
            new IllegalStateException("Marker not set, call Log4J2Configurator.setMarkerName() first")
                .printStackTrace();

        return marker0;
    }

    private @Nullable Logger getLogger(LoggerBackend backend)
    {
        if (NESTED_LOGGER_FIELD == null)
            return null;
        try
        {
            return (Logger) NESTED_LOGGER_FIELD.get(backend);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to access logger in backend " + backend.getClass(), e);
        }
    }

    private static @Nullable Class<? extends LoggerBackend> findLog4j2BackendClass()
    {
        final var cls = ReflectionBuilder
            .findClass(CustomLog4j2Backend.ORIGINAL_LOG4J2_BACKEND_CLASS_NAME)
            .getNullable();
        if (cls == null)
            return null;
        return cls.asSubclass(LoggerBackend.class);
    }

    private static BackendFactory getLog4j2BackendFactory()
    {
        final Class<?> clz = ReflectionBuilder
            .findClass("com.google.common.flogger.backend.log4j2.Log4j2BackendFactory")
            .get();

        try
        {
            return (BackendFactory) ReflectionBuilder.findConstructor()
                .inClass(clz)
                .withoutParameters()
                .setAccessible()
                .get()
                .newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
