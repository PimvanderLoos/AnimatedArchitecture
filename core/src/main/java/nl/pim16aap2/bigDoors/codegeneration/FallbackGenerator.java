package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class FallbackGenerator
{
    private final @NotNull String mappingsVersion;

    private final @NotNull Generator entityFallingBlockGenerator;
    private final @NotNull Generator craftFallingBlockGenerator;

    public FallbackGenerator()
    {
        try
        {
            mappingsVersion = getMappingsVersion();
            entityFallingBlockGenerator = new EntityFallingBlockGenerator(mappingsVersion).generate();
            craftFallingBlockGenerator = new CraftFallingBlockGenerator(mappingsVersion).generate();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to generate NMS code! Please contact pim16aap2!", e);
        }
    }

    public Pair<Class<?>, Constructor<?>> getGeneratedEntityClass()
    {
        return getGeneratedClassData(entityFallingBlockGenerator, "EntityFallingBlock");
    }

    public Pair<Class<?>, Constructor<?>> getGeneratedCraftEntityClass()
    {
        return getGeneratedClassData(craftFallingBlockGenerator, "CraftFallingBlock");
    }

    private Pair<Class<?>, Constructor<?>> getGeneratedClassData(@NotNull Generator generator, String name)
    {
        return new Pair<>(Objects.requireNonNull(generator.generatedClass, "Class for " + name + " cannot be null!"),
                          Objects.requireNonNull(generator.getGeneratedConstructor(),
                                                 "Constructor for " + name + " cannot be null!"));
    }

    public String getMappingsVersion()
        throws IllegalAccessException, InvocationTargetException
    {
        final Class<?> classCraftMagicNumbers = ReflectionUtils.findClass(ReflectionUtils.CRAFT_BASE +
                                                                              "util.CraftMagicNumbers");
        final Method methodGetMappingsVersion = ReflectionUtils.getMethod(classCraftMagicNumbers, "getMappingsVersion");
        final Field instanceField = ReflectionUtils.getField(classCraftMagicNumbers, "INSTANCE");
        final Object instance = instanceField.get(null);
        return (String) methodGetMappingsVersion.invoke(instance);
    }
}
