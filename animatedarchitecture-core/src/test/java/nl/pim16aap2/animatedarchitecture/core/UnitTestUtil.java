package nl.pim16aap2.animatedarchitecture.core;

import lombok.AllArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyManager;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.functional.CheckedSupplier;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import nl.pim16aap2.testing.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UnitTestUtil
{
    @SuppressWarnings("unused")
    public static final double EPSILON = 1E-6;

    public static ILocalizer initLocalizer()
    {
        final ILocalizer localizer = Mockito.mock(ILocalizer.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(localizer.getMessage(Mockito.anyString(), ArgumentMatchers.any(Object[].class)))
            .thenAnswer(invocation ->
            {
                String ret = invocation.getArgument(0, String.class);
                for (int idx = 1; idx < invocation.getArguments().length; ++idx)
                    //noinspection StringConcatenationInLoop
                    ret += " " + invocation.getArgument(idx, Object.class);
                return ret;
            });
        return localizer;
    }

    public static IWorld getWorld()
    {
        final IWorld world = Mockito.mock(IWorld.class);
        Mockito.when(world.worldName()).thenReturn(UUID.randomUUID().toString());
        return world;
    }

    @SuppressWarnings("unused")
    public static ILocation getLocation(Vector3Dd vec)
    {
        return getLocation(vec.x(), vec.y(), vec.z());
    }

    @SuppressWarnings("unused")
    public static ILocation getLocation(Vector3Di vec)
    {
        return getLocation(vec.x(), vec.y(), vec.z());
    }

    @SuppressWarnings("unused")
    public static ILocation getLocation(Vector3Dd vec, IWorld world)
    {
        return getLocation(vec.x(), vec.y(), vec.z(), world);
    }

    public static ILocation getLocation(Vector3Di vec, IWorld world)
    {
        return getLocation(vec.x(), vec.y(), vec.z(), world);
    }

    public static ILocation getLocation(double x, double y, double z)
    {
        return getLocation(x, y, z, getWorld());
    }

    public static ILocation getLocation(double x, double y, double z, IWorld world)
    {
        final ILocation loc = Mockito.mock(ILocation.class);

        Mockito.when(loc.getWorld()).thenReturn(world);

        Mockito.when(loc.getX()).thenReturn(x);
        Mockito.when(loc.getY()).thenReturn(y);
        Mockito.when(loc.getZ()).thenReturn(z);

        Mockito.when(loc.getBlockX()).thenReturn(MathUtil.floor(x));
        Mockito.when(loc.getBlockY()).thenReturn(MathUtil.floor(y));
        Mockito.when(loc.getBlockZ()).thenReturn(MathUtil.floor(z));

        Mockito.when(loc.getPosition())
            .thenReturn(new Vector3Di(MathUtil.floor(x), MathUtil.floor(y), MathUtil.floor(z)));

        Mockito.when(loc.getChunk()).thenReturn(new Vector2Di(MathUtil.floor(x) << 4, MathUtil.floor(z) << 4));

        return loc;
    }

    /**
     * Creates a new {@link StructureOwner} with random player data.
     *
     * @param structureUid
     *     The UID of the structure.
     * @return The created structure owner.
     */
    public static StructureOwner createStructureOwner(long structureUid)
    {
        final PlayerData playerData = Mockito.mock(PlayerData.class);
        Mockito.when(playerData.getUUID()).thenReturn(UUID.randomUUID());
        Mockito.when(playerData.getName()).thenReturn(StringUtil.randomString(6));
        return new StructureOwner(structureUid, PermissionLevel.CREATOR, playerData);
    }

    /**
     * Creates a new {@link StructureBaseBuilder} and accompanying StructureBase factory.
     *
     * @return The result of the creation.
     */
    public static StructureBaseBuilderResult newStructureBaseBuilder()
        throws ClassNotFoundException,
               IllegalAccessException,
               InstantiationException,
               InvocationTargetException,
               NoSuchMethodException
    {
        final Class<?> classStructureBase = Class.forName(
            "nl.pim16aap2.animatedarchitecture.core.structures.StructureBase");

        final Class<?> classStructureBaseFactory = Class.forName(
            "nl.pim16aap2.animatedarchitecture.core.structures.StructureBase$IFactory");

        final AssistedFactoryMocker<?, ?> assistedFactoryMocker =
            new AssistedFactoryMocker<>(classStructureBase, classStructureBaseFactory);

        final Constructor<?> ctorStructureBaseBuilder =
            StructureBaseBuilder.class.getDeclaredConstructor(classStructureBaseFactory);

        ctorStructureBaseBuilder.setAccessible(true);

        final var builder =
            (StructureBaseBuilder) ctorStructureBaseBuilder.newInstance(assistedFactoryMocker.getFactory());

        return new StructureBaseBuilderResult(builder, assistedFactoryMocker);
    }

    /**
     * Sets the property manager in a mocked structure.
     * <p>
     * If the provided properties are null, it will use the properties from the structure type. See
     * {@link StructureType#getProperties()}.
     * <p>
     * All property-related methods will be mocked to use the property manager.
     *
     * @param structure
     *     The structure to set the property manager in.
     * @param providedProperties
     *     The properties to use. If null, the properties from the structure type will be used.
     */
    public static void setPropertyManagerInMockedStructure(
        AbstractStructure structure,
        @Nullable List<Property<?>> providedProperties)
    {
        final List<Property<?>> properties = providedProperties == null
            ? Objects.requireNonNull(structure.getType().getProperties())
            : providedProperties;

        final PropertyManager propertyManager = PropertyManager.forProperties(properties);

        Mockito.doAnswer(invocation ->
        {
            final PropertyManager current = invocation.getArgument(0);
            return current.snapshot();
        }).when(structure).getPropertyManagerSnapshot();

        Mockito.doAnswer(invocation ->
        {
            final Property<?> property = invocation.getArgument(0);
            final Object value = invocation.getArgument(1);
            return propertyManager.setUntypedPropertyValue(property, value);
        }).when(structure).setPropertyValue(Mockito.any(), Mockito.any());

        Mockito.doAnswer(invocation ->
        {
            final Property<?> property = invocation.getArgument(0);
            return propertyManager.getPropertyValue(property);
        }).when(structure).getPropertyValue(Mockito.any());

        Mockito.doAnswer(invocation ->
        {
            final Property<?> property = invocation.getArgument(0);
            return propertyManager.hasProperty(property);
        }).when(structure).hasProperty(Mockito.any());

        Mockito.doAnswer(invocation ->
        {
            final Collection<Property<?>> propertiesToCheck = invocation.getArgument(0);
            return propertyManager.hasProperties(propertiesToCheck);
        }).when(structure).hasProperties(Mockito.anyCollection());
    }

    /**
     * Creates a new {@link StructureSnapshot} for a given {@link AbstractStructure}.
     * <p>
     * If the provided structure is not a mock, it will return the result of {@link AbstractStructure#getSnapshot()}.
     * <p>
     * If the provided structure is a mock, it will return a mocked {@link StructureSnapshot} that uses as much data as
     * possible from the provided structure.
     *
     * @param structure
     *     The structure to create a snapshot for.
     * @return The created snapshot.
     */
    public static StructureSnapshot createStructureSnapshotForStructure(AbstractStructure structure)
    {
        if (!Mockito.mockingDetails(structure).isMock())
            return structure.getSnapshot();

        final var ret = Mockito.mock(StructureSnapshot.class, Mockito.CALLS_REAL_METHODS);
        final var random = ThreadLocalRandom.current();

        final long uid = safeSupplierSimple(random.nextLong(), structure::getUid);

        final Vector3Di min = safeSupplier(
            () -> new Vector3Di(random.nextInt(), random.nextInt(), random.nextInt()), structure::getMinimum);

        final Vector3Di max = safeSupplier(() -> min.multiply(2).absolute(), structure::getMaximum);

        final StructureOwner primeOwner = safeSupplier(
            () ->
            {
                final var playerData = Mockito.mock(PlayerData.class);
                Mockito.when(playerData.getUUID()).thenReturn(UUID.randomUUID());
                Mockito.when(playerData.getName()).thenReturn(StringUtil.randomString(6));
                return new StructureOwner(uid, PermissionLevel.CREATOR, playerData);
            },
            structure::getPrimeOwner
        );

        Mockito.when(ret.getUid()).thenReturn(uid);

        Mockito.when(ret.getWorld()).thenReturn(safeSupplier(UnitTestUtil::getWorld, structure::getWorld));

        Mockito.when(ret.getCuboid()).thenReturn(safeSupplier(() -> new Cuboid(min, max), structure::getCuboid));

        Mockito.doReturn(safeSupplier(() -> ret.getCuboid().asFlatRectangle(), structure::getAnimationRange))
            .when(ret).getAnimationRange();

        Mockito.when(ret.getPowerBlock()).thenReturn(safeSupplier(
            () -> new Vector3Di(random.nextInt(), random.nextInt(), random.nextInt()),
            structure::getPowerBlock)
        );

        Mockito.when(ret.getName()).thenReturn(safeSupplierSimple("TestStructureSnapshot", structure::getName));

        Mockito.when(ret.getType())
            .thenReturn(safeSupplier(() -> Mockito.mock(StructureType.class), structure::getType));

        Mockito.when(ret.getPropertyManagerSnapshot()).thenReturn(safeSupplier(
            () -> PropertyManager.forType(Objects.requireNonNull(structure.getType())),
            structure::getPropertyManagerSnapshot));

        Mockito.when(ret.getOpenDir()).thenReturn(safeSupplierSimple(MovementDirection.NONE, structure::getOpenDir));

        Mockito.when(ret.isLocked()).thenReturn(safeSupplierSimple(false, structure::isLocked));

        Mockito.when(ret.getPrimeOwner()).thenReturn(primeOwner);

        // We only need to mock the 'getOwnersMap' method, as all other owner-related methods call it.
        Mockito.when(ret.getOwnersMap())
            .thenReturn(safeSupplier(
                () -> Map.of(primeOwner.playerData().getUUID(), primeOwner),
                () -> structure
                    .getOwners()
                    .stream()
                    .collect(Collectors.toMap(owner -> owner.playerData().getUUID(), owner -> owner)))
            );

        final Map<String, Object> propertyMap = safeSupplierSimple(
            Collections.emptyMap(),
            () -> StructureSnapshot.getPersistentVariableMap(structure)
        );

        //noinspection SuspiciousMethodCalls
        Mockito.doAnswer(invocation -> propertyMap.get(invocation.getArgument(0)))
            .when(ret).getPersistentVariable(Mockito.anyString());

        return ret;
    }

    /**
     * Attempts to get a value from a supplier, but returns a fallback value if the supplier throws an exception or
     * returns null.
     *
     * @param fallback
     *     The fallback value to return if the supplier fails.
     * @param supplier
     *     The supplier to get the value from.
     * @param <T>
     *     The type of the value.
     * @return The value from the supplier, or the fallback value if the supplier fails.
     */
    private static <T> T safeSupplierSimple(T fallback, CheckedSupplier<T, ?> supplier)
    {
        try
        {
            final @Nullable T ret = supplier.get();
            if (ret != null)
                return ret;
        }
        catch (Exception ignored)
        {
        }
        return fallback;
    }

    /**
     * Attempts to get a value from a supplier, but returns a fallback value if the supplier throws an exception or
     * returns null.
     * <p>
     * Note that exceptions thrown by the fallback supplier will not be caught.
     *
     * @param fallbackSupplier
     *     The fallback supplier to get the value from if the main supplier fails.
     * @param supplier
     *     The supplier to get the value from.
     * @param <T>
     *     The type of the value.
     * @return The value from the supplier, or the fallback value if the supplier fails.
     */
    private static <T> T safeSupplier(Supplier<T> fallbackSupplier, CheckedSupplier<T, ?> supplier)
    {
        try
        {
            final @Nullable T ret = supplier.get();
            if (ret != null)
                return ret;
        }
        catch (Exception ignored)
        {
        }
        return fallbackSupplier.get();
    }

    /**
     * Checks if an object and an Optional are the same or if they both don't exist/are null.
     *
     * @param obj
     *     The object to compare the optional to.
     * @param opt
     *     The Optional to compare against the object.
     * @param <T>
     *     The type of the Object and Optional.
     * @return The object inside the Optional.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable <T> T optionalEquals(@Nullable T obj, Optional<T> opt)
    {
        if (obj == null)
        {
            Assertions.assertTrue(opt.isEmpty());
            return null;
        }
        Assertions.assertTrue(opt.isPresent());
        Assertions.assertEquals(obj, opt.get());
        return opt.get();
    }

    /**
     * Checks if an object and the mapped value of an Optional are the same or if they both don't exist/are null.
     *
     * @param obj
     *     The object to compare the optional to.
     * @param opt
     *     The Optional to compare against the object.
     * @param mapper
     *     The mapping function to apply to the value inside the optional (if that exists).
     * @param <T>
     *     The type of the Object and the result of the mapping functions.
     * @param <U>
     *     The type of the object stored inside the optional.
     * @return The object inside the Optional (so without the mapping function applied!).
     */
    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable <T, U> U optionalEquals(@Nullable T obj, Optional<U> opt, Function<U, T> mapper)
    {
        if (obj == null)
        {
            Assertions.assertTrue(opt.isEmpty());
            return null;
        }
        Assertions.assertTrue(opt.isPresent());

        Assertions.assertEquals(obj, mapper.apply(opt.get()));
        return opt.get();
    }

    /**
     * Asserts a specific exception wrapped inside a {@link RuntimeException} is thrown by an {@link Executable}.
     *
     * @param expectedType
     *     The {@link Throwable} expected to be thrown wrapped inside a {@link RuntimeException}.
     * @param executable
     *     The {@link Executable} to execute that is expected to throw an exception.
     * @param <T>
     *     The type of the throwable wrapped inside the RuntimeException.
     */
    @SuppressWarnings("unused")
    public static <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable)
    {
        assertWrappedThrows(expectedType, executable, false);
    }

    /**
     * Asserts a specific exception wrapped inside a {@link RuntimeException} is thrown by an {@link Executable}.
     *
     * @param expectedType
     *     The {@link Throwable} expected to be thrown wrapped inside a {@link RuntimeException}.
     * @param executable
     *     The {@link Executable} to execute that is expected to throw an exception.
     * @param deepSearch
     *     Whether to keep digging through any number of layered {@link RuntimeException}s until we find a throwable
     *     that is not a RuntimeException.
     * @param <T>
     *     The type of the throwable wrapped inside the RuntimeException.
     */
    public static <T extends Throwable> void assertWrappedThrows(
        Class<T> expectedType,
        Executable executable,
        boolean deepSearch)
    {
        RuntimeException rte = Assertions.assertThrows(RuntimeException.class, executable);
        if (deepSearch)
            while (rte.getCause().getClass() == RuntimeException.class)
                rte = (RuntimeException) rte.getCause();
        Assertions.assertEquals(expectedType, rte.getCause().getClass(), expectedType.toString());
    }

    /**
     * Sets the field of a class to a value.
     *
     * @param clz
     *     The type in which to look for the field.
     * @param obj
     *     The object whose field to modify.
     * @param fieldName
     *     The name of the field to modify.
     * @param value
     *     The value to set the field to.
     */
    public static void setField(Class<?> clz, Object obj, String fieldName, Object value)
    {
        ReflectionUtil.setField(clz, obj, fieldName, value);
    }

    /**
     * Collects the varargs from an {@link InvocationOnMock}.
     *
     * @param clz
     *     The type of the array to instantiate.
     * @param invocationOnMock
     *     The {@link InvocationOnMock} from which to extract the varargs and put them in a single array.
     * @param offset
     *     The offset for the start of the varargs.
     *     <p>
     *     For example, when capturing the method "fun(int, int, String...)", the varargs would have an offset of 2, as
     *     the index of the first vararg value will be 2.
     * @param <T>
     *     The type of the array elements.
     * @return An array for the given type.
     *
     * @throws IllegalArgumentException
     *     When one of the provided arguments are not of the correct type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayFromCapturedVarArgs(Class<T> clz, InvocationOnMock invocationOnMock, int offset)
    {
        final Object[] args = invocationOnMock.getArguments();
        final int size = Math.max(0, args.length - offset);

        final T[] ret = (T[]) Array.newInstance(clz, size);
        if (size == 0)
            return ret;

        for (int idx = 0; idx < size; ++idx)
        {
            final Object obj = args[idx + offset];
            if (!clz.isAssignableFrom(obj.getClass()))
                throw new IllegalArgumentException(
                    "Object " + obj +
                        " of type " + obj.getClass().getName() +
                        " is not of type " + clz.getName() + "!"
                );
            ret[idx] = (T) obj;
        }
        return ret;
    }

    /**
     * Creates a new argument matcher that matches a Text argument using its {@link Text#toString()} method against an
     * input string.
     * <p>
     * See {@link TextArgumentMatcher} and {@link Mockito#argThat(ArgumentMatcher)}.
     *
     * @param string
     *     The input string.
     * @return null.
     */
    public static Text textArgumentMatcher(String string)
    {
        return Mockito.argThat(new TextArgumentMatcher(string));
    }

    /**
     * The result of creating a new {@link StructureBaseBuilder}.
     *
     * @param structureBaseBuilder
     *     The builder that was created.
     * @param assistedFactoryMocker
     *     The mocker for the factory. Use {@link AssistedFactoryMocker#setMock(Class, Object)} to set its parameters.
     */
    public record StructureBaseBuilderResult(
        StructureBaseBuilder structureBaseBuilder,
        AssistedFactoryMocker<?, ?> assistedFactoryMocker)
    {}

    @AllArgsConstructor
    public static final class TextArgumentMatcher implements ArgumentMatcher<Text>
    {
        private final String base;

        @Override
        public boolean matches(Text argument)
        {
            return base.equals(argument.toString());
        }
    }
}
