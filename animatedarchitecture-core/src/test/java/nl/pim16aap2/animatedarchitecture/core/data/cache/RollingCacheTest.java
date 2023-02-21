package nl.pim16aap2.animatedarchitecture.core.data.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class RollingCacheTest
{
    private static final Consumer<Integer> NO_OP_CONSUMER = (ignored) ->
    {
    };

    @Test
    void ensureLimit()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RollingCache<>(-12));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RollingCache<>(0));
        Assertions.assertEquals(1, new RollingCache<>(1).getLimit());
        Assertions.assertEquals(17, new RollingCache<>(17).getLimit());
    }

    @Test
    void cropCollection()
    {
        final List<Integer> lst = List.of(0, 1, 2, 3, 4, 5);
        Assertions.assertEquals(List.of(3, 4, 5), RollingCache.cropCollection(3, lst));
        Assertions.assertEquals(lst, RollingCache.cropCollection(6, lst));
        Assertions.assertEquals(lst, RollingCache.cropCollection(20, lst));
        Assertions.assertSame(lst, RollingCache.cropCollection(20, lst));
        Assertions.assertEquals(List.of(5), RollingCache.cropCollection(1, lst));
    }

    @Test
    void addAll()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);

        Assertions.assertFalse(cache.addAll(Collections.emptyList()));
        Assertions.assertEquals(0, cache.size());

        Assertions.assertTrue(cache.addAll(List.of(1)));
        Assertions.assertEquals(1, cache.size());
        testEquals(cache, 1);

        Assertions.assertTrue(cache.addAll(List.of(2)));
        Assertions.assertEquals(2, cache.size());
        testEquals(cache, 1, 2);

        Assertions.assertTrue(cache.addAll(List.of(10, 11, 12, 13)));
        Assertions.assertEquals(3, cache.size());
        testEquals(cache, 11, 12, 13);

        Assertions.assertTrue(cache.getModCount() > 0);
    }

    @Test
    void testRollover()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        for (int idx = 0; idx < 100; ++idx)
            cache.add(idx);
        Assertions.assertEquals(3, cache.size());
        testEquals(cache, 97, 98, 99);

        final int mc = cache.getModCount();
        Assertions.assertTrue(mc > 0);

        for (int idx = 0; idx < 50; ++idx)
            cache.insertLast(idx);
        Assertions.assertEquals(3, cache.size());
        testEquals(cache, 49, 48, 47);

        Assertions.assertTrue(cache.getModCount() > mc);
    }

    @Test
    void doubleSidedInserts()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        cache.insertLast(1);
        cache.insertLast(0);
        cache.insertFirst(2);
        testEquals(cache, 0, 1, 2);

        cache.insertLast(3);
        testEquals(cache, 3, 0, 1);

        cache.insertFirst(2);
        testEquals(cache, 0, 1, 2);
    }

    @Test
    void onSecondHalf()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        cache.addAll(List.of(0, 1, 2, 3, 4, 5));
        Assertions.assertTrue(cache.onSecondHalf(3));
        Assertions.assertFalse(cache.onSecondHalf(0));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testShift()
    {
        // We don't really care about _if_ shifting works; the tests
        // for deleting etc. will take care of that.
        // What we want to test here, is that shifting takes the
        // shortest path, as there are two directions we can use
        // for shifting, but one of those is usually going to be
        // faster than the other.
        final RollingCache<Integer> cache = new RollingCache<>(7);
        cache.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        cache.remove(5);
        testEquals(new Integer[]{0, 1, 2, 3, 4, 6, null}, cache.rawArray());
        cache.remove(1);
        testEquals(new Integer[]{null, 0, 2, 3, 4, 6, null}, cache.rawArray());
        testEquals(cache, 0, 2, 3, 4, 6);

        cache.add(7);
        cache.add(8);
        cache.add(9);
        // Indices:              5  6  0  1  2  3  4
        testEquals(new Integer[]{8, 9, 2, 3, 4, 6, 7}, cache.rawArray());

        cache.remove(4);
        // Indices:              5        0  1  2  3  4
        testEquals(new Integer[]{9, null, 2, 3, 4, 6, 8}, cache.rawArray());

        cache.remove(1);
        testEquals(new Integer[]{9, null, null, 2, 4, 6, 8}, cache.rawArray());
        testEquals(cache, 2, 4, 6, 8, 9);
    }

    @Test
    void removeIndex()
    {
        final RollingCache<Integer> cache = new RollingCache<>(7);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.remove(0));
        cache.addAll(List.of(0, 1, 2, 3, 4, 5, 6));

        testEquals(cache, 0, 1, 2, 3, 4, 5, 6);

        cache.remove(0);
        testEquals(cache, 1, 2, 3, 4, 5, 6);

        cache.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        cache.remove(6);
        testEquals(cache, 0, 1, 2, 3, 4, 5);
        cache.remove(4);
        testEquals(cache, 0, 1, 2, 3, 5);

        cache.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        cache.remove(1);
        testEquals(cache, 0, 2, 3, 4, 5, 6);

        cache.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        cache.remove(5);
        testEquals(cache, 0, 1, 2, 3, 4, 6);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.remove(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.remove(6));
        cache.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.remove(7));
        cache.clear();
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.remove(0));
    }

    @Test
    void removeObject()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        cache.addAll(List.of(10, 11));

        Assertions.assertFalse(cache.remove((Integer) 3));
        Assertions.assertTrue(cache.remove((Integer) 10));
        Assertions.assertEquals(cache.size(), 1);

        cache.addAll(List.of(10, 11, 12, 13, 14));
        Assertions.assertTrue(cache.remove((Integer) 11));
        Assertions.assertEquals(cache.size(), 4);

        Assertions.assertTrue(cache.remove((Integer) 13));
        Assertions.assertEquals(cache.size(), 3);

        testEquals(cache, 10, 12, 14);
    }

    @Test
    void simpleRemove()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        Assertions.assertThrows(NoSuchElementException.class, cache::removeFirst);
        Assertions.assertThrows(NoSuchElementException.class, cache::removeLast);

        // Add a bunch to ensure we loop over a few times.
        for (int idx = 0; idx < 37; ++idx)
            cache.add(idx);
        Assertions.assertEquals(5, cache.size());

        int mc = cache.getModCount();
        Assertions.assertEquals(32, cache.removeFirst());
        Assertions.assertEquals(33, cache.removeFirst());
        Assertions.assertTrue(cache.getModCount() > mc);
        Assertions.assertEquals(3, cache.size());

        mc = cache.getModCount();
        Assertions.assertEquals(36, cache.removeLast());
        Assertions.assertEquals(35, cache.removeLast());
        Assertions.assertTrue(cache.getModCount() > mc);
        Assertions.assertEquals(1, cache.size());

        testEquals(cache, 34);

        // Make sure both removeLast and removeFirst work when there's only 1 item remaining.
        Assertions.assertEquals(34, cache.removeLast());
        Assertions.assertEquals(0, cache.size());
        cache.add(34);
        Assertions.assertEquals(34, cache.removeFirst());
        Assertions.assertEquals(0, cache.size());

        //noinspection ConstantConditions
        testEquals(new Object[]{null, null, null, null, null}, cache.rawArray());

        Assertions.assertThrows(NoSuchElementException.class, cache::removeFirst);
        Assertions.assertThrows(NoSuchElementException.class, cache::removeLast);
    }

    @Test
    void clear()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        cache.addAll(List.of(0, 1, 2));
        cache.add(3);

        Assertions.assertTrue(cache.getPtrHead() != 0);
        Assertions.assertTrue(cache.getPtrTail() != 0);
        Assertions.assertTrue(cache.size() != 0);

        final int mc = cache.getModCount();
        cache.clear();

        Assertions.assertTrue(cache.getModCount() > mc);
        Assertions.assertEquals(0, cache.size());
        Assertions.assertEquals(0, cache.getPtrHead());
        Assertions.assertEquals(0, cache.getPtrTail());

        //noinspection ConstantConditions
        testEquals(new Object[]{null, null, null}, cache.rawArray());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void peek()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        Assertions.assertThrows(NoSuchElementException.class, cache::peekFirst);
        Assertions.assertThrows(NoSuchElementException.class, cache::peekLast);

        cache.addAll(List.of(0, 1, 2));
        Assertions.assertEquals(0, cache.peekFirst());
        Assertions.assertEquals(2, cache.peekLast());
    }

    @Test
    void get()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.get(0));
        cache.addAll(List.of(0, 1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> cache.get(4));

        for (int idx = 0; idx < 3; ++idx)
            Assertions.assertEquals(idx, cache.get(idx));

        // Make sure that all values pass idx 0 if you keep adding more values.
        for (int idx = 3; idx < 7; ++idx)
        {
            cache.add(idx);
            Assertions.assertEquals(idx - 2, cache.get(0));
        }
    }

    @Test
    void forEachConcurrent()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        cache.addAll(List.of(0, 1, 2, 3, 4));

        Assertions.assertThrows(ConcurrentModificationException.class, () -> cache.forEach(cache::remove));
    }

    @Test
    void forEach()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        cache.addAll(List.of(0, 1, 2));
        cache.add(3);
        cache.add(4);

        final List<Integer> lst = new ArrayList<>(3);
        //noinspection UseBulkOperation
        cache.forEach(lst::add);
        Assertions.assertEquals(lst, List.of(2, 3, 4));
    }

    @Test
    void replace()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        cache.addAll(List.of(0, 1, 2));
        testEquals(cache, 0, 1, 2);

        cache.replace(0, 5);
        testEquals(cache, 5, 1, 2);

        cache.replace(1, 7);
        testEquals(cache, 5, 7, 2);

        cache.replace(2, 9);
        testEquals(cache, 5, 7, 9);
    }

    @Test
    void iteratorConcurrentMod()
    {
        final RollingCache<Integer> cache = new RollingCache<>(3);
        cache.addAll(List.of(0, 1, 2));
        cache.add(3);

        final Iterator<Integer> it = cache.iterator();
        Assertions.assertThrows(IllegalStateException.class, it::remove);

        Assertions.assertEquals(1, it.next());
        cache.removeFirst();
        Assertions.assertThrows(ConcurrentModificationException.class, it::remove);
        Assertions.assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void iterator()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        cache.addAll(List.of(0, 1, 2, 3, 4));
        Assertions.assertIterableEquals(List.of(0, 1, 2, 3, 4), cache);

        final Iterator<Integer> it = cache.iterator();
        for (int idx = 0; idx < 5; ++idx)
        {
            Assertions.assertTrue(it.hasNext());
            Assertions.assertEquals(idx, it.next());
        }
        Assertions.assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void spliterator()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        cache.addAll(List.of(-2, -1, 0, 1, 2));
        // Roll over
        cache.add(3);
        cache.add(4);
        // Now: 0, 1, 2, 3, 4

        final RollingCache<Integer>.RollingSpliterator spliterator = cache.spliterator();
        Assertions.assertEquals(spliterator.characteristics(),
                                Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED |
                                    Spliterator.SUBSIZED | Spliterator.IMMUTABLE);
        Assertions.assertEquals(5, spliterator.estimateSize());

        final AtomicReference<Integer> ref = new AtomicReference<>();
        spliterator.tryAdvance(ref::set);
        Assertions.assertEquals(0, ref.get());

        final List<Integer> lst = new ArrayList<>(3);
        spliterator.forEachRemaining(lst::add);
        Assertions.assertEquals(List.of(1, 2, 3, 4), lst);

        // There shouldn't be any items remaining, so calling
        // it again shouldn't do anything.
        spliterator.forEachRemaining(lst::add);
        Assertions.assertEquals(List.of(1, 2, 3, 4), lst);

        // Nothing left to do.
        Assertions.assertFalse(spliterator.tryAdvance(NO_OP_CONSUMER));
        Assertions.assertNull(spliterator.peek());
    }

    @Test
    void spliteratorConcurrentMod()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        cache.addAll(List.of(0, 1, 2, 3, 4));
        final RollingCache<Integer>.RollingSpliterator spliterator = cache.spliterator();
        cache.removeFirst();
        Assertions.assertThrows(ConcurrentModificationException.class, spliterator::trySplit);
        Assertions.assertThrows(ConcurrentModificationException.class, () -> spliterator.tryAdvance(NO_OP_CONSUMER));
        Assertions.assertThrows(ConcurrentModificationException.class,
                                () -> spliterator.forEachRemaining(NO_OP_CONSUMER));
    }

    @Test
    void spliteratorSplit()
    {
        final RollingCache<Integer> cache = new RollingCache<>(10);
        cache.addAll(List.of(-3, -2, -1, 0, 1, 2, 3, 4, 5, 6));
        // Roll over
        cache.add(7);
        cache.add(8);
        cache.add(9);
        // Now: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9

        final var spliterator0 = cache.spliterator();
        final var spliterator1 = Objects.requireNonNull(spliterator0.trySplit());
        final var spliterator2 = Objects.requireNonNull(spliterator1.trySplit());
        final var spliterator3 = Objects.requireNonNull(spliterator2.trySplit());

        Assertions.assertEquals(5, spliterator0.estimateSize());
        Assertions.assertEquals(3, spliterator1.estimateSize());
        Assertions.assertEquals(1, spliterator2.estimateSize());
        Assertions.assertEquals(1, spliterator3.estimateSize());
        Assertions.assertNull(spliterator3.trySplit());

        Assertions.assertEquals(5, spliterator0.peek());
        Assertions.assertEquals(2, spliterator1.peek());
        Assertions.assertEquals(1, spliterator2.peek());
        Assertions.assertEquals(0, spliterator3.peek());

        spliterator0.tryAdvance(NO_OP_CONSUMER);
        spliterator0.tryAdvance(NO_OP_CONSUMER);
        Assertions.assertEquals(3, spliterator0.estimateSize());

        final var spliterator4 = Objects.requireNonNull(spliterator0.trySplit());
        Assertions.assertEquals(1, spliterator4.estimateSize());
        Assertions.assertEquals(2, spliterator0.estimateSize());
        Assertions.assertEquals(8, spliterator0.peek());
        Assertions.assertEquals(7, spliterator4.peek());
    }

    @Test
    void testStream()
    {
        final RollingCache<Integer> cache = new RollingCache<>(5);
        cache.addAll(List.of(0, 1, 2, 3, 4));

        // Simple stream usage to make sure streaming works as intended.
        final List<Integer> even = cache.stream().filter(val -> val % 2 == 0).toList();
        Assertions.assertEquals(List.of(0, 2, 4), even);
    }

    @SafeVarargs
    private <T> void testEquals(RollingCache<? extends T> cache, T... expected)
    {
        testEquals(expected, cache.toArray());
    }

    private void testEquals(Object[] expected, Object[] provided)
    {
        // Use Strings so that we can see all the values when they aren't equal,
        // rather than just a message saying the size doesn't match or a single value doesn't.
        Assertions.assertEquals(Arrays.toString(expected), Arrays.toString(provided));
    }
}
