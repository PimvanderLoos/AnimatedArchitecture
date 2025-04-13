package nl.pim16aap2.testing;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistedFactoryMockerTest
{
    @Test
    void testGetParameterName()
        throws NoSuchMethodException
    {
        final Parameter[] parameters = TestClassWithAnnotation.class
            .getDeclaredConstructor(Object.class, int.class, String.class, List.class)
            .getParameters();

        final Parameter obj = parameters[0]; // @Assisted @Named("myObj") Object obj
        final Parameter idx = parameters[1]; // @Assisted int idx

        final Function<Assisted, @Nullable String> assistedMapper = Assisted::value;
        assertEquals("myObj", AssistedFactoryMocker.getAnnotationValue(Assisted.class, obj, assistedMapper));
        assertNull(AssistedFactoryMocker.getAnnotationValue(Assisted.class, idx, assistedMapper));
    }

    @Test
    void testFindTargetCtor()
    {
        assertDoesNotThrow(() -> AssistedFactoryMocker.findTargetCtor(TestClassWithAnnotation.class));
    }

    @Test
    void testFindCreationMethod()
    {
        assertDoesNotThrow(() -> AssistedFactoryMocker
            .findFactoryMethod(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class));
    }

    @Test
    void testFullMock()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<TestClassWithAnnotation, TestClassWithAnnotation.IFactory> mocker =
            new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class);

        final Object obj = new Object();
        final TestClassWithAnnotation.IFactory factory = mocker.getFactory();
        Assertions.assertNotNull(factory);

        assertEquals("", factory.create(obj).str);
        assertEquals(obj, factory.create(obj).obj);
        assertNull(factory.create(obj).obj2);
        assertEquals(100, factory.create(obj, 100).idx);
        assertEquals(TestClassWithAnnotation.DEFAULT_IDX, factory.create(obj).idx);

        final String testVal = "TestVal";
        mocker.injectParameter(String.class, null, testVal);
        assertEquals(testVal, factory.create(obj).str);
    }

    @Test
    void testInvalidInput()
    {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.class));
    }

    @Test
    void injectParameters_shouldMapUntypedParameters()
        throws NoSuchMethodException
    {
        // Setup
        final var afm =
            new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class);
        final List<String> lst = new ArrayList<>();
        lst.add("DEF");

        // Execute
        afm.injectParameters("ABC", lst);
        final var factory = afm.getFactory();
        final var result = factory.create(new Object());

        // Verify
        assertThat(result.lst).isEqualTo(lst);
        assertThat(result.str).isEqualTo("ABC");
    }

    @Test
    void injectParameters_shouldMapUntypedMockedParameters()
        throws NoSuchMethodException
    {
        // Setup
        final var afm =
            new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class);
        final List<String> lst = mock();

        // Execute
        afm.injectParameters("ABC", lst);
        final var factory = afm.getFactory();
        final var result = factory.create(new Object());

        // Verify
        assertThat(result.lst).isSameAs(lst);
        assertThat(result.str).isEqualTo("ABC");
    }

    @Test
    void injectParameters_shouldThrowExceptionForUnmappedType()
        throws NoSuchMethodException
    {
        // Setup
        final var afm =
            new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class);

        // Execute & Verify
        assertThatThrownBy(() -> afm.injectParameters("ABC", new Object()))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Failed to find a matching parameter for ");
    }

    @Test
    void injectMocksFromTestClass_shouldInjectAllMockedFields()
        throws Exception
    {
        final var testObj = new TestClassWithMockAnnotatedFields();
        try (var mocks = MockitoAnnotations.openMocks(testObj))
        {
            final var afm = AssistedFactoryMocker.injectMocksFromTestClass(
                TestClassWithAnnotation.class,
                TestClassWithAnnotation.IFactory.class,
                testObj
            );

            final var result = afm.getFactory().create(new Object());

            assertThat(result.lst).isSameAs(testObj.lst);
            assertThat(result.obj).isNotSameAs(testObj.obj);
        }
    }

    static class TestClassWithMockAnnotatedFields
    {
        @Mock
        private Object obj;

        @Mock
        private List<String> lst;
    }

    /**
     * Represents a class with the correct AssistedInject parameter on its constructor.
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    @Getter
    static class TestClassWithAnnotation
    {
        public static final int DEFAULT_IDX = -1;

        private final Object obj;
        private final int idx;
        private final String str;
        private final @Nullable Object obj2;
        private final @Nullable List<String> lst;

        TestClassWithAnnotation(Object obj)
        {
            this(obj, DEFAULT_IDX, "NULL", null);
        }

        TestClassWithAnnotation(Object obj, int idx, String str, @Nullable Object obj2, @Nullable List<String> lst)
        {
            this.obj = obj;
            this.idx = idx;
            this.str = str;
            this.obj2 = obj2;
            this.lst = lst;
        }

        @AssistedInject
        TestClassWithAnnotation(
            @Assisted("myObj") Object obj,
            @Assisted int idx,
            String str,
            @Nullable List<String> lst)
        {
            this(obj, idx, str, null, lst);
        }

        /**
         * Represents the correct factory for the {@link TestClassWithAnnotation} class.
         */
        @AssistedFactory
        interface IFactory
        {
            TestClassWithAnnotation create(@Assisted("myObj") Object obj, int idx);

            default TestClassWithAnnotation create(Object obj)
            {
                return create(obj, TestClassWithAnnotation.DEFAULT_IDX);
            }
        }
    }
}
