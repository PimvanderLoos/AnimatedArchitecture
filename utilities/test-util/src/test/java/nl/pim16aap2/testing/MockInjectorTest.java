package nl.pim16aap2.testing;

import dagger.Lazy;
import jakarta.inject.Inject;
import nl.pim16aap2.util.LazyValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MockInjectorTest
{
    @Test
    void constructor_withValidInjectAnnotatedClass_shouldSucceed()
    {
        // setup & execute
        final MockInjector<TestClassWithInject> injector = new MockInjector<>(TestClassWithInject.class);

        // verify
        assertThat(injector).isNotNull();
    }

    @Test
    void constructor_withNoInjectAnnotation_shouldThrowException()
    {
        assertThatThrownBy(() -> new MockInjector<>(TestClassWithoutInject.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No constructor annotated with @Inject found");
    }

    @Test
    void createInstance_withNoParameters_shouldCreateInstanceWithMocks()
    {
        // setup
        final MockInjector<TestClassWithInject> injector = new MockInjector<>(TestClassWithInject.class);

        // execute
        final TestClassWithInject instance = injector.createInstance();

        // verify
        assertThat(instance).isNotNull();
        assertThat(instance.dependency).isNotNull();
    }

    @Test
    void createInstance_withAdditionalParameter_shouldUseProvidedParameter()
    {
        // setup
        final MockInjector<TestClassWithInject> injector = new MockInjector<>(TestClassWithInject.class);
        final TestDependency providedDependency = new TestDependency();

        // execute
        TestClassWithInject instance = injector.createInstance(providedDependency);

        // verify
        assertThat(instance.dependency).isSameAs(providedDependency);
    }

    @Test
    void createInstance_withDuplicateParameterTypes_shouldThrowException()
    {
        // setup
        final MockInjector<TestClassWithInject> injector = new MockInjector<>(TestClassWithInject.class);
        final TestDependency dep1 = new TestDependency();
        final TestDependency dep2 = new TestDependency();

        // execute & verify
        assertThatThrownBy(() -> injector.createInstance(dep1, dep2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot have multiple parameters of the same type");
    }

    @Test
    void createInstance_withLazyParameter_shouldCreateLazyValue()
    {
        // setup
        final MockInjector<TestClassWithLazy> injector = new MockInjector<>(TestClassWithLazy.class);

        // execute
        final TestClassWithLazy instance = injector.createInstance();

        // verify
        assertThat(instance.lazyDependency).isInstanceOf(LazyValue.class);
        assertThat(instance.lazyDependency.get()).isNotNull();
    }

    @Test
    void createInstance_withLazyParameterAndProvidedInstance_shouldUseLazyWithProvidedInstance()
    {
        // setup
        final MockInjector<TestClassWithLazy> injector = new MockInjector<>(TestClassWithLazy.class);
        final TestDependency providedDependency = new TestDependency();

        // execute
        final TestClassWithLazy instance = injector.createInstance(providedDependency);

        // verify
        assertThat(instance.lazyDependency).isInstanceOf(LazyValue.class);
        assertThat(instance.lazyDependency.get()).isSameAs(providedDependency);
    }

    @Test
    void createInstance_withMultipleParameters_shouldHandleCorrectly()
    {
        // setup
        final MockInjector<TestClassWithMultipleParams> injector = new MockInjector<>(TestClassWithMultipleParams.class);
        final TestDependency providedDependency = new TestDependency();

        // execute
        final TestClassWithMultipleParams instance = injector.createInstance(providedDependency);

        // verify
        assertThat(instance.dependency).isSameAs(providedDependency);
        assertThat(instance.otherDependency).isNotNull();
    }

    @Test
    void injectInto_staticMethod_shouldCreateInstance()
    {
        // setup & execute
        final TestClassWithInject instance = MockInjector.injectInto(TestClassWithInject.class);

        // verify
        assertThat(instance).isNotNull();
        assertThat(instance.dependency).isNotNull();
    }

    @Test
    void injectInto_withInvalidClass_shouldThrowException()
    {
        assertThatThrownBy(() -> MockInjector.injectInto(TestClassWithoutInject.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorParameterType_of_withRegularType_shouldReturnCorrectType()
    {
        // setup & execute
        final MockInjector.ConstructorParameterType paramType =
            MockInjector.ConstructorParameterType.of(String.class, String.class);

        // verify
        assertThat(paramType.type()).isEqualTo(String.class);
        assertThat(paramType.isLazy()).isFalse();
    }

    @Test
    void constructorParameterType_createInstance_withAdditionalParameter_shouldUseProvidedInstance()
    {
        // setup
        final MockInjector.ConstructorParameterType paramType =
            MockInjector.ConstructorParameterType.of(TestDependency.class, TestDependency.class);
        final TestDependency providedInstance = new TestDependency();
        final Map<Class<?>, Object> additionalParams = Map.of(TestDependency.class, providedInstance);

        // execute
        final Object result = paramType.createInstance(additionalParams);

        // verify
        assertThat(result).isSameAs(providedInstance);
    }

    // Test classes
    static class TestClassWithInject
    {
        final TestDependency dependency;

        @Inject
        TestClassWithInject(TestDependency dependency)
        {
            this.dependency = dependency;
        }
    }

    static class TestClassWithoutInject
    {
        final TestDependency dependency;

        TestClassWithoutInject(TestDependency dependency)
        {
            this.dependency = dependency;
        }
    }

    static class TestClassWithLazy
    {
        final Lazy<TestDependency> lazyDependency;

        @Inject
        TestClassWithLazy(Lazy<TestDependency> lazyDependency)
        {
            this.lazyDependency = lazyDependency;
        }
    }

    static class TestClassWithMultipleParams
    {
        final TestDependency dependency;
        final OtherTestDependency otherDependency;

        @Inject
        TestClassWithMultipleParams(TestDependency dependency, OtherTestDependency otherDependency)
        {
            this.dependency = dependency;
            this.otherDependency = otherDependency;
        }
    }

    static class TestDependency {}

    static class OtherTestDependency {}
}
