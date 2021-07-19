package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.util.ReflectionUtils;
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
    private final @NotNull Generator entityFallingBlockGenerator;
    private final @NotNull Generator craftFallingBlockGenerator;
    private final @NotNull Generator nmsBlockGenerator;
    private final @NotNull Generator fallingBlockFactoryGenerator;

    private final @NotNull FallingBlockFactory fallingBlockFactory;

    public FallbackGenerator()
    {
        try
        {
            final String mappingsVersion = getMappingsVersion();
            entityFallingBlockGenerator = new EntityFallingBlockGenerator(mappingsVersion).generate();
            craftFallingBlockGenerator =
                new CraftFallingBlockGenerator(mappingsVersion, entityFallingBlockGenerator.generatedClass).generate();
            nmsBlockGenerator = new NMSBlockGenerator(mappingsVersion).generate();
            fallingBlockFactoryGenerator = new FallingBlockFactoryGenerator(mappingsVersion,
                                                                            getGeneratedNMSBlockClass(),
                                                                            getGeneratedCraftEntityClass(),
                                                                            getGeneratedEntityClass()).generate();
            //noinspection ConstantConditions
            fallingBlockFactory = (FallingBlockFactory) fallingBlockFactoryGenerator.getGeneratedConstructor()
                                                                                    .newInstance();
        }
        catch (Exception | ExceptionInInitializerError e)
        {
            throw new RuntimeException("Failed to generate NMS code! Please contact pim16aap2!", e);
        }
    }

    public @NotNull FallingBlockFactory getFallingBlockFactory()
    {
        return fallingBlockFactory;
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
        return getGeneratedClassData(fallingBlockFactoryGenerator, "FallingBlockFactory");
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
