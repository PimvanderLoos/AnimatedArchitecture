package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.flogger.Flogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class for collections.
 */
@UtilityClass
@Flogger
public final class CollectionsUtil
{
    /**
     * Searches through an {@link Iterable} object using a provided search function.
     *
     * @param iterable
     *     The {@link Iterable} object to search through.
     * @param searchPredicate
     *     The search predicate to use.
     * @param <T>
     *     The type of objects stored in the {@link Iterable}.
     * @return The value in the {@link Iterable} object for which the search function returns true, otherwise
     * {@link Optional#empty()}.
     */
    public <T> Optional<T> searchIterable(Iterable<T> iterable, Predicate<T> searchPredicate)
    {
        for (final T val : iterable)
            if (searchPredicate.test(val))
                return Optional.of(val);
        return Optional.empty();
    }

    /**
     * Concatenate two arrays into a single array.
     *
     * @param first
     *     First array.
     * @param second
     *     Second array.
     * @param <T>
     *     The type of the arrays.
     * @return A single concatenated array.
     */
    @SuppressWarnings("PMD.UseVarargs")
    public static <T> T[] concat(T[] first, T[] second)
    {
        final T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Concatenates two lists into a single list.
     *
     * @param listA
     *     The first list.
     * @param listB
     *     The second list.
     * @param <T>
     *     The type of data in the lists.
     * @return A single list containing all elements from both input lists.
     */
    public static <T> List<T> concat(List<T> listA, List<T> listB)
    {
        final List<T> ret = new ArrayList<>(listA.size() + listB.size());
        ret.addAll(listA);
        ret.addAll(listB);
        return ret;
    }
}
