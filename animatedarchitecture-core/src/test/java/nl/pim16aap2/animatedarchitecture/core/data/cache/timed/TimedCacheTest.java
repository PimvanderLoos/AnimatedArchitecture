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

import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

class TimedCacheTest
{
    /**
     * Make sure that expired values cannot be retrieved.
     */
    @Test
    void testExpiry()
    {
        final MockClock clock = new MockClock(0);

        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

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

    @Test
    void testShutDown()
    {
        final MockClock clock = new MockClock(0);

        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

        timedCache.shutDown();
        Assertions.assertThrows(IllegalStateException.class, () -> timedCache.put("a", "b"));
    }

    @Test
    void testKeepAfterTimeOut()
    {
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .softReference(true)
            .keepAfterTimeOut(true)
            .build();

        // Set a hard reference, so the value cannot be garbage collected.
        String value = "can't touch this";

        // Just ensure the value is stored in the cache and didn't get mangled or whatever.
        timedCache.put("key", value);
        Assertions.assertEquals(1, timedCache.getSize());

        // Ensure the value can be retrieved properly.
        @Nullable String result = timedCache.get("key").orElse(null);
        Assertions.assertNotNull(result);
        Assertions.assertSame(value, result);

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
        Assertions.assertInstanceOf(TimedSoftValue.class, retrieved);
        return (TimedSoftValue<T>) retrieved;
    }

    @Test
    void testZeroTimeOutValidation()
    {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> TimedCache
                .builder()
                .timeOut(Duration.ZERO)
                .cleanup(Duration.ZERO)
                .softReference(true)
                .build()
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> TimedCache
                .builder()
                .timeOut(Duration.ZERO)
                .cleanup(Duration.ofMillis(1))
                .build()
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> TimedCache
                .builder()
                .timeOut(null)
                .cleanup(Duration.ofMillis(1))
                .build()
        );

        Assertions.assertDoesNotThrow(
            () -> TimedCache
                .builder()
                .timeOut(Duration.ZERO)
                .cleanup(Duration.ofMillis(1))
                .softReference(true)
                .build()
        );
    }

    @Test
    void testSoftReferenceClearing()
    {
        final MockClock clock = new MockClock(0);
        final Duration longDuration = Duration.ofHours(100);

        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .cleanup(longDuration)
            .softReference(true)
            .build();

        timedCache.put("key", "value");

        TimedSoftValue<String> retrieved = getTimedSoftValue(timedCache, "key");
        Assertions.assertEquals("value", retrieved.getValue(false));
        // The hard reference should be null, as 'keepAfterTimeOut' is false.
        Assertions.assertNull(retrieved.getRawHardReference());

        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        retrieved.getRawValue().clear();
        Assertions.assertFalse(timedCache.get("key").isPresent());
        Assertions.assertEquals(0, timedCache.getSize());
    }

    @Test
    void testTimedValueCreator()
    {
        final MockClock clock = new MockClock(0);

        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

        timedCache.put("key2", "value");
        @Nullable AbstractTimedValue<String> retrieved2 = timedCache.getRaw("key2");
        Assertions.assertNotNull(retrieved2);
        Assertions.assertInstanceOf(TimedValue.class, retrieved2);
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
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .refresh(true)
            .build();

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
        final MockClock clock = new MockClock(0);

        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

        // Make sure inserting a new item properly returns the new value
        Assertions.assertEquals("value0", timedCache.computeIfAbsent("key", (k) -> "value0"));
        // Ensure the value was actually inserted properly.
        optionalEquals(timedCache.get("key"), "value0");

        // Make sure that calling the method again returns the proper result.
        Assertions.assertEquals("value0", timedCache.computeIfAbsent("key", (k) -> "value0"));
        // Make sure that the entry isn't duplicated.
        Assertions.assertEquals(1, timedCache.getSize());

        // Make sure that trying to insert a different value for an existing key doesn't do anything.
        Assertions.assertEquals("value0", timedCache.computeIfAbsent("key", (k) -> "newVal1"));

        // Make sure that we can insert new values again once the entry has timed out.
        clock.addMillis(110);
        Assertions.assertEquals("newVal", timedCache.computeIfAbsent("key", (k) -> "newVal"));
        optionalEquals(timedCache.get("key"), "newVal");

        Assertions.assertThrows(NullPointerException.class, () -> timedCache.computeIfAbsent(null, (k) -> "value"));

        //noinspection DataFlowIssue
        Assertions.assertThrows(
            NullPointerException.class,
            () -> timedCache.computeIfAbsent(UUID.randomUUID().toString(), null)
        );
    }

    @Test
    void computeIfPresent()
    {
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

        timedCache.put("key", "value");

        optionalEquals(timedCache.computeIfPresent("key", (k, v) -> "newValue"), "newValue");

        Assertions.assertFalse(timedCache.computeIfPresent("key2", (k, v) -> "newValue").isPresent());

        clock.addMillis(110);

        Assertions.assertFalse(timedCache.computeIfPresent("key", (k, v) -> "newValue").isPresent());
    }

    @Test
    void compute()
    {
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

        String returned = timedCache.compute("key", (k, v) -> v == null ? "value" : (v + v));
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        Assertions.assertEquals("value", returned);

        returned = timedCache.compute("key", (k, v) -> v == null ? "value" : (v + v));
        //noinspection SpellCheckingInspection
        Assertions.assertEquals("valuevalue", returned);

        clock.addMillis(110);
        returned = timedCache.compute("key", (k, v) -> v == null ? "newVal" : (v + v));
        Assertions.assertEquals("newVal", returned);
    }

    @Test
    void putIfAbsent()
    {
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

        Assertions.assertFalse(timedCache.putIfAbsent("key", "value0").isPresent());
        Assertions.assertEquals("value0", timedCache.get("key").orElse(null));

        optionalEquals(timedCache.putIfAbsent("key", "value1"), "value0");
        Assertions.assertEquals("value0", timedCache.get("key").orElse(null));

        clock.addMillis(110);

        Assertions.assertFalse(timedCache.putIfAbsent("key", "value").isPresent());
    }

    @Test
    void putIfPresent()
    {
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

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
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .build();

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
    void testCleanupTask()
    {
        final MockClock clock = new MockClock(0);
        final var timedCache = TimedCache
            .<String, String>builderWithClock()
            .clock(clock)
            .timeOut(Duration.ofMillis(100))
            .cleanup(Duration.ofMillis(1))
            .build();

        timedCache.put("key", "value");
        Assertions.assertTrue(timedCache.get("key").isPresent());
        Assertions.assertEquals(1, timedCache.getSize());
        sleep(10);
        Assertions.assertEquals(1, timedCache.getSize());

        clock.addMillis(200);
        sleep(10);
        Assertions.assertEquals(0, timedCache.getSize());
    }

    @Test
    void testEmptyCache()
    {
        final TimedCache<String, String> empty = TimedCache.emptyCache();

        Assertions.assertEquals("value1", empty.compute("key1", (key, val) -> "value1"));
        Assertions.assertEquals("value2", empty.computeIfAbsent("key2", key -> "value2"));
        Assertions.assertTrue(empty.computeIfPresent("key2", (key, val) -> "value2_2").isEmpty());

        Assertions.assertEquals("value3", empty.put("key3", "value3"));
        Assertions.assertTrue(empty.putIfAbsent("key4", "value4").isEmpty());
        Assertions.assertTrue(empty.putIfPresent("key4", "value4_2").isEmpty());

        Assertions.assertEquals(0, empty.getSize());
        Assertions.assertTrue(empty.get("key1").isEmpty());
        Assertions.assertNull(empty.getRaw("key1"));
        Assertions.assertTrue(empty.remove("key1").isEmpty());

        empty.shutDown();
        Assertions.assertThrows(IllegalStateException.class, () -> empty.put("a", "b"));
    }

    /**
     * Sleeps the thread for a defined amount of time.
     * <p>
     * When interrupted, the test will fail.
     *
     * @param millis
     *     The number of milliseconds to sleep for.
     */
    public static void sleep(long millis)
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
     * @param optional
     *     The {@link Optional} to check.
     * @param val
     *     The value to compare against the value inside the optional.
     * @param <T>
     *     The type of the values to compare.
     */
    public static <T> void optionalEquals(Optional<T> optional, T val)
    {
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(val, optional.get());
    }

    /**
     * Clock that displays a determined millisecond value which can be set/updated manually.
     */
    private static final class MockClock extends Clock
    {
        @Setter
        private long currentMillis;

        public void addMillis(long millis)
        {
            currentMillis += millis;
        }

        public MockClock(long currentMillis)
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
