package nl.pim16aap2.util.collections;

import nl.pim16aap2.util.LazyInit;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Extends {@link CopyOnWriteArrayList} to allow for snapshots of the list.
 * <p>
 * The difference between this class and {@link CopyOnWriteArrayList} is that this class has the
 * <p>
 * This class delegates all methods to the super class, but any method that modifies the list will also reset the
 * snapshot. This means that the snapshot will always be up-to-date with the list.
 *
 * @param <E>
 *     The type of elements in this list.
 */
public class SnapshotCopyOnWriteArrayList<E> extends CopyOnWriteArrayList<E>
{
    private final LazyInit.Resettable<List<E>> snapshot =
        new LazyInit.Resettable<>(() -> List.copyOf(this));

    /**
     * Gets a snapshot of the list.
     * <p>
     * The snapshot is a lazily initialized unmodifiable list that is a snapshot of the list at the time of the call.
     * <p>
     * Modifications to the list will not be reflected in a snapshot that has already been obtained, but will cause the
     * snapshot to be reset and require subsequent calls to {@link #getSnapshot()} to get a new snapshot.
     *
     * @return A snapshot of the list.
     */
    public List<E> getSnapshot()
    {
        return snapshot.get();
    }

    @Override
    public boolean add(E e)
    {
        return modifyAndReset(() -> super.add(e));
    }

    @Override
    public void add(int index, E element)
    {
        modifyAndReset(() -> super.add(index, element));
    }

    @Override
    public void addFirst(E e)
    {
        modifyAndReset(() -> super.addFirst(e));
    }

    @Override
    public void addLast(E e)
    {
        modifyAndReset(() -> super.addLast(e));
    }

    @Override
    public E set(int index, E element)
    {
        return modifyAndReset(() -> super.set(index, element));
    }

    @Override
    public E remove(int index)
    {
        return modifyAndReset(() -> super.remove(index));
    }

    @Override
    public E removeFirst()
    {
        return modifyAndReset(super::removeFirst);
    }

    @Override
    public E removeLast()
    {
        return modifyAndReset(super::removeLast);
    }

    @Override
    public boolean remove(Object o)
    {
        return modifyAndReset(() -> super.remove(o));
    }

    @Override
    public boolean addIfAbsent(E e)
    {
        return modifyAndReset(() -> super.addIfAbsent(e));
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return modifyAndReset(() -> super.retainAll(c));
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return modifyAndReset(() -> super.removeAll(c));
    }

    @Override
    public int addAllAbsent(Collection<? extends E> c)
    {
        return modifyAndReset(() -> super.addAllAbsent(c));
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return modifyAndReset(() -> super.addAll(c));
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        return modifyAndReset(() -> super.addAll(index, c));
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter)
    {
        return modifyAndReset(() -> super.removeIf(filter));
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator)
    {
        modifyAndReset(() -> super.replaceAll(operator));
    }

    @Override
    public void sort(Comparator<? super E> c)
    {
        modifyAndReset(() -> super.sort(c));
    }

    @Override
    public void clear()
    {
        modifyAndReset(super::clear);
    }

    private boolean modifyAndReset(BooleanSupplier modification)
    {
        synchronized (snapshot)
        {
            boolean result = modification.getAsBoolean();
            snapshot.reset();
            return result;
        }
    }

    private <T> T modifyAndReset(Supplier<T> modification)
    {
        synchronized (snapshot)
        {
            T result = modification.get();
            snapshot.reset();
            return result;
        }
    }

    private void modifyAndReset(Runnable modification)
    {
        synchronized (snapshot)
        {
            modification.run();
            snapshot.reset();
        }
    }
}
