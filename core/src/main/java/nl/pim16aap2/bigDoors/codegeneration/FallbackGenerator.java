package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FallbackGenerator
{
    private final @NotNull String mappingsVersion;

    private final Class<?> classCustomEntityFallingBlock;
    private final EntityFallingBlockGenerator entityFallingBlockGenerator;

    public FallbackGenerator()
        throws Exception
    {
        mappingsVersion = getMappingsVersion();
        entityFallingBlockGenerator = new EntityFallingBlockGenerator(mappingsVersion).generate();
        classCustomEntityFallingBlock = entityFallingBlockGenerator.getGeneratedClass();
    }

    public Pair<Class<?>, Constructor<?>> getGeneratedEntityClass()
    {
        return new Pair<>(entityFallingBlockGenerator.getGeneratedClass(),
                          entityFallingBlockGenerator.getGeneratedConstructor());
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
