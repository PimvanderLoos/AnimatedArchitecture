package nl.pim16aap2.util;

import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * A kind of {@link StringBuilder} with some added methods to append objects using suppliers in try/catch blocks.
 *
 * @author Pim
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
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
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
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
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
