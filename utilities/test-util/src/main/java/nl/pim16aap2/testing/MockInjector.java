package nl.pim16aap2.testing;

import jakarta.inject.Inject;
import org.mockito.Mockito;
import dagger.Lazy;
import nl.pim16aap2.util.LazyValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

/**
 * Simple class that creates real instances of a class using its constructor that is annotated with {@link Inject}.
 * <p>
 * All arguments to the constructor are mocked instances of those classes.
 *
 * @param <T>
 *     The type of the class to create an instance of.
 */
public class MockInjector<T>
{
    private final Constructor<T> constructor;

    /**
     * Creates a new instance of this class.
     *
     * @param clz
     *     The class to create an instance of. This class must have a constructor annotated with {@link Inject}.
     * @throws IllegalArgumentException
     *     If no constructor annotated with {@link Inject} is found.
     */
    public MockInjector(Class<T> clz)
        throws IllegalArgumentException
    {
        this.constructor = getConstructor(clz);
    }

    private Constructor<T> getConstructor(Class<T> clz)
        throws IllegalArgumentException
    {
        for (final Constructor<?> constructor : clz.getDeclaredConstructors())
        {
            if (constructor.isAnnotationPresent(Inject.class))
            {
                constructor.setAccessible(true);
                //noinspection unchecked
                return (Constructor<T>) constructor;
            }
        }
        throw new IllegalArgumentException("No constructor annotated with @Inject found in class " + clz.getName());
    }

    /**
     * Creates a new instance of the class using the constructor annotated with {@link Inject}.
     *
     * @param additionalParameters
     *     Additional parameters to pass to the constructor. If the class of a parameter of the constructor matches one
     *     of the additional parameters, that parameter will be used instead of new mocked instance.
     * @return The new instance.
     *
     * @throws RuntimeException
     *     If an exception occurs while creating the instance.
     */
    public T createInstance(Object... additionalParameters)
    {
        Map<Class<?>, Object> additionalParametersMap = Stream.of(additionalParameters)
            .collect(Collectors.toMap(
                Object::getClass,
                Function.identity(),
                (a, b) ->
                {
                    throw new IllegalArgumentException(
                        "Cannot have multiple parameters of the same type: " + a.getClass().getName());
                }));

        try
        {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            final Type[] genericParameterTypes = constructor.getGenericParameterTypes();

            final Object[] parameters = new Object[parameterTypes.length];
            for (int idx = 0; idx < parameterTypes.length; idx++)
            {
                parameters[idx] = ConstructorParameterType
                    .of(parameterTypes[idx], genericParameterTypes[idx])
                    .createInstance(additionalParametersMap);
            }

            return constructor.newInstance(parameters);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * Creates a new instance of the class using the constructor annotated with {@link Inject}.
     *
     * @param clz
     *     The class to create an instance of. This class must have a constructor annotated with {@link Inject}.
     * @param <T>
     *     The type of the class to create an instance of.
     * @return The new instance.
     *
     * @throws IllegalArgumentException
     *     If no constructor annotated with {@link Inject} is found.
     * @throws RuntimeException
     *     If an exception occurs while creating the instance.
     */
    public static <T> T injectInto(Class<T> clz)
        throws IllegalArgumentException, RuntimeException
    {
        return new MockInjector<>(clz).createInstance();
    }

    record ConstructorParameterType(
        Class<?> type,
        boolean isLazy)
    {
        public static ConstructorParameterType of(Class<?> rawType, Type genericType)
        {
            if (rawType == Lazy.class && genericType instanceof ParameterizedType parameterizedType)
            {
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                Class<?> lazyType = (Class<?>) actualTypeArgument;
                return new ConstructorParameterType(lazyType, true);
            }
            return new ConstructorParameterType(rawType, false);
        }

        public Object createInstance(Map<Class<?>, Object> additionalParameterMap)
        {
            final Object instance = additionalParameterMap.entrySet().stream()
                .filter(entry -> type.isAssignableFrom(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> mock(type));

            if (isLazy)
                return new LazyValue<>(() -> instance);
            return instance;
        }
    }
}
