package nl.pim16aap2.animatedarchitecture.core.data.cache;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Represents a LIFO (Last-In-First-Out) stack of objects with a limited capacity.
 * <p>
 * When the capacity is exceeded, the last element is evicted to make space for the new element.
 * <p>
 * The head contains the element that has been in the stack the shortest and the tail contains the element that has been
 * in the stack the longest.
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
            throw new IllegalArgumentException("Limit of rolling stack must be greater than 0!");
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
     * See {@link #insertFirst(Object)}.
     */
    @Override
    public boolean add(T t)
    {
        insertFirst(t);
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
     *     If the given index is negative or exceeds the current size of the stack.
     */
    public T remove(int index)
    {
        verifyIndex(index);
        if (index == 0)
            return removeFirst();
        if (index == (size - 1))
            return removeLast();

        --size;
        ++modCount;
        final int realIndex = loopedIndex(ptrTail + index);
        return onSecondHalf(index) ? shiftFromHead(realIndex) : shiftFromTail(realIndex);
    }

    /**
     * Checks if a specific index is on the first half or on the last half of the current data.
     *
     * @param index
     *     The index to check.
     * @return True if the index lies on the second half of the data stored in this stack.
     */
    boolean onSecondHalf(int index)
    {
        return index >= (size / 2);
    }

    T shiftFromHead(int targetIdx)
    {
        ptrHead = loopedIndex(ptrHead - 1);
        @Nullable T previousValue = null;
        for (int idx = ptrHead; idx != targetIdx; idx = loopedIndex(idx - 1))
            previousValue = replace(idx, previousValue);
        ++modCount;
        return replace(targetIdx, previousValue);
    }

    T shiftFromTail(int targetIdx)
    {
        @Nullable T previousValue = null;
        for (int idx = ptrTail; idx != targetIdx; idx = loopedIndex(idx + 1))
            previousValue = replace(idx, previousValue);
        ptrTail = loopedIndex(ptrTail + 1);
        ++modCount;
        return replace(targetIdx, previousValue);
    }

    T replace(int realIdx, @Nullable T value)
    {
        final T tmp = arr[realIdx];
        //noinspection ConstantConditions
        arr[realIdx] = value;
        ++modCount;
        return tmp;
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        if (c.isEmpty())
            return false;

        final Collection<? extends T> add = cropCollection(limit, c);
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
     * Adds an element to this stack. The element is inserted at the head of the stack.
     * <p>
     * If the stack is currently at capacity, the oldest element (i.e. the element at the tail) will be evicted.
     *
     * @param t
     *     The element to add.
     */
    public void insertFirst(T t)
    {
        Objects.requireNonNull(t);
        arr[ptrHead] = t;
        ptrHead = loopedIndex(ptrHead + 1);
        // When the stack is full, move the tail forward,
        // as it has been overwritten by the head now.
        if (size == limit)
            ptrTail = loopedIndex(ptrTail + 1);
        else
            ++size;
        ++modCount;
    }

    /**
     * Adds an element to this stack. The element is inserted at the tail of the stack.
     * <p>
     * If the stack is currently at capacity, the newest element (i.e. the element at the head) will be evicted.
     *
     * @param t
     *     The element to add.
     */
    public void insertLast(T t)
    {
        Objects.requireNonNull(t);
        ptrTail = loopedIndex(ptrTail - 1);
        arr[ptrTail] = t;
        // When the stack is full, move the head back,
        // as it has been overwritten by the tail now.
        if (size == limit)
            ptrHead = loopedIndex(ptrHead - 1);
        else
            ++size;
        ++modCount;
    }

    /**
     * @return The last value. This is the value that has been in the stack the shortest.
     *
     * @throws NoSuchElementException
     *     If this stack is empty.
     */
    public T peekLast()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty stack!");
        return arr[loopedIndex(ptrHead - 1)];
    }

    /**
     * @return The first value. This is the value that has been in the stack the longest.
     *
     * @throws NoSuchElementException
     *     If this stack is empty.
     */
    public T peekFirst()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty stack!");
        return arr[ptrTail];
    }

    /**
     * Removes the element at the head of this stack. This is the element that has been in this stack the shortest.
     *
     * @return The removed element.
     *
     * @throws NoSuchElementException
     *     If this stack is empty.
     */
    public T removeLast()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty stack!");

        ptrHead = loopedIndex(ptrHead - 1);
        final T ret = arr[ptrHead];
        setNull(ptrHead);
        --size;
        ++modCount;
        return ret;
    }

    /**
     * Removes the element at the tail of this stack. This is the element that has been in this stack the longest.
     *
     * @return The removed element.
     *
     * @throws NoSuchElementException
     *     If this stack is empty.
     */
    public T removeFirst()
    {
        if (size == 0)
            throw new NoSuchElementException("Cannot retrieve items from empty stack!");

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
     *     If the given index is negative or exceeds the current size of the stack.
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
     * Removes all entries from this stack.
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
     * @return A copy of the raw underlying array.
     */
    // For testing.
    T[] rawArray()
    {
        return Arrays.copyOf(arr, limit);
    }

    int loopedIndex(int index)
    {
        if (index < 0)
            return index + limit;
        return index % limit;
    }

    void checkModCount(int expectedModCount)
    {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }

    private final class RollingIterator implements Iterator<T>
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
            checkRemaining();
            if (index < 0)
                throw new IllegalStateException();

            RollingCache.this.remove(index);
            expectedModCount = modCount;
            --remaining;
        }

        private void checkRemaining()
        {
            if (remaining <= 0)
                throw new NoSuchElementException();
        }
    }

    @AllArgsConstructor final class RollingSpliterator implements Spliterator<T>
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

        @Nullable T peek()
        {
            if (remaining <= 0)
                return null;
            checkModCount(expectedModCount);
            return arr[loopedIndex(ptrTail + index)];
        }

        @Override
        public @Nullable RollingCache<T>.RollingSpliterator trySplit()
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
                Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE;
        }
    }
}
