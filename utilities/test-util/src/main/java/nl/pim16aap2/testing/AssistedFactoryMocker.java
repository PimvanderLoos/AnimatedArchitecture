package nl.pim16aap2.testing;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a class that can be used to create mocked objects for an {@link AssistedFactory}.
 * <p>
 * Any dependencies that cannot be mocked will default to null. These can later be manually updated using
 * {@link AssistedFactoryMocker#setMock(Class, String, Object)} if needed.
 * <p>
 * Example usage:
 * <pre>{@code
 * // The class with the AssistedInject constructor that we want to create a factory for.
 * public class MyObject {
 *     @AssistedInject
 *     MyObject(MyDependency myDependency,
 *              @Assisted String someString) {
 *         ...
 *     }
 * }
 *
 * // The interface that represents the factory.
 * @AssistedFactory
 * public interface MyObjectFactory {
 *    MyObject create(String someString);
 * }
 *
 * // Create a new factory using an existing instance of MyDependency.
 * public MyObjectFactory createMyObjectFactory(MyDependency existingMyDependency) {
 *     final var mocker = new AssistedFactoryMocker<>(MyObject.class, MyObjectFactory.class);
 *     mocker.setMock(MyDependency.class, existingMyDependency);
 *     return mocker.getFactory();
 * }
 *
 * public MyObject createMyObject(MyObjectFactory factory, String someString) {
 *     return factory.create(someString);
 * }
 * }</pre>
 *
 * @param <T>
 *     The type of the class that is instantiated by the factory.
 * @param <U>
 *     The type of the factory that instantiates the target class.
 */
public class AssistedFactoryMocker<T, U>
{
    /**
     * The class that is used to create new instances of the target class.
     */
    private final Class<U> factoryClass;

    /**
     * The factory instance that can be used to create new instances of the target class.
     */
    private final U factory;

    /**
     * The method in the factory class that is used to create new instances of the target class.
     */
    private final Method factoryMethod;

    /**
     * The target constructor of the target class. This is the constructor that is annotated with
     * {@link AssistedInject}.
     */
    private final Constructor<T> targetCtor;

    /**
     * The list of parameters of the target constructor (see {@link #targetCtor}).
     * <p>
     * The parameters are mapped from their index in the factory method to their index in the target constructor.
     */
    private final List<MappedParameter> mappedParameters;

    /**
     * The parameters that are injected into the target class and are not provided by the factory method.
     * <p>
     * The keys of the map are provided by {@link MappedParameter#getNamedTypeHash(Class, String)}.
     */
    private final Int2ObjectMap<MappedParameter> injectedParameters;

    /**
     * The mock settings that are used to create the injected objects for the factory (i.e. the objects that are not
     * provided by the factory method).
     */
    private final MockSettings mockSettings;

    /**
     * @param targetClass
     *     The class that is instantiated by the factory.
     * @param factoryClass
     *     The factory class that instantiates the target class.
     * @param mockSettings
     *     The mock settings that are used to create the injected objects for the factory (i.e. the objects that are not
     *     provided by the factory method).
     * @throws NoSuchMethodException
     *     If the factory method or the target constructor could not be found.
     */
    public AssistedFactoryMocker(Class<T> targetClass, Class<U> factoryClass, MockSettings mockSettings)
        throws NoSuchMethodException
    {
        if (!factoryClass.isAnnotationPresent(AssistedFactory.class))
            throw new IllegalArgumentException(
                "Factory class " + factoryClass + " is not annotated with " + AssistedFactory.class);

        if (!Modifier.isInterface(factoryClass.getModifiers()) && !Modifier.isAbstract(factoryClass.getModifiers()))
            throw new IllegalArgumentException(
                "Factory class " + factoryClass + " is not an interface or an abstract class!");

        this.factoryClass = factoryClass;
        this.mockSettings = mockSettings;

        factoryMethod = findFactoryMethod(targetClass, factoryClass);
        targetCtor = findTargetCtor(targetClass);

        validateAssistedParameterCounts();

        mappedParameters = mapParameters(factoryMethod, targetCtor);
        injectedParameters = insertMocks();

        factory = Objects.requireNonNull(constructFactory());
    }

    /**
     * @param targetClass
     *     The class that is instantiated by the factory.
     * @param factoryClass
     *     The factory class that instantiates the target class.
     * @param defaultAnswer
     *     The default answer that is used for all mocks that are created for the factory.
     * @throws NoSuchMethodException
     *     If the factory method or the target constructor could not be found.
     */
    @SuppressWarnings("unused")
    public AssistedFactoryMocker(Class<T> targetClass, Class<U> factoryClass, Answer<?> defaultAnswer)
        throws NoSuchMethodException
    {
        this(targetClass, factoryClass, Mockito.withSettings().defaultAnswer(defaultAnswer));
    }

    /**
     * @param targetClass
     *     The class that is instantiated by the factory.
     * @param factoryClass
     *     The factory class that instantiates the target class.
     * @throws NoSuchMethodException
     *     If the factory method or the target constructor could not be found.
     */
    @SuppressWarnings("unused")
    public AssistedFactoryMocker(Class<T> targetClass, Class<U> factoryClass)
        throws NoSuchMethodException
    {
        this(targetClass, factoryClass, Mockito.withSettings());
    }

    /**
     * Creates a new instance of the target class using the provided parameters.
     * <p>
     * The arguments of the invocation are mixed in with the mocked/provided instances to get the final array of
     * parameters of the target constructor.
     *
     * @param invocation
     *     The invocation of the factory method.
     * @return The new instance of the target class.
     */
    T instantiateNewTarget(InvocationOnMock invocation)
    {
        final Object[] ctorParams = new Object[targetCtor.getParameterCount()];
        final Object[] factoryParams = invocation.getArguments();

        for (final MappedParameter parameter : mappedParameters)
        {
            final int idx = parameter.getTargetIdx();

            final @Nullable Object value =
                parameter.isInjected() ?
                    parameter.getValue() :
                    factoryParams[parameter.getFactoryIdx()];

            //noinspection ConstantConditions
            ctorParams[idx] = value;
        }

        try
        {
            return targetCtor.newInstance(ctorParams);
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("""
                    Failed to create new instance of target class: '%s'
                    Used factory method: '%s'
                    Target constructor:  '%s'
                    Arguments:            %s
                    """,
                targetCtor.getDeclaringClass(),
                factoryMethod.toGenericString(),
                targetCtor.toGenericString(),
                Stream
                    .of(ctorParams)
                    .map(val -> String.format("{type=%s, value=\"%s\"}", (val == null ? null : val.getClass()), val))
                    .collect(Collectors.joining(", ", "{", "}"))
            ), e);
        }
    }

    /**
     * Gets the factory class that can be used to construct new instances of the target class using the values provided
     * through the factory and the mocked instances for the remaining values.
     *
     * @return The factory instance.
     */
    public U getFactory()
    {
        return factory;
    }

    /**
     * See {@link #getMock(boolean, Class, String)}.
     * <p>
     * Shortcut for unnamed parameters.
     */
    public <V> V getMock(Class<V> type)
    {
        return getMock(true, type, null);
    }

    /**
     * See {@link #getMock(boolean, Class, String)}.
     */
    @SuppressWarnings("unused")
    public <V> V getMock(Class<V> type, @Nullable String name)
    {
        return getMock(true, type, name);
    }

    /**
     * Gets the mocked object of the given type.
     *
     * @param nonNull
     *     Whether to allow null values. If this is true, an exception will be thrown in case the return value would be
     *     null.
     * @param type
     *     The type of the mocked object to get.
     * @param name
     *     The name of the mocked object, as specified by {@link Assisted#value()} in the constructor..
     * @param <V>
     *     The type of the mocked object to get.
     * @return The mocked object of the given type with the given name.
     *
     * @throws IllegalArgumentException
     *     when null values are not allowed and no existing mock exists for the given type and name combination.
     */
    @SuppressWarnings("unused")
    @Contract("true, _, _ -> !null")
    public <V> V getMock(boolean nonNull, Class<V> type, @Nullable String name)
    {
        final @Nullable MappedParameter param = injectedParameters.get(MappedParameter.getNamedTypeHash(type, name));
        //noinspection unchecked
        final @Nullable V ret = param == null ? null : (V) param.getValue();
        if (ret == null)
            throw new IllegalArgumentException(
                "Could not find a mapping for a mocked object with type: " + type + " and name: " + name);
        return ret;
    }

    /**
     * Checks if the assisted factory mocker has a mocked object of the given type and with the given name.
     *
     * @param type
     *     The type of the mocked object to get.
     * @param name
     *     The name of the mocked object, as specified by {@link Assisted#value()} in the constructor.
     * @return True if a mocked object exists of the given type and name.
     */
    @SuppressWarnings("unused")
    public boolean hasMock(Class<?> type, @Nullable String name)
    {
        return injectedParameters.containsKey(MappedParameter.getNamedTypeHash(type, name));
    }

    /**
     * See {@link #setMock(Class, String, Object)}.
     * <p>
     * Shortcut for unnamed parameters.
     */
    @SuppressWarnings("unused")
    public <V> AssistedFactoryMocker<T, U> setMock(Class<V> type, V mock)
    {
        return setMock(type, null, mock);
    }

    /**
     * Sets the mocked instance of a constructor parameter.
     * <p>
     * Only works for parameters that do not exist in the factory method.
     *
     * @param type
     *     The type of the mocked object.
     * @param name
     *     The name of the mocked object, as specified by {@link Assisted#value()} in the constructor.
     * @param mock
     *     The new mocked object to use as constructor parameter.
     * @param <V>
     *     The type of the mocked object.
     * @return The current {@link AssistedFactoryMocker} instance.
     */
    public <V> AssistedFactoryMocker<T, U> setMock(Class<V> type, @Nullable String name, V mock)
    {
        final MappedParameter param = Objects.requireNonNull(
            injectedParameters.get(MappedParameter.getNamedTypeHash(type, name)),
            String.format(
                "No mocked parameter of type %s and name %s could be found in constructor: %s",
                type,
                name,
                targetCtor
            ));
        param.setValue(mock);
        return this;
    }

    /**
     * Creates a new (mocked) instance of the factory class that can be used to construct new instances of the target
     * class using the values provided through the factory and the mocked instances for the remaining values.
     *
     * @return The new factory instance.
     */
    private U constructFactory()
    {
        final Function<InvocationOnMock, Object> mapper = this::instantiateNewTarget;
        return Mockito.mock(factoryClass, new DefaultAnswer(mapper, factoryMethod));
    }

    /**
     * Creates mocked instances of the constructor parameter types that aren't provided by the creator method.
     * <p>
     * See {@link TestUtil#newMock(Class, MockSettings)}.
     * <p>
     * The mocked instances are created using {@link #mockSettings}.
     *
     * @return The map of the mocked parameters with the {@link MappedParameter#getNamedTypeHash()}
     */
    private Int2ObjectMap<MappedParameter> insertMocks()
    {
        final Int2ObjectMap<MappedParameter> ret = new Int2ObjectOpenHashMap<>();
        for (final MappedParameter parameter : mappedParameters)
        {
            if (!parameter.isInjected())
                continue;
            parameter.setValue(TestUtil.newMock(parameter.getType(), mockSettings));
            ret.put(parameter.getNamedTypeHash(), parameter);
        }
        return ret;
    }

    /**
     * Ensures that the number of assisted parameters provided by {@link #factoryMethod} matches the number of assisted
     * parameters expected by {@link #targetCtor}
     *
     * @throws IllegalStateException
     *     When the number of assisted parameters does not match the number of provided parameters.
     */
    private void validateAssistedParameterCounts()
    {
        int assistedTargetCount = 0;
        for (final Parameter parameter : targetCtor.getParameters())
            if (parameter.isAnnotationPresent(Assisted.class))
                assistedTargetCount++;

        if (assistedTargetCount != factoryMethod.getParameterCount())
            throw new IllegalStateException(
                "Factory method provides " + factoryMethod.getParameterCount() +
                    " parameters while the constructor expects " + assistedTargetCount);
    }

    /**
     * Gets the name of a parameter if it is annotated with {@link Assisted}.
     *
     * @param parameter
     *     The parameter to analyze.
     * @return The name as provided by {@link Assisted#value()} if the parameter has a non-blank name, otherwise null.
     */
    static @Nullable <T extends Annotation> String getAnnotationValue(
        Class<T> annotationType,
        Parameter parameter,
        Function<T, @Nullable String> mapper)
    {
        final @Nullable T annotation = parameter.getAnnotation(annotationType);
        //noinspection ConstantConditions
        if (annotation == null)
            return null;
        final @Nullable String val = mapper.apply(annotation);
        if ("".equals(val))
            return null;
        return val;
    }

    /**
     * Gets the {@link ParameterDescription}s from an array of {@link Parameter}s.
     *
     * @param parameters
     *     The parameters to analyze.
     * @return The list of {@link ParameterDescription}s representing the parameters.
     */
    static List<ParameterDescription> getParameterDescriptions(Parameter... parameters)
    {
        final ArrayList<ParameterDescription> ret = new ArrayList<>(parameters.length);
        ret.ensureCapacity(parameters.length);
        for (final Parameter parameter : parameters)
        {
            @Nullable String named = getAnnotationValue(Assisted.class, parameter, Assisted::value);
            if (named == null)
                named = getAnnotationValue(Named.class, parameter, Named::value);

            final boolean isAssisted = parameter.isAnnotationPresent(Assisted.class);
            ret.add(new ParameterDescription(parameter.getType(), isAssisted, named));
        }
        return ret;
    }

    /**
     * Finds which {@link ParameterDescription} from a list of parameter descriptions matches the provided parameter
     * description.
     *
     * @param param
     *     The target parameter description.
     * @param matches
     *     The list of parameter descriptions to search through.
     * @return The matching parameter description from the list of matches.
     *
     * @throws IllegalStateException
     *     When 0 or more than 1 match was found.
     */
    static ParameterDescription getMatchingParameter(ParameterDescription param, List<ParameterDescription> matches)
    {
        if (param.name() == null && matches.size() > 1)
            throw new IllegalStateException(
                "Found more than one match for param " + param + "!" + " Potential matches: " + matches);

        if (matches.size() == 1)
        {
            final ParameterDescription match = matches.getFirst();
            if ((match.name() == null && param.name == null) ||
                (match.name() != null && param.name != null))
                return match;
        }

        final String paramName = Objects.requireNonNull(
            param.name(),
            "Name of parameter " + param + " cannot be null!"
        );

        final List<ParameterDescription> result = matches.stream().filter(val -> paramName.equals(val.name())).toList();
        if (result.isEmpty())
            throw new IllegalStateException(
                "Failed to find a matching factory parameter for constructor parameter: " + param);

        if (result.size() > 1)
            throw new IllegalStateException("Found too many matches for constructor param " + param + ": " + result);

        return result.getFirst();
    }

    /**
     * Maps the parameters of the factory method to the target constructor.
     *
     * @param factoryParams
     *     The {@link ParameterDescription}s of the parameters of the factory method.
     * @param ctorParams
     *     The {@link ParameterDescription}s of the parameters of the constructor.
     * @return A list of {@link MappedParameter}s.
     */
    static List<MappedParameter> getMappedParameters(
        List<ParameterDescription> factoryParams,
        List<ParameterDescription> ctorParams)
    {
        final List<MappedParameter> ret = new ArrayList<>(ctorParams.size());
        for (int idx = 0; idx < ctorParams.size(); ++idx)
        {
            final ParameterDescription ctorParam = ctorParams.get(idx);
            if (!ctorParam.assisted())
            {
                ret.add(new MappedParameter(idx, -1, ctorParam.type(), ctorParam.name(), null));
                continue;
            }

            // Get a list of factory parameters with the same type as the current ctor param.
            final List<ParameterDescription> matches = factoryParams.stream()
                .filter(val -> val.type().equals(ctorParam.type()))
                .collect(Collectors.toList());

            final ParameterDescription match = getMatchingParameter(ctorParam, matches);
            final int factoryIdx = factoryParams.indexOf(match);
            ret.add(new MappedParameter(idx, factoryIdx, match.type(), match.name(), null));
        }

        return ret;
    }

    /**
     * Maps the parameters of the factory method to the target constructor.
     *
     * @param factoryMethod
     *     The factory method used to create new instances of the target class.
     * @param targetCtor
     *     The target constructor of the target class.
     * @return A list of {@link MappedParameter}s.
     */
    static List<MappedParameter> mapParameters(Method factoryMethod, Constructor<?> targetCtor)
    {
        final Parameter[] parameters = targetCtor.getParameters();
        final List<ParameterDescription> factoryParams = getParameterDescriptions(factoryMethod.getParameters());
        final List<ParameterDescription> ctorParams = getParameterDescriptions(parameters);
        return getMappedParameters(factoryParams, ctorParams);
    }

    /**
     * Finds the factory method that is used to create new instances of the target class.
     * <p>
     * It is assumed that there can be only one method abstract / non-default method can exist.
     *
     * @param targetClass
     *     The target class to analyze.
     * @param factoryClass
     *     The factory class to analyze.
     * @return The factory method.
     *
     * @throws NoSuchMethodException
     *     When no factory method could be found that meets the requirements.
     */
    static Method findFactoryMethod(Class<?> targetClass, Class<?> factoryClass)
        throws NoSuchMethodException
    {
        for (final Method method : factoryClass.getDeclaredMethods())
        {
            if (method.isDefault())
                continue;
            if (targetClass.equals(method.getReturnType()))
                return method;
        }
        throw new NoSuchMethodException("Failed to find non-default creation method in factory class: " +
            factoryClass + " with return type: " + targetClass);
    }

    /**
     * Finds the target constructor.
     * <p>
     * This will be the constructor annotated with {@link AssistedInject}.
     *
     * @param targetClass
     *     The class to analyze.
     * @param <T>
     *     The type of the class.
     * @return The target constructor.
     *
     * @throws NoSuchMethodException
     *     When no constructor could be found that meets the requirements.
     */
    static <T> Constructor<T> findTargetCtor(Class<T> targetClass)
        throws NoSuchMethodException
    {
        for (final Constructor<?> ctor : targetClass.getDeclaredConstructors())
        {
            if (!ctor.isAnnotationPresent(AssistedInject.class))
                continue;
            ctor.setAccessible(true);
            //noinspection unchecked
            return (Constructor<T>) ctor;
        }
        throw new NoSuchMethodException(
            "Failed to find constructor annotated with " + AssistedInject.class + " in target class: " + targetClass);
    }

    /**
     * Represents a parameter mapping from the factory method and the mocked objects onto the constructor.
     */
    @Getter
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    static final class MappedParameter
    {
        /**
         * The index of the parameter in the target constructor.
         */
        private final int targetIdx;

        /**
         * The index of the parameter from the input factory method.
         * <p>
         * This will be -1 if the parameter is injected and thus not provided by the factory method.
         */
        private final int factoryIdx;

        /**
         * The type of the parameter.
         */
        private final Class<?> type;

        /**
         * The name of the parameter, as provided by {@link Assisted#value()}.
         */
        private final @Nullable String named;

        /**
         * The value of the parameter.
         * <p>
         * This might be null. For non-mocked parameters (i.e. those with a valid {@link #factoryIdx}), this will always
         * be null.
         */
        @Setter
        private @Nullable Object value;

        /**
         * Gets whether this parameter is injected, i.e. not provided by the factory method.
         *
         * @return True if this parameter is injected, false otherwise.
         */
        public boolean isInjected()
        {
            return factoryIdx == -1;
        }

        /**
         * Gets the hash of {@link #type} and {@link #named}.
         *
         * @return The hash of {@link #type} and {@link #named}.
         */
        public int getNamedTypeHash()
        {
            return getNamedTypeHash(type, named);
        }

        /**
         * Gets the hash of {@link #type} and {@link #named}.
         *
         * @return The hash of {@link #type} and {@link #named}.
         */
        public static int getNamedTypeHash(Class<?> type, @Nullable String named)
        {
            return Objects.hash(type, named);
        }
    }

    record ParameterDescription(Class<?> type, boolean assisted, @Nullable String name)
    {
    }

    /**
     * Represents the answer of a method invocation on the factory class.
     * <p>
     * This will either call the real method or apply a mapper to create a new instance of the target class.
     */
    @AllArgsConstructor
    private static class DefaultAnswer implements Answer<Object>
    {
        private final Function<InvocationOnMock, Object> mapper;
        private final Method targetMethod;

        @Override
        public Object answer(InvocationOnMock invocation)
            throws Throwable
        {
            // If the invoked method is the target (factory) method, then apply the mapper to create a new instance of
            // the target class.
            // Otherwise, call the real method. This ensures that default methods in the factory interface are called,
            // which will eventually call the target method.
            return invocation.getMethod().equals(targetMethod) ?
                mapper.apply(invocation) :
                Mockito.CALLS_REAL_METHODS.answer(invocation);
        }
    }
}
