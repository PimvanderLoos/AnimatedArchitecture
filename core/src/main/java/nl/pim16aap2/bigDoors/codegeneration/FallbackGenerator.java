package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.classCraftMagicNumbers;

public final class FallbackGenerator
{
    private final @NotNull String mappingsVersion;

    private final @NotNull Generator entityFallingBlockGenerator;
    private final @NotNull Generator craftFallingBlockGenerator;
    private final @NotNull Generator nmsBlockGenerator;
    private final @NotNull Generator FallingBlockFactoryGenerator;

    public FallbackGenerator()
    {
        try
        {
            mappingsVersion = getMappingsVersion();
            entityFallingBlockGenerator = new EntityFallingBlockGenerator(mappingsVersion).generate();
            craftFallingBlockGenerator =
                new CraftFallingBlockGenerator(mappingsVersion, entityFallingBlockGenerator.generatedClass).generate();
            nmsBlockGenerator = new NMSBlockGenerator(mappingsVersion).generate();
            FallingBlockFactoryGenerator = new FallingBlockFactoryGenerator(mappingsVersion,
                                                                            getGeneratedNMSBlockClass(),
                                                                            getGeneratedCraftEntityClass(),
                                                                            getGeneratedEntityClass()).generate();
        }
        catch (Exception | ExceptionInInitializerError e)
        {
            throw new RuntimeException("Failed to generate NMS code! Please contact pim16aap2!", e);
        }
    }

    private @NotNull Pair<Class<?>, Constructor<?>> getGeneratedEntityClass()
    {
        return getGeneratedClassData(entityFallingBlockGenerator, "EntityFallingBlock");
    }

    private @NotNull Pair<Class<?>, Constructor<?>> getGeneratedCraftEntityClass()
    {
        return getGeneratedClassData(craftFallingBlockGenerator, "CraftFallingBlock");
    }

    private @NotNull Pair<Class<?>, Constructor<?>> getGeneratedNMSBlockClass()
    {
        return getGeneratedClassData(nmsBlockGenerator, "NMSBlock");
    }

    private @NotNull Pair<Class<?>, Constructor<?>> getGeneratedFallingBlockFactoryClass()
    {
        return getGeneratedClassData(FallingBlockFactoryGenerator, "FallingBlockFactory");
    }

    private @NotNull Pair<Class<?>, Constructor<?>> getGeneratedClassData(@NotNull Generator generator, String name)
    {
        return new Pair<>(Objects.requireNonNull(generator.getGeneratedClass(),
                                                 "Class for " + name + " cannot be null!"),
                          Objects.requireNonNull(generator.getGeneratedConstructor(),
                                                 "Constructor for " + name + " cannot be null!"));
    }

    private @NotNull String getMappingsVersion()
        throws IllegalAccessException, InvocationTargetException
    {
        final Method methodGetMappingsVersion = ReflectionUtils.getMethod(classCraftMagicNumbers, "getMappingsVersion");
        final Field instanceField = ReflectionUtils.getField(classCraftMagicNumbers, "INSTANCE");
        final Object instance = instanceField.get(null);
        return Objects.requireNonNull((String) methodGetMappingsVersion.invoke(instance),
                                      "Failed to find the current mappings version!");
    }
}
