package nl.pim16aap2.animatedarchitecture.core;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.LimitContainer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandDefinition;
import nl.pim16aap2.animatedarchitecture.core.commands.PermissionsStatus;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureID;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgument;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgumentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.functional.CheckedSupplier;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import nl.pim16aap2.testing.TestUtil;
import nl.pim16aap2.testing.reflection.ReflectionUtil;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UnitTestUtil
{
    public static ILocalizer DUMMY_LOCALIZER = new ILocalizer()
    {
        @Override
        public @NotNull String getMessage(@NotNull String key, @Nullable Locale clientLocale, @NotNull Object... args)
        {
            return key + Stream.of(args).map(Objects::toString).collect(Collectors.joining(" "));
        }

        @Override
        public @NotNull List<Locale> getAvailableLocales()
        {
            return List.of();
        }
    };

    public static final PersonalizedLocalizer DUMMY_PERSONALIZED_LOCALIZER =
        new PersonalizedLocalizer(DUMMY_LOCALIZER, null);

    private static final TextArgumentFactory DUMMY_TEXT_ARGUMENT_FACTORY =
        new TextArgumentFactory(ITextComponentFactory.SimpleTextComponentFactory.INSTANCE,
            DUMMY_PERSONALIZED_LOCALIZER);

    private UnitTestUtil()
    {
    }

    public static StructureID newStructureID(long id)
    {
        final StructureID structureID = Mockito.mock();
        when(structureID.getId()).thenReturn(id);
        return structureID;
    }

    public static void initMessageable(IMessageable... messageables)
    {
        final var personalizedLocalizer = initPersonalizedLocalizer();
        for (final var messageable : messageables)
        {
            final ITextFactory textFactory = new ITextFactory()
            {
                @Override
                public Text newText(@Nullable PersonalizedLocalizer personalizedLocalizer)
                {
                    return new Text(ITextComponentFactory.SimpleTextComponentFactory.INSTANCE, personalizedLocalizer);
                }

                @Override
                public Text newText()
                {
                    return newText(personalizedLocalizer);
                }
            };

            when(messageable.getPersonalizedLocalizer()).thenReturn(personalizedLocalizer);
            doReturn(textFactory.newText()).when(messageable).newText();
        }
    }

    public static void setPersonalizedLocalizer(IMessageable... messageables)
    {
        setPersonalizedLocalizer(null, messageables);
    }

    public static void setPersonalizedLocalizer(@Nullable Locale locale, IMessageable... messageables)
    {
        final var personalizedLocalizer = initPersonalizedLocalizer(locale);
        for (final IMessageable messageable : messageables)
            lenient().when(messageable.getPersonalizedLocalizer()).thenReturn(personalizedLocalizer);
    }

    public static PersonalizedLocalizer initPersonalizedLocalizer()
    {
        return initPersonalizedLocalizer(null);
    }

    public static PersonalizedLocalizer initPersonalizedLocalizer(@Nullable Locale locale)
    {
        return new PersonalizedLocalizer(initLocalizer(), locale);
    }

    public static ILocalizer initLocalizer()
    {
        final ILocalizer localizer = mock(ILocalizer.class, Mockito.CALLS_REAL_METHODS);
        lenient()
            .when(localizer.getMessage(anyString(), nullable(Locale.class), ArgumentMatchers.any(Object[].class)))
            .thenAnswer(invocation ->
            {
                String ret = invocation.getArgument(0, String.class);
                for (int idx = 2; idx < invocation.getArguments().length; ++idx)
                    //noinspection StringConcatenationInLoop
                    ret += " " + invocation.getArgument(idx, Object.class);
                return ret;
            });
        return localizer;
    }

    /**
     * Sets the localization key of a structure type to "StructureType".
     *
     * @param structure
     *     The structure to set the localization key in.
     */
    public static void setStructureLocalization(Structure structure)
    {
        final StructureType type = mock();
        when(type.getLocalizationKey()).thenReturn("StructureType");
        when(structure.getType()).thenReturn(type);
    }

    public static IWorld getWorld()
    {
        final IWorld world = Mockito.mock();
        when(world.worldName()).thenReturn(UUID.randomUUID().toString());
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
        final ILocation loc = mock();

        when(loc.getWorld()).thenReturn(world);

        when(loc.getX()).thenReturn(x);
        when(loc.getY()).thenReturn(y);
        when(loc.getZ()).thenReturn(z);

        when(loc.getBlockX()).thenReturn(MathUtil.floor(x));
        when(loc.getBlockY()).thenReturn(MathUtil.floor(y));
        when(loc.getBlockZ()).thenReturn(MathUtil.floor(z));

        when(loc.getPosition()).thenReturn(new Vector3Di(MathUtil.floor(x), MathUtil.floor(y), MathUtil.floor(z)));

        when(loc.getChunk()).thenReturn(new Vector2Di(MathUtil.floor(x) << 4, MathUtil.floor(z) << 4));

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
        final PlayerData playerData = mock();
        when(playerData.getUUID()).thenReturn(UUID.randomUUID());
        when(playerData.getName()).thenReturn(StringUtil.randomString(6));
        return new StructureOwner(structureUid, PermissionLevel.CREATOR, playerData);
    }

    /**
     * Creates a new {@link StructureBuilder} and accompanying Structure factory mocker.
     *
     * @return The result of the creation.
     */
    public static StructureBuilderResult newStructureBuilder()
        throws ClassNotFoundException,
               IllegalAccessException,
               InstantiationException,
               InvocationTargetException,
               NoSuchMethodException
    {
        final Class<?> classStructureFactory = Class.forName(Structure.class.getName() + "$IFactory");

        final AssistedFactoryMocker<?, ?> assistedFactoryMocker =
            new AssistedFactoryMocker<>(Structure.class, classStructureFactory);

        final Constructor<?> ctorStructureBuilder =
            StructureBuilder.class.getDeclaredConstructor(classStructureFactory);

        ctorStructureBuilder.setAccessible(true);

        final var builder =
            (StructureBuilder) ctorStructureBuilder.newInstance(assistedFactoryMocker.getFactory());

        return new StructureBuilderResult(builder, assistedFactoryMocker);
    }

    /**
     * Shortcut for {@link #newStructureBuilder()}.
     *
     * @param structure
     *     The structure to set the property container in.
     * @param properties
     *     The properties to use.
     */
    public static void setPropertyContainerInMockedStructure(Structure structure, Property<?>... properties)
    {
        if (properties == null || properties.length == 0)
            setPropertyContainerInMockedStructure(structure, (List<Property<?>>) null);
        else
            setPropertyContainerInMockedStructure(structure, List.of(properties));
    }

    /**
     * Sets the property container in a mocked structure.
     * <p>
     * If the provided properties are null, it will use the properties from the structure type. See
     * {@link StructureType#getProperties()}.
     * <p>
     * All property-related methods will be mocked to use the property container.
     *
     * @param structure
     *     The structure to set the property container in.
     * @param providedProperties
     *     The properties to use. If null, the properties from the structure type will be used.
     */
    public static void setPropertyContainerInMockedStructure(
        Structure structure,
        @Nullable List<Property<?>> providedProperties)
    {
        final List<Property<?>> properties = providedProperties == null
            ? Objects.requireNonNull(structure.getType().getProperties())
            : providedProperties;

        final PropertyContainer propertyContainer = PropertyContainer.forProperties(properties);

        Mockito.doAnswer(invocation ->
        {
            final PropertyContainer current = invocation.getArgument(0);
            return current.snapshot();
        }).when(structure).getPropertyContainerSnapshot();

        Mockito.doAnswer(invocation ->
        {
            final Property<?> property = invocation.getArgument(0);
            final Object value = invocation.getArgument(1);
            return propertyContainer.setUntypedPropertyValue(property, value);
        }).when(structure).setPropertyValue(Mockito.any(), Mockito.any());

        Mockito.doAnswer(invocation ->
        {
            final Property<?> property = invocation.getArgument(0);
            return propertyContainer.getPropertyValue(property);
        }).when(structure).getPropertyValue(Mockito.any());

        Mockito.doAnswer(invocation ->
        {
            final Property<?> property = invocation.getArgument(0);
            return propertyContainer.hasProperty(property);
        }).when(structure).hasProperty(Mockito.any());

        Mockito.doAnswer(invocation ->
        {
            final Collection<Property<?>> propertiesToCheck = invocation.getArgument(0);
            return propertyContainer.hasProperties(propertiesToCheck);
        }).when(structure).hasProperties(Mockito.anyCollection());
    }

    /**
     * Creates a new {@link StructureSnapshot} for a given {@link Structure}.
     * <p>
     * If the provided structure is not a mock, it will return the result of {@link Structure#getSnapshot()}.
     * <p>
     * If the provided structure is a mock, it will return a mocked {@link StructureSnapshot} that uses as much data as
     * possible from the provided structure.
     *
     * @param structure
     *     The structure to create a snapshot for.
     * @return The created snapshot.
     */
    public static StructureSnapshot createStructureSnapshotForStructure(Structure structure)
    {
        if (!mockingDetails(structure).isMock())
            return structure.getSnapshot();

        final var ret = mock(StructureSnapshot.class, Mockito.CALLS_REAL_METHODS);
        final var random = ThreadLocalRandom.current();

        final long uid = safeSupplierSimple(random.nextLong(), structure::getUid);

        final Vector3Di min = safeSupplier(
            () -> new Vector3Di(random.nextInt(), random.nextInt(), random.nextInt()), structure::getMinimum);

        final Vector3Di max = safeSupplier(() -> min.multiply(2).absolute(), structure::getMaximum);

        final StructureOwner primeOwner = safeSupplier(
            () ->
            {
                final var playerData = mock(PlayerData.class);
                when(playerData.getUUID()).thenReturn(UUID.randomUUID());
                when(playerData.getName()).thenReturn(StringUtil.randomString(6));
                return new StructureOwner(uid, PermissionLevel.CREATOR, playerData);
            },
            structure::getPrimeOwner
        );

        when(ret.getUid()).thenReturn(uid);

        when(ret.getWorld()).thenReturn(safeSupplier(UnitTestUtil::getWorld, structure::getWorld));

        when(ret.getCuboid()).thenReturn(safeSupplier(() -> new Cuboid(min, max), structure::getCuboid));

        doReturn(safeSupplier(() -> ret.getCuboid().asFlatRectangle(), structure::getAnimationRange))
            .when(ret).getAnimationRange();

        when(ret
            .getPowerBlock())
            .thenReturn(safeSupplier(
                () -> new Vector3Di(random.nextInt(), random.nextInt(), random.nextInt()),
                structure::getPowerBlock)
            );

        when(ret.getName()).thenReturn(safeSupplierSimple("TestStructureSnapshot", structure::getName));

        when(ret.getType())
            .thenReturn(safeSupplier(() -> Mockito.mock(StructureType.class), structure::getType));

        when(ret
            .getPropertyContainerSnapshot())
            .thenReturn(safeSupplier(
                () -> PropertyContainer.forType(Objects.requireNonNull(structure.getType())),
                structure::getPropertyContainerSnapshot)
            );

        Mockito
            .when(ret.getOpenDirection())
            .thenReturn(safeSupplierSimple(MovementDirection.NONE, structure::getOpenDirection));

        when(ret.isLocked()).thenReturn(safeSupplierSimple(false, structure::isLocked));

        when(ret.getPrimeOwner()).thenReturn(primeOwner);

        // We only need to mock the 'getOwnersMap' method, as all other owner-related methods call it.
        when(ret
            .getOwnersMap())
            .thenReturn(safeSupplier(
                () -> Map.of(primeOwner.playerData().getUUID(), primeOwner),
                () -> structure
                    .getOwners()
                    .stream()
                    .collect(Collectors.toMap(owner -> owner.playerData().getUUID(), owner -> owner)))
            );

        return ret;
    }

    /**
     * Creates a new instance of a mocked {@link IPlayerFactory}.
     * <p>
     * All methods will return mocked {@link IPlayer} instances using as much of the provided data as possible.
     *
     * @return The mocked player factory instance.
     */
    public static IPlayerFactory createPlayerFactory()
    {
        final IPlayerFactory playerFactory = Mockito.mock(IPlayerFactory.class);
        when(playerFactory.
            create(any(UUID.class)))
            .thenAnswer(invocation -> createPlayer(invocation.getArgument(0, UUID.class)));
        when(playerFactory
            .create(any(PlayerData.class)))
            .thenAnswer(invocation -> createPlayer(invocation.getArgument(0, PlayerData.class)));
        return playerFactory;
    }

    /**
     * Creates a new instance of a mocked {@link IPlayer} with the provided data.
     *
     * @param data
     *     The data to use for the player.
     * @return The mocked player instance.
     */
    public static IPlayer createPlayer(PlayerData data)
    {
        return createPlayer(
            data.getUUID(),
            data.getName(),
            LimitContainer.of(data),
            null,
            data.isOp(),
            data.hasProtectionBypassPermission()
        );
    }

    /**
     * Creates a new instance of a mocked {@link IPlayer} with mostly random data.
     *
     * @param uuid
     *     The UUID of the player.
     * @return The mocked player instance.
     */
    public static IPlayer createPlayer(UUID uuid)
    {
        return createPlayer(uuid, null, null, null, false, false);
    }

    /**
     * Creates a new instance of a mocked {@link IPlayer} with the provided data.
     *
     * @param uuid
     *     The UUID of the player. If null, a random UUID will be generated.
     * @param name
     *     The name of the player. If null, the UUID will be used as the name.
     * @param limits
     *     The limits of the player. If null, the limits will be set to the maximum value.
     * @param location
     *     The location of the player. If null, getting the location will return an empty optional.
     * @param isOp
     *     Whether the player is an OP or not.
     * @param hasProtectionBypassPermission
     *     Whether the player has a protection bypass permission or not.
     * @return The mocked player instance.
     */
    public static IPlayer createPlayer(
        @Nullable UUID uuid,
        @Nullable String name,
        @Nullable LimitContainer limits,
        @Nullable ILocation location,
        boolean isOp,
        boolean hasProtectionBypassPermission
    )
    {
        final UUID uuid1 = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        final String name1 = Objects.requireNonNullElseGet(name, uuid1::toString);
        final LimitContainer limits1 =
            Objects.requireNonNullElseGet(
                limits,
                () -> new LimitContainer(
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE
                ));

        final IPlayer player = mock();
        when(player.getUUID()).thenReturn(uuid1);
        when(player.getName()).thenReturn(name1);
        when(player.getLimit(any())).thenAnswer(invocation -> limits1.getLimit(invocation.getArgument(0)));
        when(player.isOp()).thenReturn(isOp);
        when(player.hasProtectionBypassPermission()).thenReturn(hasProtectionBypassPermission);
        when(player.hasPermission(anyString())).thenReturn(CompletableFuture.completedFuture(true));
        when(player
            .hasPermission(any(CommandDefinition.class)))
            .thenReturn(CompletableFuture.completedFuture(new PermissionsStatus(true, true)));
        when(player.getLocation()).thenReturn(Optional.ofNullable(location));
        return player;
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
    @SuppressWarnings("EmptyCatch")
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
    @SuppressWarnings("EmptyCatch")
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
            assertTrue(opt.isEmpty());
            return null;
        }
        assertTrue(opt.isPresent());
        assertEquals(obj, opt.get());
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
            assertTrue(opt.isEmpty());
            return null;
        }
        assertTrue(opt.isPresent());

        assertEquals(obj, mapper.apply(opt.get()));
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
            while (rte.getCause() != null && rte.getCause().getClass() == RuntimeException.class)
                rte = (RuntimeException) rte.getCause();

        final var rootCause = rte.getCause();
        assertNotNull(rootCause);
        assertEquals(expectedType, rootCause.getClass(), expectedType.toString());
    }

    /**
     * Asserts that an executable throws an exception whose root cause is of a specific type.
     * <p>
     * This method simply calls {@link Assertions#assertThrows(Class, Executable)} and then digs through the exception
     * until it finds the root cause. It then asserts that the root cause is an instance of the expected type.
     *
     * @param expectedType
     *     The expected type of the root cause.
     * @param executable
     *     The executable to execute.
     * @param <T>
     *     The type of the expected root cause.
     * @return The root cause of the exception if it matches the expected type.
     */
    public static <T extends Throwable> T assertRootCause(Class<T> expectedType, Executable executable)
    {
        Throwable cause = Assertions.assertThrows(Throwable.class, executable);
        while (cause.getCause() != null)
            cause = cause.getCause();
        if (!expectedType.isAssignableFrom(cause.getClass()))
            Assertions.fail(getThrowableMisMatchMessage(expectedType, cause));

        return expectedType.cast(cause);
    }

    /**
     * Returns a message that indicates a mismatch between the expected and actual types of a throwable. This message
     * will contain the expected type and the actual value as well as the stack trace of the actual value.
     *
     * @param expectedType
     *     The type that the actual value should be.
     * @param actualValue
     *     The actual value.
     * @param <T>
     *     The type of the expected value.
     * @return The failure message.
     */
    private static <T extends Throwable> String getThrowableMisMatchMessage(
        Class<T> expectedType,
        Throwable actualValue)
    {
        return String.format("""
                Unexpected type of exception thrown:
                Expected:    %s
                Actual:      %s
                Stack trace:
                %s
                """,
            expectedType.getName(),
            actualValue.getClass().getName(),
            TestUtil.throwableToString(actualValue)
        );
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
    private static Text textArgumentMatcher(String string)
    {
        return argThat(new TextArgumentMatcher(string));
    }

    /**
     * Verifies that no messages were sent to a messageable.
     * <p>
     * This method uses Mockito's {@link Mockito#verify(Object)} and {@link Mockito#never()} to verify that no messages
     * were sent to the messageable.
     * <p>
     * It checks for the following methods:
     * <ul>
     *     <li>{@link IMessageable#sendMessage(Text)}</li>
     *     <li>{@link IMessageable#sendMessage(TextType, String, Text.ArgumentCreator...)}</li>
     *     <li>{@link IMessageable#sendError(String, Text.ArgumentCreator...)}</li>
     *     <li>{@link IMessageable#sendSuccess(String, Text.ArgumentCreator...)}</li>
     *     <li>{@link IMessageable#sendInfo(String, Text.ArgumentCreator...)} </li>
     * </ul>
     *
     * @param messageable
     *     The messageable to verify.
     */
    public static void verifyNoMessagesSent(IMessageable messageable)
    {
        verify(messageable, never()).sendMessage(any(Text.class));
        verify(messageable, never()).sendMessage(any(TextType.class), anyString(), any(Text.ArgumentCreator.class));
        verify(messageable, never()).sendError(anyString(), any(Text.ArgumentCreator.class));
        verify(messageable, never()).sendSuccess(anyString(), any(Text.ArgumentCreator.class));
        verify(messageable, never()).sendInfo(anyString(), any(Text.ArgumentCreator.class));
    }

    private static void assertArgumentsEqual(
        Text.ArgumentCreator[] actualCreators,
        String methodName,
        String message,
        Object[] expectedArgs)
    {
        if (actualCreators.length != expectedArgs.length)
        {
            throw new AssertionError(String.format(
                "Expected %d arguments but got %d arguments for message key '%s'",
                expectedArgs.length, actualCreators.length, message));
        }

        final Object[] actualValues = new Object[actualCreators.length];
        for (int i = 0; i < actualCreators.length; i++)
        {
            TextArgument textArg = actualCreators[i].create(DUMMY_TEXT_ARGUMENT_FACTORY);
            actualValues[i] = textArg.argument();
        }

        org.assertj.core.api.Assertions.assertThat(actualValues)
            .withFailMessage(() -> String.format("""
                    
                    Expected IMessageable.%s('%s') to be called with arguments:
                      [%s]
                    But got:
                      [%s]
                    """,
                methodName,
                message,
                Stream.of(expectedArgs).map(Objects::toString).collect(Collectors.joining(", ")),
                Stream.of(actualValues).map(Objects::toString).collect(Collectors.joining(", "))
            ))
            .containsExactly(expectedArgs);
    }

    /**
     * Creates a new {@link MessageableAssert} for the given {@link IMessageable}.
     *
     * @param messageable
     *     The {@link IMessageable} to create the assert for.
     * @return The {@link MessageableAssert} for the given {@link IMessageable}.
     */
    public static MessageableAssert assertThatMessageable(IMessageable messageable)
    {
        return new MessageableAssert(messageable);
    }

    /**
     * Creates a new {@link MessageableAssert} for the given {@link IMessageable}. AssertJ standard naming convention.
     */
    public static MessageableAssert assertThat(IMessageable messageable)
    {
        return MessageableAssert.assertThat(messageable);
    }

    /**
     * Assert that a message was sent to a messageable.
     */
    public static class MessageableAssert extends AbstractAssert<MessageableAssert, IMessageable>
    {
        private final IMessageable messageable;

        private MessageableAssert(IMessageable messageable)
        {
            super(messageable, MessageableAssert.class);
            this.messageable = messageable;
        }

        /**
         * Static factory method for AssertJ standard pattern.
         *
         * @param messageable
         *     The messageable to assert on.
         * @return The {@link MessageableAssert} for the given messageable.z
         */
        public static MessageableAssert assertThat(IMessageable messageable)
        {
            return new MessageableAssert(messageable);
        }

        /**
         * Asserts that a success message was sent to the messageable.
         *
         * @param message
         *     The message to check for.
         * @return The {@link MessageAssert} for the given messageable.
         *
         * @throws AssertionError
         *     If the messageable did not receive the success message.
         */
        public MessageAssert sentSuccessMessage(String message)
        {
            return new MessageAssert("sendSuccess", messageable, message);
        }

        /**
         * Asserts that an error message was sent to the messageable.
         *
         * @param message
         *     The message to check for.
         * @return The {@link MessageAssert} for the given messageable.
         *
         * @throws AssertionError
         *     If the messageable did not receive the error message.
         */
        public MessageAssert sentErrorMessage(String message)
        {
            return new MessageAssert("sendError", messageable, message);
        }

        /**
         * Asserts that an info message was sent to the messageable.
         *
         * @param message
         *     The message to check for.
         * @return The {@link MessageAssert} for the given messageable.
         *
         * @throws AssertionError
         *     If the messageable did not receive the info message.
         */
        public MessageAssert sentInfoMessage(String message)
        {
            return new MessageAssert("sendInfo", messageable, message);
        }

        /**
         * Gets the list of messages sent to the messageable using {@link IMessageable#sendMessage(Text)}.
         *
         * @return A new {@link ListAssert} containing the messages sent to the messageable.
         */
        public ListAssert<String> extractSentTextMessages()
        {
            final List<String> messages = mockingDetails(messageable)
                .getInvocations()
                .stream()
                .filter(invocation -> invocation.getArguments().length == 1)
                .filter(invocation -> "sendMessage".equals(invocation.getMethod().getName()))
                .map(invocation -> invocation.getArgument(0, Text.class).toString())
                .toList();
            return new ListAssert<>(messages);
        }

        /**
         * Asserts that a message was sent to the messageable.
         *
         * @param text
         *     The text to check for.
         * @throws AssertionError
         *     If the messageable did not receive the message.
         */
        public void sentMessage(Text text)
        {
            verify(messageable).sendMessage(text);
        }

        /**
         * Asserts that a message was sent to the messageable using {@link IMessageable#sendMessage(Text)}.
         *
         * @param text
         *     The String representation of the text to check for.
         * @throws AssertionError
         *     If the messageable did not receive the message.
         */
        public void sentMessage(String text)
        {
            verify(messageable).sendMessage(textArgumentMatcher(text));
        }
    }

    /**
     * Assert that a message was sent to a messageable.
     */
    public static class MessageAssert
    {
        private final String methodName;
        private final String message;
        private final Text.ArgumentCreator[] actualCreators;

        private MessageAssert(String methodName, IMessageable messageable, String message)
        {
            this.methodName = methodName;
            this.message = message;

            final var captor = ArgumentCaptor.forClass(Text.ArgumentCreator[].class);

            switch (methodName)
            {
                case "sendSuccess" -> verify(messageable).sendSuccess(eq(message), captor.capture());
                case "sendError" -> verify(messageable).sendError(eq(message), captor.capture());
                case "sendInfo" -> verify(messageable).sendInfo(eq(message), captor.capture());
                default -> throw new IllegalArgumentException("Unknown method: " + methodName);
            }

            this.actualCreators = captor.getValue();
        }

        /**
         * Asserts that the message was sent with the given arguments.
         *
         * @param expectedArgs
         *     The expected arguments.
         * @throws AssertionError
         *     If the message was not sent with the expected arguments.
         */
        public void withArgs(Object... expectedArgs)
        {
            assertArgumentsEqual(
                actualCreators,
                methodName,
                message,
                expectedArgs
            );
        }

        /**
         * Gets the actual arguments that were passed to the messageable.
         *
         * @return The actual arguments that were passed to the messageable.
         */
        public Object[] getActualArguments()
        {
            return Stream.of(actualCreators)
                .map(creator -> creator.create(DUMMY_TEXT_ARGUMENT_FACTORY).argument())
                .toArray();
        }
    }

    /**
     * The result of creating a new {@link StructureBuilder}.
     *
     * @param structureBuilder
     *     The builder that was created.
     * @param assistedFactoryMocker
     *     The mocker for the factory. Use {@link AssistedFactoryMocker#injectParameter(Class, Object)} to set its
     *     parameters.
     */
    public record StructureBuilderResult(
        StructureBuilder structureBuilder,
        AssistedFactoryMocker<?, ?> assistedFactoryMocker)
    {}

    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
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
