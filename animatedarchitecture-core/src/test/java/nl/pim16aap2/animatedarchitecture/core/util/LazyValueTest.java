package nl.pim16aap2.animatedarchitecture.core.util;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

class LazyValueTest
{
    @Test
    void testGet()
    {
        final Supplier<Integer> supplier = getIntegerSupplier(42);
        final LazyValue<Integer> lazyValue = new LazyValue<>(supplier);
        Mockito.verify(supplier, Mockito.never()).get();

        Assertions.assertEquals(42, lazyValue.get());
        Mockito.verify(supplier, Mockito.atMostOnce()).get();

        Mockito.when(supplier.get()).thenReturn(16);
        Assertions.assertEquals(42, lazyValue.get());

        Mockito.verify(supplier, Mockito.atMostOnce()).get();
    }

    @Test
    void testInvalidation()
    {
        final Supplier<Integer> supplier = getIntegerSupplier(42);
        final LazyValue<Integer> lazyValue = new LazyValue<>(supplier);

        Assertions.assertNull(lazyValue.invalidate());
        Mockito.verify(supplier, Mockito.never()).get();

        Assertions.assertEquals(42, lazyValue.get());
        Assertions.assertEquals(42, lazyValue.invalidate());
        Mockito.verify(supplier, Mockito.times(1)).get();

        Mockito.when(supplier.get()).thenReturn(16);
        Assertions.assertEquals(16, lazyValue.get());
        Mockito.verify(supplier, Mockito.times(2)).get();
    }

    @Test
    void testInvalidSupplier()
    {
        final Supplier<Integer> supplier = getIntegerSupplier(null);
        final LazyValue<Integer> lazyValue = new LazyValue<>(supplier);

        Assertions.assertThrows(NullPointerException.class, lazyValue::get);
    }

    private static Supplier<Integer> getIntegerSupplier(@Nullable Integer value)
    {
        @SuppressWarnings("unchecked")//
        final Supplier<Integer> supplier = (Supplier<Integer>) Mockito.mock(Supplier.class);
        if (value != null)
            Mockito.when(supplier.get()).thenReturn(value);
        return supplier;
    }
}
