package nl.pim16aap2.util;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

class LazyValueTest
{
    @Test
    void isInitialized_shouldReturnFalseWhenNotInitialized()
    {
        // Setup
        final Supplier<Integer> supplier = getIntegerSupplier(42);
        final LazyValue<Integer> lazyValue = new LazyValue<>(supplier);

        // Execute
        final boolean isInitialized = lazyValue.isInitialized();

        // Verify
        assertThat(isInitialized).isFalse();
    }

    @Test
    void isInitialized_shouldReturnTrueWhenInitialized()
    {
        // Setup
        final Supplier<Integer> supplier = getIntegerSupplier(42);
        final LazyValue<Integer> lazyValue = new LazyValue<>(supplier);

        // Execute
        lazyValue.get();
        final boolean isInitialized = lazyValue.isInitialized();

        // Verify
        assertThat(isInitialized).isTrue();
    }

    @Test
    void isInitialized_shouldReturnFalseAfterReset()
    {
        // Setup
        final Supplier<Integer> supplier = getIntegerSupplier(42);
        final LazyValue<Integer> lazyValue = new LazyValue<>(supplier);

        // Execute
        lazyValue.get();
        lazyValue.reset();
        final boolean isInitialized = lazyValue.isInitialized();

        // Verify
        assertThat(isInitialized).isFalse();
    }

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

        Assertions.assertNull(lazyValue.reset());
        Mockito.verify(supplier, Mockito.never()).get();

        Assertions.assertEquals(42, lazyValue.get());
        Assertions.assertEquals(42, lazyValue.reset());
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
