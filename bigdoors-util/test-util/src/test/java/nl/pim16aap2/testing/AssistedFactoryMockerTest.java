package nl.pim16aap2.testing;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Named;
import java.lang.reflect.Parameter;
import java.util.function.Function;

class AssistedFactoryMockerTest
{
    @Test
    void testGetParameterName()
        throws NoSuchMethodException
    {
        final Parameter[] parameters =
            TestClassWithAnnotation.class.getDeclaredConstructor(Object.class, int.class, String.class)
                                         .getParameters();
        final Parameter obj = parameters[0]; // @Assisted @Named("myObj") Object obj
        final Parameter idx = parameters[1]; // @Assisted int idx

        final Function<Named, @Nullable String> namedMapper = Named::value;
        final Function<Assisted, @Nullable String> assistedMapper = Assisted::value;

        Assertions.assertNull(AssistedFactoryMocker.getAnnotationValue(Assisted.class, obj, assistedMapper));
        Assertions.assertEquals("myObj", AssistedFactoryMocker.getAnnotationValue(Named.class, obj, namedMapper));

        Assertions.assertNull(AssistedFactoryMocker.getAnnotationValue(Assisted.class, idx, assistedMapper));
        Assertions.assertNull(AssistedFactoryMocker.getAnnotationValue(Named.class, idx, namedMapper));
    }

    @Test
    void testFindTargetCtor()
    {
        Assertions.assertThrows(NoSuchMethodException.class, () -> AssistedFactoryMocker
            .findTargetCtor(TestClassWithoutAnnotation.class));

        Assertions.assertDoesNotThrow(() -> AssistedFactoryMocker.findTargetCtor(TestClassWithAnnotation.class));
    }

    @Test
    void testFindCreationMethod()
    {
        Assertions.assertThrows(NoSuchMethodException.class, () -> AssistedFactoryMocker
            .findFactoryMethod(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactoryOnlyDefault.class));

        Assertions.assertThrows(NoSuchMethodException.class, () -> AssistedFactoryMocker
            .findFactoryMethod(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactoryTypeMisMatch.class));

        Assertions.assertDoesNotThrow(() -> AssistedFactoryMocker
            .findFactoryMethod(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class));
    }

    @Test
    public void testFullMock()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<TestClassWithAnnotation, TestClassWithAnnotation.IFactory> mocker =
            new AssistedFactoryMocker<>(TestClassWithAnnotation.class, TestClassWithAnnotation.IFactory.class);

        final Object obj = new Object();
        final TestClassWithAnnotation.IFactory factory = mocker.getFactory();
        Assertions.assertNotNull(factory);

        Assertions.assertEquals("", factory.create(obj).str);
        Assertions.assertEquals(obj, factory.create(obj).obj);
        Assertions.assertNull(factory.create(obj).obj2);
        Assertions.assertEquals(100, factory.create(obj, 100).idx);
        Assertions.assertEquals(TestClassWithAnnotation.DEFAULT_IDX, factory.create(obj).idx);

        final String testVal = "TestVal";
        mocker.setMock(String.class, null, testVal);
        Assertions.assertEquals(testVal, factory.create(obj).str);
    }

    @Test
    public void testInvalidInput()
    {
        Assertions
            .assertThrows(IllegalStateException.class,
                          () -> new AssistedFactoryMocker<>(TestClassWithAnnotation.class,
                                                            TestClassWithAnnotation.IFactoryParameterMisMatch.class));
        Assertions
            .assertThrows(IllegalArgumentException.class,
                          () -> new AssistedFactoryMocker<>(TestClassWithAnnotation.class,
                                                            TestClassWithAnnotation.IFactoryWithoutAnnotation.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new AssistedFactoryMocker<>(TestClassWithAnnotation.class,
                                                                  TestClassWithAnnotation.class));
        Assertions.assertThrows(NoSuchMethodException.class,
                                () -> new AssistedFactoryMocker<>(TestClassWithoutAnnotation.class,
                                                                  TestClassWithoutAnnotation.IFactory.class));
        Assertions.assertThrows(NoSuchMethodException.class,
                                () -> new AssistedFactoryMocker<>(TestClassWithAnnotation.class,
                                                                  TestClassWithAnnotation.IFactoryTypeMisMatch.class));
        Assertions.assertThrows(NoSuchMethodException.class,
                                () -> new AssistedFactoryMocker<>(TestClassWithAnnotation.class,
                                                                  TestClassWithAnnotation.IFactoryOnlyDefault.class));
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

        TestClassWithAnnotation(Object obj)
        {
            this(obj, DEFAULT_IDX, "NULL");
        }

        TestClassWithAnnotation(@Assisted @Named("myObj") Object obj, @Assisted int idx, String str,
                                @Nullable Object obj2)
        {
            this.obj = obj;
            this.idx = idx;
            this.str = str;
            this.obj2 = obj2;
        }

        @AssistedInject //
        TestClassWithAnnotation(@Assisted @Named("myObj") Object obj, @Assisted int idx, String str)
        {
            this(obj, idx, str, null);
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

        /**
         * Represents a factory for the {@link TestClassWithAnnotation} class without an annotation
         */
        interface IFactoryWithoutAnnotation
        {
            TestClassWithAnnotation create(@Assisted("myObj") Object obj, int idx);
        }

        /**
         * Represents a factory for {@link TestClassWithAnnotation} that does not have all @Assisted parameters.
         */
        @AssistedFactory
        interface IFactoryParameterMisMatch
        {
            TestClassWithAnnotation create(int idx);
        }

        /**
         * Represents a factory for {@link TestClassWithAnnotation} that returns the incorrect type from its creator.
         */
        @AssistedFactory
        interface IFactoryTypeMisMatch
        {
            String create(@Assisted("myObj") Object obj, int idx);
        }

        /**
         * Represents a factory for {@link TestClassWithAnnotation} that only has a default method.
         */
        @AssistedFactory
        interface IFactoryOnlyDefault
        {
            default TestClassWithAnnotation create(Object obj, int idx)
            {
                return new TestClassWithAnnotation(new Object(), DEFAULT_IDX, "NULL");
            }
        }
    }

    /**
     * Represents a class with the correct AssistedInject parameter.
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    static class TestClassWithoutAnnotation
    {
        private final Object obj;
        private final int idx;
        private final String str;

        TestClassWithoutAnnotation(@Assisted Object obj, @Assisted int idx, String str)
        {
            this.obj = obj;
            this.idx = idx;
            this.str = str;
        }

        /**
         * Represents the correct factory for {@link TestClassWithoutAnnotation}.
         */
        @AssistedFactory
        interface IFactory
        {
            TestClassWithAnnotation create(Object obj, int idx);
        }
    }
}
