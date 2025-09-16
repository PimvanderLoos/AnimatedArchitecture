package nl.pim16aap2.testing;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import jakarta.inject.Named;
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
            .getDeclaredConstructor(Object.class, int.class, String.class, List.class, Object.class)
            .getParameters();

        final Parameter obj = parameters[0]; // @Assisted @Assisted("myObj") Object obj
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
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);
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
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);
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
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Execute & Verify
        assertThatThrownBy(() -> afm.injectParameters("ABC", new Object()))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Failed to find a matching parameter for ");
    }

    @Test
    void injectParameter_withName_shouldMapToCorrectParameter()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);
        final Object obj = new Object();

        // Execute
        afm.injectParameter(Object.class, "namedObj", obj);
        final var factory = afm.getFactory();
        final var result = factory.create(new Object());

        // Verify
        //noinspection DataFlowIssue
        assertThat(result.getNamedObj()).isSameAs(obj);
    }

    @Test
    void injectParameter_withName_shouldThrowExceptionForNameMismatch()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Execute & Verify
        assertThatThrownBy(() -> afm.injectParameter(Object.class, "wrongName", new Object()))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Failed to find a matching parameter for ");
    }

    @Test
    void injectParameter_withoutName_shouldThrowExceptionForName()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Execute & Verify
        assertThatThrownBy(() -> afm.injectParameter(Object.class, new Object()))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Failed to find a matching parameter for ");
    }

    @Test
    void injectMocksFromTestClass_shouldInjectAllMockedFields()
        throws Exception
    {
        final var testObj = new TestClassWithMockAnnotatedFields();
        try (var ignored = MockitoAnnotations.openMocks(testObj))
        {
            final var afm = AssistedFactoryMocker.injectMocksFromTestClass(
                TestClassWithAnnotation.IFactory.class,
                testObj
            );

            final var result = afm.getFactory().create(new Object());

            assertThat(result.lst).isSameAs(testObj.lst);
            assertThat(result.obj).isNotSameAs(testObj.obj);
        }
    }

    @Test
    void findTargetClass_shouldReturnClass()
    {
        assertThat(AssistedFactoryMocker.findTargetClass(TestClassWithAnnotation.IFactory.class))
            .isEqualTo(TestClassWithAnnotation.class);
    }

    @Test
    void findTargetClass_shouldThrowExceptionForInvalidClass()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> AssistedFactoryMocker.findTargetClass(Object.class))
            .withCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void injectParameter_withoutName_shouldInjectParameter()
    {
        // Setup
        final var afm =
            new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class);

        final String testVal = "TestVal";

        // Execute
        final TestClassWithAnnotation.IFactory factory = afm.injectParameter(String.class, testVal).getFactory();
        final var result = factory.create(new Object());

        // Verify
        assertThat(result.str).isEqualTo(testVal);
    }

    @Test
    void getParameter_shouldReturnProvidedParameter()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);
        final List<String> lst = List.of("TestVal");

        // Execute
        afm.injectParameter(lst);

        // Verify
        assertThat(afm.getParameter(List.class)).isSameAs(lst);
    }

    @Test
    void getParameter_shouldThrowExceptionForTypeMismatch()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);
        final List<String> lst = new ArrayList<>();
        lst.add("TestVal");

        // Execute
        afm.injectParameter(lst);

        // Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> afm.getParameter(ArrayList.class))
            .withMessageStartingWith("Could not find a mapping for a mocked object with type: ");
    }

    @Test
    void getParameter_shouldReturnMockedValueForUnsetParameter()
    {
        // Setup & Execute
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Verify
        assertThat(afm.getParameter(List.class)).isNotNull();
    }

    @Test
    void getParameter_shouldThrowExceptionForTypeThatDoesNotExist()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> afm.getParameter(Double.class))
            .withMessageStartingWith("Could not find a mapping for a mocked object with type: ");
    }

    @Test
    void getParameter_withName_shouldReturnNamedParameter()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);
        final Object obj = new Object();

        // Execute
        afm.injectParameter(Object.class, "namedObj", obj);

        // Verify
        assertThat(afm.getParameter(Object.class, "namedObj")).isSameAs(obj);
    }

    @Test
    void getParameter_withName_shouldThrowExceptionWithoutNameForNamedParameter()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> afm.getParameter(Object.class))
            .withMessageStartingWith("Could not find a mapping for a mocked object with type: ");
    }

    @Test
    void getParameter_withName_shouldThrowExceptionForNameMismatch()
    {
        // Setup
        final var afm = new AssistedFactoryMocker<>(TestClassWithAnnotation.IFactory.class);

        // Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> afm.getParameter(Object.class, "wrongName"))
            .withMessageStartingWith("Could not find a mapping for a mocked object with type: ");
    }

    @SuppressWarnings("NullAway")
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
        private final @Nullable Object namedObj;

        TestClassWithAnnotation(Object obj)
        {
            this(obj, DEFAULT_IDX, "NULL", null, null);
        }

        TestClassWithAnnotation(
            Object obj,
            int idx,
            String str,
            @Nullable Object obj2,
            @Nullable List<String> lst,
            @Nullable Object namedObj)
        {
            this.obj = obj;
            this.idx = idx;
            this.str = str;
            this.obj2 = obj2;
            this.lst = lst;
            this.namedObj = namedObj;
        }

        @AssistedInject
        TestClassWithAnnotation(
            @Assisted("myObj") Object obj,
            @Assisted int idx,
            String str,
            @Nullable List<String> lst,
            @Named("namedObj") @Nullable Object namedObj)
        {
            this(obj, idx, str, null, lst, namedObj);
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
