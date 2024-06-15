package nl.pim16aap2.animatedarchitecture.spigot.core.hooks;

import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IFakePlayer;
import nl.pim16aap2.util.codegeneration.ClassGenerator;
import nl.pim16aap2.util.reflection.MethodFinder;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.Metadatable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.implementation.MethodCall.construct;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static nl.pim16aap2.util.reflection.ReflectionBuilder.findConstructor;
import static nl.pim16aap2.util.reflection.ReflectionBuilder.findMethod;

/**
 * Class used to generate a class that implements {@link Player} and can be used to create a fake-online player who is
 * actually offline.
 */
public final class FakePlayerClassGenerator extends ClassGenerator
{
    private final Class<?>[] constructorParameterTypes = new Class<?>[]{OfflinePlayer.class, Location.class};
    private final String fieldLocation = "location";
    private final String fieldOfflinePlayer = "offlinePlayer";

    private final @Nullable Path outputDir;

    /**
     * Creates a new {@link FakePlayerClassGenerator}.
     *
     * @param plugin
     *     The plugin to use.
     * @throws Exception
     *     If an error occurs while generating the class.
     */
    FakePlayerClassGenerator(AnimatedArchitecturePlugin plugin, @Nullable Path outputDir)
        throws Exception
    {
        super(plugin.getPluginClassLoader());
        this.outputDir = outputDir;
        generate();
    }

    /**
     * Adds all the fields to the generated class.
     *
     * @param currentBuilder
     *     The builder to add the fields to.
     * @return The builder with the added fields.
     */
    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> currentBuilder)
    {
        var builder = currentBuilder
            .defineField(fieldOfflinePlayer, OfflinePlayer.class, Visibility.PRIVATE, FieldManifestation.FINAL);
        builder = builder.defineField(fieldLocation, Location.class, Visibility.PRIVATE, FieldManifestation.FINAL);
        return builder;
    }

    /**
     * Adds the constructor to the generated class.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @return The builder with the added constructor.
     */
    private DynamicType.Builder<?> addCtor(DynamicType.Builder<?> currentBuilder)
        throws NoSuchMethodException
    {
        return currentBuilder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(getConstructorArgumentTypes())
            .intercept(invoke(Object.class.getConstructor()).andThen(
                FieldAccessor.ofField(fieldOfflinePlayer).setsArgumentAt(0)).andThen(
                FieldAccessor.ofField(fieldLocation).setsArgumentAt(1))
            );
    }

    /**
     * Adds all methods to the generated class.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @param methods
     *     The remaining methods that still need to be added.
     *     <p>
     *     Every method that is added is removed from this map.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> addMethods(DynamicType.Builder<?> currentBuilder, Map<String, Method> methods)
    {
        var builder = currentBuilder;

        builder = interceptMethodWithImplementation(
            builder,
            methods,
            FixedValue.value(new ArrayList<>(0)),
            findMethod(Metadatable.class).withName("getMetadata").get()
        );

        builder = interceptMethodWithImplementation(
            builder,
            methods,
            FixedValue.self(),
            findMethod(OfflinePlayer.class).withName("getPlayer").get()
        );

        builder = interceptMethodWithImplementation(
            builder,
            methods,
            FixedValue.value(true),
            findMethod(OfflinePlayer.class).withName("isOnline").get()
        );

        builder = interceptMethodWithImplementation(
            builder,
            methods,
            FixedValue.value(EntityType.PLAYER),
            findMethod(Entity.class).withName("getType").get()
        );

        builder = interceptMethodRedirectToOfflinePlayer(builder, methods, "getDisplayName", "getName");
        builder = interceptMethodRedirectToOfflinePlayer(builder, methods, "getPlayerListName", "getName");

        builder = addMethodGetWorld(builder, methods);
        builder = addMethodsGetLocation(builder, methods);
        builder = addMethodsOfFakePlayer(builder);
        builder = addOfflinePlayerMethods(builder, methods);
        builder = addMethodsOfObject(builder);
        return builder;
    }

    /**
     * Adds all methods from {@link OfflinePlayer} to the generated class.
     * <p>
     * Every method is intercepted and calls the corresponding method on the {@link OfflinePlayer} field.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @param remainingMethods
     *     The remaining methods that still need to be added.
     *     <p>
     *     Every method that is added is removed from this map.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> addOfflinePlayerMethods(
        DynamicType.Builder<?> currentBuilder,
        Map<String, Method> remainingMethods)
    {
        var builder = currentBuilder;
        final Map<String, Method> offlinePlayerMethods = getMethods(OfflinePlayer.class);
        for (final Map.Entry<String, Method> entry : offlinePlayerMethods.entrySet())
        {
            if (remainingMethods.remove(entry.getKey()) == null)
                continue;
            final Implementation impl = invoke(entry.getValue()).onField(fieldOfflinePlayer).withAllArguments();
            builder = builder.define(entry.getValue()).intercept(impl);
        }
        return builder;
    }

    /**
     * Adds a method to the generated class.
     * <p>
     * The generated method returns the provided implementation.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @param remainingMethods
     *     The remaining methods that still need to be added.
     *     <p>
     *     Every method that is added is removed from this map.
     * @param implementation
     *     The implementation to provide for the generated method.
     * @param method
     *     The method to intercept and provide an implementation for.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> interceptMethodWithImplementation(
        DynamicType.Builder<?> currentBuilder,
        Map<String, Method> remainingMethods,
        Implementation implementation,
        Method method)
    {
        if (remainingMethods.remove(simpleMethodString(method)) == null)
            throw new IllegalStateException("Failed to find mapped method: " + method);

        return currentBuilder.define(method).intercept(implementation);
    }

    /**
     * Adds a method to the generated class.
     * <p>
     * The generated method calls a method on the {@link OfflinePlayer} field.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @param remainingMethods
     *     The remaining methods that still need to be added.
     *     <p>
     *     Every method that is added is removed from this map.
     * @param methodName
     *     The name of the method to add.
     * @param targetMethodName
     *     The name of the method to call on the {@link OfflinePlayer} field.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> interceptMethodRedirectToOfflinePlayer(
        DynamicType.Builder<?> currentBuilder,
        Map<String, Method> remainingMethods,
        String methodName,
        String targetMethodName)
    {
        final Method method = findMethod(Player.class).withName(methodName).checkInterfaces().get();
        final Method target = findMethod(OfflinePlayer.class).withName(targetMethodName).get();

        if (remainingMethods.remove(simpleMethodString(method)) == null)
            throw new IllegalStateException("Failed to find mapped method: " + method);

        return currentBuilder.define(method).intercept(invoke(target).onField(fieldOfflinePlayer));
    }

    /**
     * Adds the {@link Player#getLocation()} and the {@link Player#getLocation(Location)} methods to the generated
     * class.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @param remainingMethods
     *     The remaining methods that still need to be added.
     *     <p>
     *     Every method that is added is removed from this map.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> addMethodsGetLocation(
        DynamicType.Builder<?> currentBuilder, Map<String, Method> remainingMethods)
    {
        final Constructor<?> locCtor = findConstructor(Location.class)
            .withParameters(World.class, double.class, double.class, double.class).get();

        final Method method0 = findMethod(Player.class)
            .withName("getLocation")
            .withoutParameters()
            .checkInterfaces()
            .get();

        final Method method1 = findMethod(Player.class)
            .withName("getLocation")
            .withParameters(Location.class)
            .checkInterfaces()
            .get();

        if (remainingMethods.remove(simpleMethodString(method0)) == null)
            throw new IllegalStateException("Failed to find mapped method: " + method0);
        if (remainingMethods.remove(simpleMethodString(method1)) == null)
            throw new IllegalStateException("Failed to find mapped method: " + method1);

        final MethodFinder.MethodFinderInSource findLocationMethod = findMethod().inClass(Location.class);
        final MethodCall getWorld = invoke(findLocationMethod.withName("getWorld").get()).onField(fieldLocation);
        final MethodCall getX = invoke(findLocationMethod.withName("getX").get()).onField(fieldLocation);
        final MethodCall getY = invoke(findLocationMethod.withName("getY").get()).onField(fieldLocation);
        final MethodCall getZ = invoke(findLocationMethod.withName("getZ").get()).onField(fieldLocation);

        // Add Player#getLocation() method.
        var builder = currentBuilder
            .define(method0)
            .intercept(construct(locCtor)
                .withMethodCall(getWorld)
                .withMethodCall(getX)
                .withMethodCall(getY)
                .withMethodCall(getZ));

        final MethodCall setWorld = invoke(findLocationMethod.withName("setWorld").get()).onArgument(0);
        final MethodCall setX = invoke(findLocationMethod.withName("setX").get()).onArgument(0);
        final MethodCall setY = invoke(findLocationMethod.withName("setY").get()).onArgument(0);
        final MethodCall setZ = invoke(findLocationMethod.withName("setZ").get()).onArgument(0);
        final MethodCall setYaw = invoke(findLocationMethod.withName("setYaw").get()).onArgument(0);
        final MethodCall setPitch = invoke(findLocationMethod.withName("setPitch").get()).onArgument(0);

        // Add Player#getLocation(Location) method.
        builder = builder
            .define(method1)
            .intercept(
                setWorld.withMethodCall(getWorld)
                    .andThen(setX.withMethodCall(getX))
                    .andThen(setY.withMethodCall(getY))
                    .andThen(setZ.withMethodCall(getZ))
                    .andThen(setYaw.with(0F))
                    .andThen(setPitch.with(0F))
                    .andThen(FixedValue.argument(0))
            );
        return builder;
    }

    private static String simpleMethodString(Method m)
    {
        String ret = m.getName();
        for (final Class<?> clz : m.getParameterTypes())
            //noinspection StringConcatenationInLoop
            ret += clz.getName();
        return ret;
    }

    /**
     * Gets all methods of a class.
     * <p>
     * The returned map contains the result of {@link #simpleMethodString(Method)} as key and the method as value.
     *
     * @param clz
     *     The class to get the methods from.
     * @return A map of all methods of the class.
     */
    private static Map<String, Method> getMethods(Class<?> clz)
    {
        final Method[] methodsArr = clz.getMethods();
        int filteredCount = 0;
        for (int idx = 0; idx < methodsArr.length; ++idx)
        {
            final Method method = methodsArr[idx];
            if (method.isDefault())
            {
                ++filteredCount;
                continue;
            }
            methodsArr[idx - filteredCount] = method;
        }

        final Map<String, Method> methods = new HashMap<>(methodsArr.length - filteredCount);
        for (int idx = 0; idx < (methodsArr.length - filteredCount); ++idx)
        {
            final Method method = methodsArr[idx];
            methods.put(simpleMethodString(method), method);
        }

        return methods;
    }

    /**
     * Adds the {@link Player#getWorld()} method to the generated class.
     *
     * @param currentBuilder
     *     The builder to add the method to.
     * @param methods
     *     The remaining methods that still need to be added.
     *     <p>
     *     Every method that is added is removed from this map.
     * @return The builder with the added method.
     */
    private DynamicType.Builder<?> addMethodGetWorld(DynamicType.Builder<?> currentBuilder, Map<String, Method> methods)
    {
        final Method method = findMethod(Player.class).withName("getWorld").checkInterfaces().get();
        final Method target = findMethod(Location.class).withName("getWorld").get();
        if (methods.remove(simpleMethodString(method)) == null)
            throw new IllegalStateException("Failed to find mapped method: " + method);

        return currentBuilder.define(method).intercept(invoke(target).onField(fieldLocation));
    }

    /**
     * Adds stubs for all methods that have not been added yet.
     *
     * @param currentBuilder
     *     The builder to add the method to.
     * @param methods
     *     The remaining methods that still need to be added.
     *     <p>
     *     A stub will be generated for each method in this map.
     * @return The builder with the added method.
     */
    private DynamicType.Builder<?> addStubs(DynamicType.Builder<?> currentBuilder, Map<String, Method> methods)
    {
        var builder = currentBuilder;
        for (final Method method : methods.values())
        {
            if (method.isDefault())
                continue;
            builder = builder.define(method).intercept(StubMethod.INSTANCE);
        }
        return builder;
    }

    /**
     * Adds the {@link IFakePlayer#getOfflinePlayer0()} and {@link IFakePlayer#getLocation0()} methods.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> addMethodsOfFakePlayer(DynamicType.Builder<?> currentBuilder)
    {
        final MethodFinder.MethodFinderInSource findMethod = findMethod().inClass(IFakePlayer.class);
        final Method getPlayer = findMethod.withName("getOfflinePlayer0").get();
        final Method getLocation = findMethod.withName("getLocation0").get();

        var builder = currentBuilder.define(getPlayer).intercept(FieldAccessor.ofField(fieldOfflinePlayer));
        builder = builder.define(getLocation).intercept(FieldAccessor.ofField(fieldLocation));
        return builder;
    }

    /**
     * Adds the {@link Object#equals(Object)}, {@link Object#hashCode()} and {@link Object#toString()} methods.
     *
     * @param currentBuilder
     *     The builder to add the methods to.
     * @return The builder with the added methods.
     */
    private DynamicType.Builder<?> addMethodsOfObject(DynamicType.Builder<?> currentBuilder)
    {
        final MethodFinder.MethodFinderInSource findObjectMethod = findMethod().inClass(Object.class);
        final Method equals = findObjectMethod.withName("equals").withParameters(Object.class).get();
        final Method hashCode = findObjectMethod.withName("hashCode").get();
        final Method toString = findObjectMethod.withName("toString").get();

        final MethodFinder.MethodFinderInSource findFakePlayerMethod = findMethod().inClass(IFakePlayer.class);
        final Method equals0 = findFakePlayerMethod.withName("equals0").get();
        final Method hashCode0 = findFakePlayerMethod.withName("hashCode0").get();
        final Method toString0 = findFakePlayerMethod.withName("toString0").get();

        var builder = currentBuilder.define(equals).intercept(invoke(equals0).withAllArguments());
        builder = builder.define(hashCode).intercept(invoke(hashCode0).withAllArguments());
        builder = builder.define(toString).intercept(invoke(toString0).withAllArguments());

        return builder;
    }

    @Override
    protected void generateImpl()
        throws Exception
    {
        final Map<String, Method> methods = getMethods(Player.class);
        DynamicType.Builder<?> builder = createBuilder(Player.class).implement(IFakePlayer.class);

        builder = addFields(builder);
        builder = addCtor(builder);
        builder = addMethods(builder, methods);
        builder = addStubs(builder, methods);

        finishBuilder(builder, outputDir);
    }

    @Override
    protected String getBaseName()
    {
        return "FakePlayer";
    }

    @Override
    protected Class<?>[] getConstructorArgumentTypes()
    {
        return Arrays.copyOf(constructorParameterTypes, constructorParameterTypes.length);
    }
}
