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

package nl.pim16aap2.animatedarchitecture.core.data.cache.timed;

import org.jetbrains.annotations.Nullable;

import java.time.Clock;

/**
 * Represents a basic implementation of a {@link AbstractTimedValue}.
 * <p>
 * The value wrapped inside will be available until the timeOut is exceeded. After this point, null will always be
 * returned.
 *
 * @param <T>
 *     The type of the value to store.
 */
class TimedValue<T> extends AbstractTimedValue<T>
{
    private final T value;

    /**
     * Constructor of {@link TimedValue}.
     *
     * @param clock
     *     The {@link Clock} to use to determine anything related to time (insertion, age).
     * @param val
     *     The value of this {@link TimedValue}.
     * @param timeOut
     *     The amount of time (in milliseconds) before this entry expires.
     */
    public TimedValue(Clock clock, T val, long timeOut)
    {
        super(clock, timeOut);
        value = val;
    }

    @Override
    public @Nullable T getValue(boolean refresh)
    {
        if (timedOut())
            return null;
        if (refresh)
            refresh();
        return value;
    }
}
