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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;

/**
 * Represents a value in a {@link TimedCache}. It holds the value and the time of insertion.
 *
 * @param <T> Type of the value.
 */
abstract class AbstractTimedValue<T>
{
    protected final long timeOut;
    protected long insertTime;
    protected final @NotNull Clock clock;

    protected AbstractTimedValue(final @NotNull Clock clock, final long timeOut)
    {
        this.clock = clock;
        this.timeOut = timeOut;
        refresh();
    }

    /**
     * Refreshes the insertion time of this timed value. This updates the {@link #insertTime} to the current time.
     */
    protected void refresh()
    {
        insertTime = clock.millis();
    }

    /**
     * Gets the value wrapped inside this {@link AbstractTimedValue}.
     * <p>
     * If this value is not accessible (e.g. exceeds {@link #timeOut} or the value itself has become invalid), null is
     * returned.
     *
     * @param refresh Whether or not to refresh the value. See {@link #refresh()}.
     * @return The value wrapped inside this {@link AbstractTimedValue}.
     */
    public abstract @Nullable T getValue(boolean refresh);

    /**
     * Check if this {@link AbstractTimedValue} was inserted more than milliseconds ago. If so, it's considered "timed
     * out".
     *
     * @return True if the value has timed out.
     */
    public boolean timedOut()
    {
        if (timeOut == 0)
            return false;
        if (timeOut < 0)
            return true;
        return (clock.millis() - insertTime) > timeOut;
    }

    /**
     * Checks if this value can be evicted from the cache.
     *
     * @return True if this value is ready for eviction from the cache.
     */
    public boolean canBeEvicted()
    {
        return timedOut();
    }
}
