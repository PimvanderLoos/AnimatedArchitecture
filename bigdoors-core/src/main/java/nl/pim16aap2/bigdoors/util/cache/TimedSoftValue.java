/*
 *  MIT License
 *
 * Copyright (c) 2020 Pim van der Loos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nl.pim16aap2.bigdoors.util.cache;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.time.Clock;

/**
 * Represents an {@link AbstractTimedValue} wrapped in a {@link SoftReference}.
 *
 * @param <T>
 *     The type of the value to store.
 * @author Pim
 */
class TimedSoftValue<T> extends AbstractTimedValue<T>
{
    private final boolean keepAfterTimeOut;
    private final SoftReference<T> value;

    // The hard reference looks unused because it is never accessed.
    // However, its sole purpose is to keep a reference to the value
    // to avoid garbage collection reclaiming it before it has timed out.
    @SuppressWarnings("unused")
    private @Nullable T hardReference;

    /**
     * Constructor of {@link TimedSoftValue}.
     *
     * @param clock
     *     The {@link Clock} to use to determine anything related to time (insertion, age).
     * @param val
     *     The value of this {@link TimedSoftValue}.
     * @param timeOut
     *     The amount of time (in milliseconds) before this entry expires.
     * @param keepAfterTimeOut
     *     Whether to wait until the garbage collector has reclaimed the item. When this is true, {@link
     *     #canBeEvicted()} won't return true until the value has both timed out and been reclaimed and {@link
     *     #getValue(boolean)} will return the value for the same duration.
     *     <p>
     *     When this is false, {@link #canBeEvicted()} will return true as soon as the value has timed out, regardless
     *     of whether it may still be available. Similarly, {@link #getValue(boolean)} will return null after the value
     *     has timed out.
     */
    public TimedSoftValue(Clock clock, T val, long timeOut, boolean keepAfterTimeOut)
    {
        super(clock, timeOut);
        this.keepAfterTimeOut = keepAfterTimeOut;
        value = new SoftReference<>(val);
        if (keepAfterTimeOut)
            hardReference = val;
    }

    @Override
    public @Nullable T getValue(boolean refresh)
    {
        if (!keepAfterTimeOut && timedOut())
            return null;
        final @Nullable T val = value.get();
        if (val == null)
            return null;
        if (refresh)
            refresh(val);
        return val;
    }

    private void refresh(T val)
    {
        super.refresh();
        if (keepAfterTimeOut)
            hardReference = val;
    }

    @Override
    public boolean timedOut()
    {
        final boolean timedOut = super.timedOut();
        if (timedOut)
            hardReference = null;
        return timedOut;
    }

    @Override
    public boolean canBeEvicted()
    {
        if (keepAfterTimeOut)
            return timedOut() && value.get() == null;
        return timedOut();
    }

    /**
     * Gets the raw {@link SoftReference}-wrapped value.
     *
     * @return The raw value, wrapped in a {@link SoftReference}.
     */
    public SoftReference<T> getRawValue()
    {
        return value;
    }

    /**
     * Gets the raw hard reference to the value.
     * <p>
     * This value may not exist.
     *
     * @return The raw hard reference to the value.
     */
    // Useful for testing.
    @Nullable T getRawHardReference()
    {
        return hardReference;
    }
}
