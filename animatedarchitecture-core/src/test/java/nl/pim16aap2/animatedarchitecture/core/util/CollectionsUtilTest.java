package nl.pim16aap2.animatedarchitecture.core.util;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CollectionsUtilTest
{
    @Test
    void testSearchIterable()
    {
        final var list = Arrays.asList("item1", "item2", "item3", "item4");
        final var exists = CollectionsUtil.searchIterable(list, "item2"::equals);
        UnitTestUtil.optionalEquals("item2", exists);

        final var missing = CollectionsUtil.searchIterable(list, "item5"::equals);
        assertFalse(missing.isPresent());
    }

    @Test
    void testConcatArray()
    {
        final Integer[] first = {1, 2, 3};
        final Integer[] second = {4, 5, 6};
        final Integer[] expected = {1, 2, 3, 4, 5, 6};
        final Integer[] result = CollectionsUtil.concat(first, second);
        assertArrayEquals(expected, result);
    }

    @Test
    void testConcatLists()
    {
        final var first = Arrays.asList(1, 2, 3);
        final var second = Arrays.asList(4, 5, 6);
        final var expected = Arrays.asList(1, 2, 3, 4, 5, 6);
        final var result = CollectionsUtil.concat(first, second);
        Assertions.assertIterableEquals(expected, result);
    }
}
