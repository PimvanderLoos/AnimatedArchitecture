package nl.pim16aap2.testing;

import org.mockito.Mockito;

import java.lang.reflect.Constructor;

/**
 * Simple class that creates real instances of a class using its constructor that is annotated with
 * {@link javax.inject.Inject}.
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
     *     The class to create an instance of. This class must have a constructor annotated with
     *     {@link javax.inject.Inject}.
     * @throws IllegalArgumentException
     *     If no constructor annotated with {@link javax.inject.Inject} is found.
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
            if (constructor.isAnnotationPresent(javax.inject.Inject.class))
                //noinspection unchecked
                return (Constructor<T>) constructor;
        }
        throw new IllegalArgumentException("No constructor annotated with @Inject found in class " + clz.getName());
    }

    /**
     * Creates a new instance of the class using the constructor annotated with {@link javax.inject.Inject}.
     *
     * @return The new instance.
     *
     * @throws RuntimeException
     *     If an exception occurs while creating the instance.
     */
    public T createInstance()
    {
        try
        {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            final Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++)
                parameters[i] = Mockito.mock(parameterTypes[i]);
            return constructor.newInstance(parameters);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new instance of the class using the constructor annotated with {@link javax.inject.Inject}.
     *
     * @param clz
     *     The class to create an instance of. This class must have a constructor annotated with
     *     {@link javax.inject.Inject}.
     * @param <T>
     *     The type of the class to create an instance of.
     * @return The new instance.
     *
     * @throws IllegalArgumentException
     *     If no constructor annotated with {@link javax.inject.Inject} is found.
     * @throws RuntimeException
     *     If an exception occurs while creating the instance.
     */
    public static <T> T injectInto(Class<T> clz)
        throws IllegalArgumentException, RuntimeException
    {
        return new MockInjector<>(clz).createInstance();
    }
}
