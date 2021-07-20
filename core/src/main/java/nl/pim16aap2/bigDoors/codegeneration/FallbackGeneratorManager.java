package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
import nl.pim16aap2.bigDoors.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.classCraftMagicNumbers;

/**
 * Represents the manager for the {@link ClassGenerator}s for the NMS/Craft classes that can be generated as a fallback
 * when no compiled classes exist for the current mappings.
 * <p>
 * The generation takes subclasses for {@link CustomCraftFallingBlock}, {@link CustomEntityFallingBlock}, {@link
 * NMSBlock}, and {@link FallingBlockFactory}. Only an instance of the generated {@link FallingBlockFactory} can be
 * accessed directly.
 *
 * @author Pim
 */
public final class FallbackGeneratorManager
{
    private final @NotNull FallingBlockFactory fallingBlockFactory;

    private static final @NotNull Object lck = new Object();
    private static @Nullable FallbackGeneratorManager instance;

    private FallbackGeneratorManager()
        throws Exception
    {
        try
        {
            final String mappingsVersion = getMappingsVersion();
            final ClassGenerator entityFallingBlockClassGenerator =
                new EntityFallingBlockClassGenerator(mappingsVersion);

            final ClassGenerator craftFallingBlockClassGenerator =
                new CraftFallingBlockClassGenerator(mappingsVersion,
                                                    entityFallingBlockClassGenerator.getGeneratedClass());

            final ClassGenerator nmsBlockClassGenerator = new NMSBlockClassGenerator(mappingsVersion);

            final ClassGenerator fallingBlockFactoryClassGenerator =
                new FallingBlockFactoryClassGenerator(mappingsVersion,
                                                      nmsBlockClassGenerator,
                                                      craftFallingBlockClassGenerator,
                                                      entityFallingBlockClassGenerator);

            fallingBlockFactory = (FallingBlockFactory) fallingBlockFactoryClassGenerator.getGeneratedConstructor()
                                                                                         .newInstance();
        }
        catch (Exception | ExceptionInInitializerError e)
        {
            throw new Exception("Failed to generate NMS code! Please contact pim16aap2!", e);
        }
    }

    /**
     * Obtains the instance of this class. If no instance exists yet, a new one will be constructor.
     *
     * @return The instance of this class.
     *
     * @throws Exception When any step of the generation process encounters an issue.
     */
    public static @NotNull FallbackGeneratorManager getInstance()
        throws Exception
    {
        synchronized (lck)
        {
            return instance == null ? instance = new FallbackGeneratorManager() : instance;
        }
    }

    /**
     * Retrieves the instances of the generated subclass of {@link FallingBlockFactory}.
     *
     * @return The instances of the generated subclass of {@link FallingBlockFactory}.
     */
    public @NotNull FallingBlockFactory getFallingBlockFactory()
    {
        synchronized (lck)
        {
            return fallingBlockFactory;
        }
    }

    private @NotNull String getMappingsVersion()
        throws IllegalAccessException, InvocationTargetException
    {
        final Method methodGetMappingsVersion = ReflectionUtils.getMethod(classCraftMagicNumbers, "getMappingsVersion");
        final Field instanceField = ReflectionUtils.getField(classCraftMagicNumbers, "INSTANCE");
        final Object craftMagicNumbersInstance = instanceField.get(null);
        return Objects.requireNonNull((String) methodGetMappingsVersion.invoke(craftMagicNumbersInstance),
                                      "Failed to find the current mappings version!");
    }
}
