package nl.pim16aap2.animatedarchitecture.core.data.cache;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Represents a bounded deque with a fixed maximum capacity.
 * <p>
 * When the capacity is exceeded, elements are evicted to make space for the new element.
 * <p>
 * When adding an element to the head, the oldest element (i.e. the element at the tail) is evicted. When adding an
 * element to the tail, the newest element (i.e. the element at the head) is evicted.
 * <p>
 * The head contains the element that has been in the cache the shortest and the tail contains the element that has been
 * in the cache the longest.
 * <p>
 * Note that access to this class is not thread-safe and that as such, external synchronization is required if used in a
 * multithreaded environment.
 *
 * @param <T>
 *     The type of data to store.
 */
public final class RollingCache<T> extends AbstractCollection<T> implements Iterable<T>
{
    @Getter(AccessLevel.PACKAGE)
    private final int limit;
    private final T[] arr;
    private int size = 0;
    @Getter(AccessLevel.PACKAGE)
    private int ptrHead = 0;
    @Getter(AccessLevel.PACKAGE)
    private int ptrTail = 0;
    @Getter(AccessLevel.PACKAGE)
    private int modCount = 0;

    public RollingCache(int limit)
    {
        if (limit <= 0)
            throw new IllegalArgumentException("Limit of rolling cache must be greater than 0!");
        this.limit = limit;
        //noinspection unchecked
        arr = (T[]) new Object[limit];
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public RollingIterator iterator()
    {
        return new RollingIterator();
    }

    /**
     * See {@link #addFirst(Object)}.
     */
    @Override
    public boolean add(T t)
    {
        addFirst(t);
        return true;
    }

    @Override
    public void forEach(Consumer<? super T> action)
    {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int idx = 0; modCount == expectedModCount && idx < size; ++idx)
            action.accept(arr[loopedIndex(ptrTail + idx)]);
        checkModCount(expectedModCount);
    }

    /**
     * Removes an object at the given index.
     * <p>
     * The index is counted from the tail to the head (i.e. from the oldest to the newest value).
     *
     * @param index
     *     The index of the object to remove.
     * @return The value that was removed.
     *
     * @throws IndexOutOfBoundsException
     *     If the given index is negative or exceeds the current size of the cache.
     */
    public T remove(int index)
    {
        verifyIndex(index);
        if (index == 0)
            return removeFirst();
        if (index == (size - 1))
            return removeLast();

        final boolean onSecondHalf = onSecondHalf(index);

        --size;
        ++modCount;
        final int realIndex = loopedIndex(ptrTail + index);
        return onSecondHalf ? shiftFromHead(realIndex) : shiftFromTail(realIndex);
    }

    /**
     * Checks if a specific index is on the first half or on the last half of the current data.
     *
     * @param index
     *     The index to check.
     * @return True if the index lies on the second half of the data stored in this cache.
     */
    boolean onSecondHalf(int index)
    {
        return index >= (size / 2);
    }

    T shiftFromHead(int targetIdx)
    {
        ptrHead = loopedIndex(ptrHead - 1);
        T previousValue = null;
        for (int idx = ptrHead; idx != targetIdx; idx = loopedIndex(idx - 1))
            previousValue = replace(idx, previousValue);
        return replace(targetIdx, previousValue);
    }

    T shiftFromTail(int targetIdx)
    {
        T previousValue = null;
        for (int idx = ptrTail; idx != targetIdx; idx = loopedIndex(idx + 1))
            previousValue = replace(idx, previousValue);
        ptrTail = loopedIndex(ptrTail + 1);
        return replace(targetIdx, previousValue);
    }

    T replace(int realIdx, @Nullable T value)
    {
        final T tmp = arr[realIdx];
        //noinspection ConstantConditions
        arr[realIdx] = value;
        return tmp;
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        if (c.isEmpty())
            return false;

        Collection<? extends T> add = cropCollection(limit, c);
        if (c == this)
            add = new ArrayList<>(add);

        for (final var val : add)
            add(val);

        return true;
    }

    static <T> Collection<? extends T> cropCollection(int limit, Collection<? extends T> c)
    {
        if (c.size() < limit)
            return c;
        return c.stream().skip(Math.max(0, c.size() - limit)).toList();
    }

    @Override
    public RollingSpliterator spliterator()
    {
        return new RollingSpliterator(modCount, 0, size);
    }

    /**
     * Adds an element to this cache. The element is inserted at the head of the cache.
     * <p>
     * If the cache is currently at capacity, the oldest element (i.e. the element at the tail) will be evicted.
     *
     * @param t
     *     The element to add.
     */
    public void addFirst(T t)
    {
        Objects.requireNonNull(t);
        arr[ptrHead] = t;
        ptrHead = loopedIndex(ptrHead + 1);
        // When the cache is full, move the tail forward,
        // as it has been overwritten by the head now.
        if (size == limit)
            ptrTail = loopedIndex(ptrTail + 1);
        else
            ++size;
        ++modCount;
    }

    /**
     * Adds an element to this cache. The element is inserted at the tail of the cache.
     * <p>
     * If the cache is currently at capacity, the newest element (i.e. the element at the head) will be evicted.
     *
     * @param t
     *     The element to add.
     */
    public void addLast(T t)
    {
        Objects.requireNonNull(t);
        ptrTail = loopedIndex(ptrTail - 1);
        arr[ptrTail] = t;
        // When the cache is full, move the head back,
        // as it has been overwritten by the tail now.
        if (size == limit)
            ptrHead = loopedIndex(ptrHead - 1);
        else
            ++size;
        ++modCount;
    }

    /**
     * Retrieves, but does not remove, the element at the head of this cache. This is the element that has been in the
     * cache the shortest.
     *
     * @return The last value. This is the value that has been in the cache the shortest.
     *
     * @throws NoSuchElementException
     *     If this cache is empty.
     */
    public T getLast()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty cache!");
        return arr[loopedIndex(ptrHead - 1)];
    }

    /**
     * Retrieves, but does not remove, the element at the tail of this cache. This is the element that has been in the
     * cache the longest.
     *
     * @return The first value. This is the value that has been in the cache the longest.
     *
     * @throws NoSuchElementException
     *     If this cache is empty.
     */
    public T getFirst()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty cache!");
        return arr[ptrTail];
    }

    /**
     * Removes the element at the head of this cache. This is the element that has been in this cache the shortest.
     *
     * @return The removed element.
     *
     * @throws NoSuchElementException
     *     If this cache is empty.
     */
    public T removeLast()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty cache!");

        ptrHead = loopedIndex(ptrHead - 1);
        final T ret = arr[ptrHead];
        setNull(ptrHead);
        --size;
        ++modCount;
        return ret;
    }

    /**
     * Removes the element at the tail of this cache. This is the element that has been in this cache the longest.
     *
     * @return The removed element.
     *
     * @throws NoSuchElementException
     *     If this cache is empty.
     */
    public T removeFirst()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty cache!");

        final T ret = arr[ptrTail];
        setNull(ptrTail);
        ptrTail = loopedIndex(ptrTail + 1);
        --size;
        ++modCount;
        return ret;
    }

    /**
     * Gets the element at the given index if it exists.
     *
     * @param index
     *     The position of the index as counted from the oldest value.
     * @return The value at the given position.
     *
     * @throws IndexOutOfBoundsException
     *     If the given index is negative or exceeds the current size of the cache.
     */
    public T get(int index)
    {
        verifyIndex(index);
        return arr[loopedIndex(ptrTail + index)];
    }

    private void verifyIndex(int index)
    {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size + "!");
    }

    /**
     * Removes all entries from this cache.
     */
    @Override
    public void clear()
    {
        for (int idx = 0; idx != size; ++idx)
            setNull(loopedIndex(ptrTail + idx));
        ptrHead = 0;
        ptrTail = 0;
        size = 0;
        ++modCount;
    }

    @SuppressWarnings({"AssignmentToNull", "ConstantConditions"})
    private void setNull(int idx)
    {
        arr[idx] = null;
    }

    /**
     * Returns a copy of the raw underlying array.
     *
     * @return A copy of the raw underlying array.
     */
    @VisibleForTesting
    T[] rawArray()
    {
        return Arrays.copyOf(arr, limit);
    }

    int loopedIndex(int index)
    {
        return Math.floorMod(index, limit);
    }

    void checkModCount(int expectedModCount)
    {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }

    /**
     * An iterator for the rolling cache.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public final class RollingIterator implements Iterator<T>
    {
        private int expectedModCount = modCount;
        private int index = -1;
        private int remaining = size;

        @Override
        public boolean hasNext()
        {
            return remaining > 0;
        }

        @Override
        public T next()
        {
            checkModCount(expectedModCount);
            checkRemaining();

            ++index;
            --remaining;
            return arr[loopedIndex(ptrTail + index)];
        }

        @Override
        public void remove()
        {
            checkModCount(expectedModCount);
            if (index < 0)
                throw new IllegalStateException();

            RollingCache.this.remove(index);
            expectedModCount = modCount;
            --index;
        }

        private void checkRemaining()
        {
            if (remaining <= 0)
                throw new NoSuchElementException();
        }
    }

    /**
     * A spliterator for the rolling cache.
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public final class RollingSpliterator implements Spliterator<T>
    {
        private int expectedModCount;
        private int index;
        private int remaining;

        @Override
        public boolean tryAdvance(Consumer<? super T> action)
        {
            Objects.requireNonNull(action);
            if (remaining <= 0)
                return false;
            checkModCount(expectedModCount);

            final T val = arr[loopedIndex(ptrTail + index)];
            ++index;
            --remaining;

            action.accept(val);
            checkModCount(expectedModCount);
            return true;
        }

        /**
         * Peeks at the next element that would be returned by {@link #tryAdvance(Consumer)} without advancing the
         * iterator.
         *
         * @return The next element, or null if there are no more elements.
         */
        @VisibleForTesting
        @Nullable T peek()
        {
            if (remaining <= 0)
                return null;
            checkModCount(expectedModCount);
            return arr[loopedIndex(ptrTail + index)];
        }

        @Override
        public RollingCache<T>.@Nullable RollingSpliterator trySplit()
        {
            final int halfRemaining = remaining / 2;
            if (halfRemaining == 0)
                return null;
            checkModCount(expectedModCount);

            final int oldCursor = index;

            index = index + halfRemaining;
            remaining = remaining - halfRemaining;

            return new RollingSpliterator(expectedModCount, oldCursor, halfRemaining);
        }

        @Override
        public long estimateSize()
        {
            return remaining;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action)
        {
            Objects.requireNonNull(action);
            if (remaining <= 0)
                return;
            checkModCount(expectedModCount);

            final int lastIdx = index + remaining;
            for (; index < lastIdx; ++index)
            {
                final T val = arr[loopedIndex(ptrTail + index)];
                action.accept(val);
                --remaining;
            }
            checkModCount(expectedModCount);
        }

        @Override
        public int characteristics()
        {
            return Spliterator.NONNULL | Spliterator.ORDERED |
                Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }
}
