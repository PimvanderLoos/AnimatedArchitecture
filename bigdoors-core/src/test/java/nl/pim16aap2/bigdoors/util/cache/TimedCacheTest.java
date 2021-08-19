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

import lombok.Setter;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.ref.SoftReference;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Function;

class TimedCacheTest
{
    private final MockClock clock = new MockClock(0);

    /**
     * Make sure that expired values cannot be retrieved.
     */
    @Test
    void testExpiry()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, false, false);
        timedCache.put("key", "value");
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(80);
        timedCache.put("key2", "value2");
        Assertions.assertTrue(timedCache.get("key2").isPresent());
        Assertions.assertEquals(2, timedCache.getSize());

        clock.addMillis(70);
        Assertions.assertEquals(2, timedCache.getSize());
        Assertions.assertFalse(timedCache.get("key").isPresent());
        Assertions.assertTrue(timedCache.get("key2").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(50);
        timedCache.cleanupCache();
        Assertions.assertEquals(0, timedCache.getSize());
    }

    /**
     * Test whether the keepAfterTimeOut option keeps the values around after the timeOut when possible.
     */
    @Test
    void testKeepAfterTimeOut()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, true, false, true);
        // Set a hard reference, so the value cannot be garbage collected.
        String value = "can't touch this";

        // Just ensure the value is stored in the cache and didn't get mangled or whatever.
        timedCache.put("key", value);
        Assertions.assertEquals(1, timedCache.getSize());

        // Ensure the value can be retrieved properly.
        String result = timedCache.get("key").orElse(null);
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, value);

        // Ensure the value has timed out.
        clock.addMillis(1000);

        final TimedSoftValue<String> tsv = getTimedSoftValue(timedCache, "key");

        // Ensure that timedOut correctly reflects that the value is overstaying its welcome.
        Assertions.assertTrue(tsv.timedOut());
        // Ensure that even though the value has timed out, it cannot be evicted
        // because keepAfterTimeOut is enabled.
        Assertions.assertFalse(tsv.canBeEvicted());
        // Ensure that the hard reference is removed when the value times out.
        Assertions.assertNull(tsv.getRawHardReference());

        // Ensure that refreshing still works and that refreshing also re-initializes the hard reference.
        tsv.getValue(true);
        Assertions.assertFalse(tsv.timedOut());
        Assertions.assertNotNull(tsv.getRawHardReference());

        // Ensure that even after the value has been cleared, it cannot be
        // evicted if it hasn't timed out yet (as it was refreshed).
        tsv.getRawValue().clear();
        Assertions.assertFalse(tsv.canBeEvicted());

        clock.addMillis(1000);

        // Ensure that once it has both timed out and reclaimed, the value can finally be evicted.
        Assertions.assertTrue(tsv.canBeEvicted());
    }

    private <T> TimedSoftValue<T> getTimedSoftValue(TimedCache<String, T> cache, String keyName)
    {
        @Nullable AbstractTimedValue<T> retrieved = cache.getRaw(keyName);
        Assertions.assertNotNull(retrieved);
        Assertions.assertTrue(retrieved instanceof TimedSoftValue);
        return (TimedSoftValue<T>) retrieved;
    }

    /**
     * Make sure that configuring the cache to use {@link SoftReference}s actually wraps the values in them and that
     * they behave properly when cleared.
     */
    @Test
    void testSoftReference()
    {
        val longDuration = Duration.ofHours(100);
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new TimedCache<>(clock, Duration.ZERO, longDuration, false, false, false));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new TimedCache<>(clock, Duration.ZERO, Duration.ZERO, true, false, false));

        TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ZERO, longDuration,
                                                                 true, false, false);
        timedCache.put("key", "value");

        TimedSoftValue<String> retrieved = getTimedSoftValue(timedCache, "key");
        Assertions.assertNull(retrieved.getRawHardReference());

        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        retrieved.getRawValue().clear();
        Assertions.assertFalse(timedCache.get("key").isPresent());
        Assertions.assertEquals(0, timedCache.getSize());


        // Now test that setting soft-reference to false doesn't wrap values in them.
        timedCache = new TimedCache<>(clock, Duration.ofMillis(100), null, false, false, false);
        timedCache.put("key2", "value");
        @Nullable AbstractTimedValue<String> retrieved2 = timedCache.getRaw("key2");
        Assertions.assertNotNull(retrieved2);
        Assertions.assertFalse(retrieved2 instanceof TimedSoftValue);
    }

    /**
     * Make sure that refreshing values works properly.
     * <p>
     * Refreshing values should make sure that they don't expire after the expiry time after their last access, not
     * after their insertion.
     */
    @Test
    void testRefresh()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, true, false);
        timedCache.put("key", "value");
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(90);
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(20);
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(110);
        Assertions.assertFalse(timedCache.get("key").isPresent());
        Assertions.assertEquals(0, timedCache.getSize());


        timedCache.put("key", "value");
        Assertions.assertEquals(1, timedCache.getSize());
        clock.addMillis(70);
        timedCache.putIfPresent("key", "updatedValue");
        clock.addMillis(70);
        optionalEquals(timedCache.get("key"), "updatedValue");
    }

    /**
     * Make sure that {@link TimedCache#computeIfAbsent(Object, Function)} works properly.
     * <p>
     * It should insert new values if they don't exist yet, update existing ones if they have expired and not do
     * anything in any other case.
     */
    @Test
    void computeIfAbsent()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, false, false);

        // Make sure inserting a new item properly returns an empty optional.
        Assertions.assertFalse(timedCache.computeIfAbsent("key", (k) -> "value").isPresent());
        // Ensure the value was actually inserted properly.
        optionalEquals(timedCache.get("key"), "value");

        // Make sure that calling the method again returns the proper result.
        Assertions.assertTrue(timedCache.computeIfAbsent("key", (k) -> "value").isPresent());
        // Make sure that the entry isn't duplicated.
        Assertions.assertEquals(1, timedCache.getSize());

        // Make sure that trying to insert a different value for an existing key doesn't do anything.
        optionalEquals(timedCache.computeIfAbsent("key", (k) -> "newVal"), "value");

        // Make sure that we can insert new values again once the entry has timed out.
        clock.addMillis(110);
        Assertions.assertFalse(timedCache.computeIfAbsent("key", (k) -> "newVal").isPresent());
        optionalEquals(timedCache.get("key"), "newVal");
    }

    @Test
    void computeIfPresent()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, false, false);
        timedCache.put("key", "value");

        optionalEquals(timedCache.computeIfPresent("key", (k, v) -> "newValue"), "newValue");

        Assertions.assertFalse(timedCache.computeIfPresent("key2", (k, v) -> "newValue").isPresent());

        clock.addMillis(110);

        Assertions.assertFalse(timedCache.computeIfPresent("key", (k, v) -> "newValue").isPresent());
    }

    @Test
    void compute()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, false, false);
        String returned = timedCache.compute("key", (k, v) -> v == null ? "value" : (v + v));
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        Assertions.assertEquals("value", returned);

        returned = timedCache.compute("key", (k, v) -> v == null ? "value" : (v + v));
        Assertions.assertEquals("valuevalue", returned);

        clock.addMillis(110);
        returned = timedCache.compute("key", (k, v) -> v == null ? "newVal" : (v + v));
        Assertions.assertEquals("newVal", returned);
    }

    @Test
    void putIfAbsent()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, false, false);

        Assertions.assertFalse(timedCache.putIfAbsent("key", "value").isPresent());

        optionalEquals(timedCache.putIfAbsent("key", "value"), "value");


        clock.addMillis(110);

        Assertions.assertFalse(timedCache.putIfAbsent("key", "value").isPresent());
    }

    @Test
    void putIfPresent()
    {
        TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                 null, false, false, false);

        timedCache.put("key", "value");

        optionalEquals(timedCache.putIfPresent("key", "newValue"), "newValue");

        Assertions.assertFalse(timedCache.putIfPresent("key2", "value").isPresent());

        clock.addMillis(110);

        Assertions.assertFalse(timedCache.putIfPresent("key", "value").isPresent());
    }

    /**
     * Ensure that removing keys from the cache works as intended.
     */
    @Test
    void remove()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       null, false, false, false);
        timedCache.put("key", "value");
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());

        final Optional<String> result = timedCache.remove("key");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("value", result.get());
        Assertions.assertEquals(0, timedCache.getSize());
        Assertions.assertFalse(timedCache.containsKey("key"));

        timedCache.put("key", "value");
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        timedCache.clear();
        Assertions.assertEquals(0, timedCache.getSize());
    }

    /**
     * Make sure that the cleanup task properly cleans up any items it should.
     */
    @Test
    void cleanupTask()
    {
        final TimedCache<String, String> timedCache = new TimedCache<>(clock, Duration.ofMillis(100),
                                                                       Duration.ofMillis(1), false, false,
                                                                       false);

        timedCache.put("key", "value");
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        sleep(10);
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(200);
        sleep(10);
        Assertions.assertEquals(0, timedCache.getSize());
    }

    /**
     * Sleeps the thread for a defined amount of time.
     * <p>
     * When interrupted, the test will fail.
     *
     * @param millis The number of milliseconds to sleep for.
     */
    public static void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Assertions.fail("Failed to sleep thread");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Makes sure that an {@link Optional} is both present and that its result matches the provided value.
     *
     * @param optional The {@link Optional} to check.
     * @param val      The value to compare against the value inside the optional.
     * @param <T>      The type of the values to compare.
     */
    public static <T> void optionalEquals(final Optional<T> optional, final T val)
    {
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(val, optional.get());
    }

    /**
     * Clock that displays a determined millisecond value which can be set/updated manually.
     *
     * @author Pim
     */
    private static final class MockClock extends Clock
    {
        @Setter
        private long currentMillis;

        public void addMillis(final long millis)
        {
            currentMillis += millis;
        }

        public MockClock(final long currentMillis)
        {
            this.currentMillis = currentMillis;
        }

        @Override
        public long millis()
        {
            return currentMillis;
        }

        @Override
        public ZoneId getZone()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant()
        {
            throw new UnsupportedOperationException();
        }
    }
}
