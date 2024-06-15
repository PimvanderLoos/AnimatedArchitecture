package nl.pim16aap2.util;

import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * A kind of {@link StringBuilder} with some added methods to append objects using suppliers in try/catch blocks.
 * <p>
 * Note that these catch all throwables, so carefully consider if you really need this class before using it!
 */
@Flogger
public final class SafeStringBuilder implements CharSequence, Appendable, Serializable
{
    private final StringBuilder sb;

    public SafeStringBuilder()
    {
        this.sb = new StringBuilder();
    }

    public SafeStringBuilder(String str)
    {
        this.sb = new StringBuilder(str);
    }

    public SafeStringBuilder(int capacity)
    {
        this.sb = new StringBuilder(capacity);
    }

    public SafeStringBuilder(CharSequence charSequence)
    {
        this.sb = new StringBuilder(charSequence);
    }

    @Override
    public SafeStringBuilder append(CharSequence csq)
    {
        sb.append(csq);
        return this;
    }

    public SafeStringBuilder append(Supplier<@Nullable Object> fun)
    {
        try
        {
            sb.append(fun.get());
            return this;
        }
        catch (Throwable t)
        {
            log.atSevere().withCause(t).log();
            sb.append("ERROR!");
            return this;
        }
    }

    @Override
    public SafeStringBuilder append(CharSequence csq, int start, int end)
    {
        sb.append(csq, start, end);
        return this;
    }

    public SafeStringBuilder append(Supplier<CharSequence> fun, int start, int end)
    {
        try
        {
            sb.append(fun.get(), start, end);
            return this;
        }
        catch (Throwable t)
        {
            log.atSevere().withCause(t).log();
            sb.append("ERROR!");
            return this;
        }
    }

    @Override
    public SafeStringBuilder append(char c)
    {
        sb.append(c);
        return this;
    }

    public SafeStringBuilder append(@Nullable Object o)
    {
        sb.append(o);
        return this;
    }

    public SafeStringBuilder appendIndented(int indent, Supplier<@Nullable Object> fun)
    {
        @Nullable Object obj;
        try
        {
            obj = fun.get();
        }
        catch (Throwable t)
        {
            log.atSevere().withCause(t).log();
            obj = "ERROR";
        }
        return appendIndented(indent, obj);
    }

    public SafeStringBuilder appendIndented(int indent, @Nullable Object obj)
    {
        if (obj == null)
            return this;
        try
        {
            final String prefix = " ".repeat(indent);
            final String str = obj.toString();
            str.lines().forEach(line -> sb.append(prefix).append(line).append('\n'));
        }
        catch (Throwable t)
        {
            log.atSevere().withCause(t).log();
        }
        return this;
    }

    @Override
    public int length()
    {
        return sb.length();
    }

    @Override
    public char charAt(int index)
    {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return sb.subSequence(start, end);
    }

    @Override
    public String toString()
    {
        return sb.toString();
    }
}
