package nl.pim16aap2.testing.assertions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class AssertionsUtilTest
{
    @Test
    void constructor_shouldBeSingletonConstructor()
    {
        AssertionsUtil.assertSingletonConstructor(AssertionsUtil.class);
    }

    @Test
    void assertSingletonConstructor_shouldSucceedForClassWithSingletonConstructor()
    {
        assertThatNoException()
            .isThrownBy(() -> AssertionsUtil.assertSingletonConstructor(SingletonClassWithCorrectConstructor.class));
    }

    @Test
    void assertSingletonConstructor_shouldThrowExceptionForClassWithPublicConstructor()
    {
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> AssertionsUtil.assertSingletonConstructor(SingletonClassWithPublicConstructor.class))
            .withMessageContaining("should be private.");
    }

    @Test
    void assertSingletonConstructor_shouldThrowExceptionForClassWithMultipleConstructors()
    {
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> AssertionsUtil.assertSingletonConstructor(SingletonClassWithMultipleConstructors.class))
            .withMessageContaining("should have a single constructor.");
    }

    @Test
    void assertSingletonConstructor_shouldThrowExceptionForClassWithConstructorWithArguments()
    {
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() ->
                AssertionsUtil.assertSingletonConstructor(SingletonClassWithConstructorWithArguments.class))
            .withMessageContaining(" should have no parameters.");
    }

    private static final class SingletonClassWithCorrectConstructor
    {
        @SuppressWarnings("unused")
        private SingletonClassWithCorrectConstructor()
        {
            throw new UnsupportedOperationException("This class cannot be instantiated.");
        }
    }

    private static final class SingletonClassWithPublicConstructor
    {
        @SuppressWarnings("unused")
        public SingletonClassWithPublicConstructor()
        {
            throw new UnsupportedOperationException("This class cannot be instantiated.");
        }
    }

    private static final class SingletonClassWithMultipleConstructors
    {
        @SuppressWarnings("unused")
        private SingletonClassWithMultipleConstructors()
        {
            throw new UnsupportedOperationException("This class cannot be instantiated.");
        }

        @SuppressWarnings("unused")
        private SingletonClassWithMultipleConstructors(int value)
        {
            throw new UnsupportedOperationException("This class cannot be instantiated.");
        }
    }

    private static final class SingletonClassWithConstructorWithArguments
    {
        @SuppressWarnings("unused")
        private SingletonClassWithConstructorWithArguments(String value)
        {
            throw new UnsupportedOperationException("This class cannot be instantiated.");
        }
    }
}
