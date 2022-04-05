package nl.pim16aap2.bigdoors.util.data;

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

class RollingStackTest
{
    private static final Consumer<Integer> NO_OP_CONSUMER = (ignored) ->
    {
    };

    @Test
    void ensureLimit()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RollingStack<>(-12));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RollingStack<>(0));
        Assertions.assertEquals(1, new RollingStack<>(1).getLimit());
        Assertions.assertEquals(17, new RollingStack<>(17).getLimit());
    }

    @Test
    void cropCollection()
    {
        final List<Integer> lst = List.of(0, 1, 2, 3, 4, 5);
        Assertions.assertEquals(List.of(3, 4, 5), RollingStack.cropCollection(3, lst));
        Assertions.assertEquals(lst, RollingStack.cropCollection(6, lst));
        Assertions.assertEquals(lst, RollingStack.cropCollection(20, lst));
        Assertions.assertSame(lst, RollingStack.cropCollection(20, lst));
        Assertions.assertEquals(List.of(5), RollingStack.cropCollection(1, lst));
    }

    @Test
    void addAll()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);

        Assertions.assertFalse(stack.addAll(Collections.emptyList()));
        Assertions.assertEquals(0, stack.size());

        Assertions.assertTrue(stack.addAll(List.of(1)));
        Assertions.assertEquals(1, stack.size());
        testEquals(stack, 1);

        Assertions.assertTrue(stack.addAll(List.of(2)));
        Assertions.assertEquals(2, stack.size());
        testEquals(stack, 1, 2);

        Assertions.assertTrue(stack.addAll(List.of(10, 11, 12, 13)));
        Assertions.assertEquals(3, stack.size());
        testEquals(stack, 11, 12, 13);

        Assertions.assertTrue(stack.getModCount() > 0);
    }

    @Test
    void testRollover()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        for (int idx = 0; idx < 100; ++idx)
            stack.add(idx);
        Assertions.assertEquals(3, stack.size());
        testEquals(stack, 97, 98, 99);

        final int mc = stack.getModCount();
        Assertions.assertTrue(mc > 0);

        for (int idx = 0; idx < 50; ++idx)
            stack.insertLast(idx);
        Assertions.assertEquals(3, stack.size());
        testEquals(stack, 49, 48, 47);

        Assertions.assertTrue(stack.getModCount() > mc);
    }

    @Test
    void doubleSidedInserts()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        stack.insertLast(1);
        stack.insertLast(0);
        stack.insertFirst(2);
        testEquals(stack, 0, 1, 2);

        stack.insertLast(3);
        testEquals(stack, 3, 0, 1);

        stack.insertFirst(2);
        testEquals(stack, 0, 1, 2);
    }

    @Test
    void onSecondHalf()
    {
        final RollingStack<Integer> stack = new RollingStack<>(5);
        stack.addAll(List.of(0, 1, 2, 3, 4, 5));
        Assertions.assertTrue(stack.onSecondHalf(3));
        Assertions.assertFalse(stack.onSecondHalf(0));
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
        final RollingStack<Integer> stack = new RollingStack<>(7);
        stack.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        stack.remove(5);
        testEquals(new Integer[]{0, 1, 2, 3, 4, 6, null}, stack.rawArray());
        stack.remove(1);
        testEquals(new Integer[]{null, 0, 2, 3, 4, 6, null}, stack.rawArray());
        testEquals(stack, 0, 2, 3, 4, 6);

        stack.add(7);
        stack.add(8);
        stack.add(9);
        // Indices:              5  6  0  1  2  3  4
        testEquals(new Integer[]{8, 9, 2, 3, 4, 6, 7}, stack.rawArray());

        stack.remove(4);
        // Indices:              5        0  1  2  3  4
        testEquals(new Integer[]{9, null, 2, 3, 4, 6, 8}, stack.rawArray());

        stack.remove(1);
        testEquals(new Integer[]{9, null, null, 2, 4, 6, 8}, stack.rawArray());
        testEquals(stack, 2, 4, 6, 8, 9);
    }

    @Test
    void removeIndex()
    {
        final RollingStack<Integer> stack = new RollingStack<>(7);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.remove(0));
        stack.addAll(List.of(0, 1, 2, 3, 4, 5, 6));

        testEquals(stack, 0, 1, 2, 3, 4, 5, 6);

        stack.remove(0);
        testEquals(stack, 1, 2, 3, 4, 5, 6);

        stack.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        stack.remove(6);
        testEquals(stack, 0, 1, 2, 3, 4, 5);
        stack.remove(4);
        testEquals(stack, 0, 1, 2, 3, 5);

        stack.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        stack.remove(1);
        testEquals(stack, 0, 2, 3, 4, 5, 6);

        stack.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        stack.remove(5);
        testEquals(stack, 0, 1, 2, 3, 4, 6);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.remove(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.remove(6));
        stack.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.remove(7));
        stack.clear();
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.remove(0));
    }

    @Test
    void removeObject()
    {
        final RollingStack<Integer> stack = new RollingStack<>(5);
        stack.addAll(List.of(10, 11));

        Assertions.assertFalse(stack.remove((Integer) 3));
        Assertions.assertTrue(stack.remove((Integer) 10));
        Assertions.assertEquals(stack.size(), 1);

        stack.addAll(List.of(10, 11, 12, 13, 14));
        Assertions.assertTrue(stack.remove((Integer) 11));
        Assertions.assertEquals(stack.size(), 4);

        Assertions.assertTrue(stack.remove((Integer) 13));
        Assertions.assertEquals(stack.size(), 3);

        testEquals(stack, 10, 12, 14);
    }

    @Test
    void simpleRemove()
    {
        final RollingStack<Integer> stack = new RollingStack<>(5);
        Assertions.assertThrows(NoSuchElementException.class, stack::removeFirst);
        Assertions.assertThrows(NoSuchElementException.class, stack::removeLast);

        // Add a bunch to ensure we loop over a few times.
        for (int idx = 0; idx < 37; ++idx)
            stack.add(idx);
        Assertions.assertEquals(5, stack.size());

        int mc = stack.getModCount();
        Assertions.assertEquals(32, stack.removeFirst());
        Assertions.assertEquals(33, stack.removeFirst());
        Assertions.assertTrue(stack.getModCount() > mc);
        Assertions.assertEquals(3, stack.size());

        mc = stack.getModCount();
        Assertions.assertEquals(36, stack.removeLast());
        Assertions.assertEquals(35, stack.removeLast());
        Assertions.assertTrue(stack.getModCount() > mc);
        Assertions.assertEquals(1, stack.size());

        testEquals(stack, 34);

        // Make sure both removeLast and removeFirst work when there's only 1 item remaining.
        Assertions.assertEquals(34, stack.removeLast());
        Assertions.assertEquals(0, stack.size());
        stack.add(34);
        Assertions.assertEquals(34, stack.removeFirst());
        Assertions.assertEquals(0, stack.size());

        //noinspection ConstantConditions
        testEquals(new Object[]{null, null, null, null, null}, stack.rawArray());

        Assertions.assertThrows(NoSuchElementException.class, stack::removeFirst);
        Assertions.assertThrows(NoSuchElementException.class, stack::removeLast);
    }

    @Test
    void clear()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        stack.addAll(List.of(0, 1, 2));
        stack.add(3);

        Assertions.assertTrue(stack.getPtrHead() != 0);
        Assertions.assertTrue(stack.getPtrTail() != 0);
        Assertions.assertTrue(stack.size() != 0);

        final int mc = stack.getModCount();
        stack.clear();

        Assertions.assertTrue(stack.getModCount() > mc);
        Assertions.assertEquals(0, stack.size());
        Assertions.assertEquals(0, stack.getPtrHead());
        Assertions.assertEquals(0, stack.getPtrTail());

        //noinspection ConstantConditions
        testEquals(new Object[]{null, null, null}, stack.rawArray());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void peek()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        Assertions.assertThrows(NoSuchElementException.class, stack::peekFirst);
        Assertions.assertThrows(NoSuchElementException.class, stack::peekLast);

        stack.addAll(List.of(0, 1, 2));
        Assertions.assertEquals(0, stack.peekFirst());
        Assertions.assertEquals(2, stack.peekLast());
    }

    @Test
    void get()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.get(0));
        stack.addAll(List.of(0, 1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> stack.get(4));

        for (int idx = 0; idx < 3; ++idx)
            Assertions.assertEquals(idx, stack.get(idx));

        // Make sure that all values pass idx 0 if you keep adding more values.
        for (int idx = 3; idx < 7; ++idx)
        {
            stack.add(idx);
            Assertions.assertEquals(idx - 2, stack.get(0));
        }
    }

    @Test
    void forEachConcurrent()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        stack.addAll(List.of(0, 1, 2, 3, 4));

        Assertions.assertThrows(ConcurrentModificationException.class, () -> stack.forEach(stack::remove));
    }

    @Test
    void forEach()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        stack.addAll(List.of(0, 1, 2));
        stack.add(3);
        stack.add(4);

        final List<Integer> lst = new ArrayList<>(3);
        //noinspection UseBulkOperation
        stack.forEach(lst::add);
        Assertions.assertEquals(lst, List.of(2, 3, 4));
    }

    @Test
    void replace()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        stack.addAll(List.of(0, 1, 2));
        testEquals(stack, 0, 1, 2);

        stack.replace(0, 5);
        testEquals(stack, 5, 1, 2);

        stack.replace(1, 7);
        testEquals(stack, 5, 7, 2);

        stack.replace(2, 9);
        testEquals(stack, 5, 7, 9);
    }

    @Test
    void iteratorConcurrentMod()
    {
        final RollingStack<Integer> stack = new RollingStack<>(3);
        stack.addAll(List.of(0, 1, 2));
        stack.add(3);

        final Iterator<Integer> it = stack.iterator();
        Assertions.assertThrows(IllegalStateException.class, it::remove);

        Assertions.assertEquals(1, it.next());
        stack.removeFirst();
        Assertions.assertThrows(ConcurrentModificationException.class, it::remove);
        Assertions.assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void iterator()
    {
        final RollingStack<Integer> stack = new RollingStack<>(5);
        stack.addAll(List.of(0, 1, 2, 3, 4));
        Assertions.assertIterableEquals(List.of(0, 1, 2, 3, 4), stack);

        final Iterator<Integer> it = stack.iterator();
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
        final RollingStack<Integer> stack = new RollingStack<>(5);
        stack.addAll(List.of(-2, -1, 0, 1, 2));
        // Roll over
        stack.add(3);
        stack.add(4);
        // Now: 0, 1, 2, 3, 4

        final RollingStack<Integer>.StackSpliterator spliterator = stack.spliterator();
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
        final RollingStack<Integer> stack = new RollingStack<>(5);
        stack.addAll(List.of(0, 1, 2, 3, 4));
        final RollingStack<Integer>.StackSpliterator spliterator = stack.spliterator();
        stack.removeFirst();
        Assertions.assertThrows(ConcurrentModificationException.class, spliterator::trySplit);
        Assertions.assertThrows(ConcurrentModificationException.class, () -> spliterator.tryAdvance(NO_OP_CONSUMER));
        Assertions.assertThrows(ConcurrentModificationException.class,
                                () -> spliterator.forEachRemaining(NO_OP_CONSUMER));
    }

    @Test
    void spliteratorSplit()
    {
        final RollingStack<Integer> stack = new RollingStack<>(10);
        stack.addAll(List.of(-3, -2, -1, 0, 1, 2, 3, 4, 5, 6));
        // Roll over
        stack.add(7);
        stack.add(8);
        stack.add(9);
        // Now: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9

        final var spliterator0 = stack.spliterator();
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
        final RollingStack<Integer> stack = new RollingStack<>(5);
        stack.addAll(List.of(0, 1, 2, 3, 4));

        // Simple stream usage to make sure streaming works as intended.
        final List<Integer> even = stack.stream().filter(val -> val % 2 == 0).toList();
        Assertions.assertEquals(List.of(0, 2, 4), even);
    }

    @SafeVarargs
    private <T> void testEquals(RollingStack<? extends T> stack, T... expected)
    {
        testEquals(expected, stack.toArray());
    }

    private void testEquals(Object[] expected, Object[] provided)
    {
        // Use Strings so that we can see all the values when they aren't equal,
        // rather than just a message saying the size doesn't match or a single value doesn't.
        Assertions.assertEquals(Arrays.toString(expected), Arrays.toString(provided));
    }
}
