package nl.pim16aap2.util.logging;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

@ToString
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true) final class VariableLevelFilter extends AbstractFilter
{
    /**
     * The level to filter on.
     * <p>
     * Any log event with a level lower than this level will be filtered out.
     */
    @Getter
    @Setter
    private volatile Level level;

    /**
     * Creates a new VariableLevelFilter with the specified level.
     *
     * @param level
     *     The level to filter on.
     */
    public VariableLevelFilter(Level level)
    {
        super(Filter.Result.NEUTRAL, Filter.Result.DENY);
        this.level = level;
    }

    private Result filter(final Level testLevel)
    {
        return testLevel.isMoreSpecificThan(this.level) ? onMatch : onMismatch;
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, String msg, Object... params)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
        Object p4)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
        Object p4, Object p5)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
        Object p4, Object p5, Object p6)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
        Object p4, Object p5, Object p6, Object p7)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
        Object p4, Object p5, Object p6, Object p7, Object p8)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(
        Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
        Object p4, Object p5, Object p6, Object p7, Object p8, Object p9)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t)
    {
        return filter(level);
    }

    @Override
    public Filter.Result filter(LogEvent event)
    {
        return filter(event.getLevel());
    }
}
