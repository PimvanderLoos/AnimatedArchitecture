package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * Represents a generator that can generate new classes and insert them into the {@link ClassLoader} provided by {@link
 * BigDoors#getBigDoorsClassLoader()}.
 *
 * @author Pim
 */
abstract class ClassGenerator
{
    private boolean isGenerated = false;

    private final @NotNull String mappingsVersion;

    private final @NotNull ClassLoader classLoader = BigDoors.get().getBigDoorsClassLoader();

    protected @Nullable Class<?> generatedClass;
    protected @Nullable Constructor<?> generatedConstructor;

    /**
     * @param mappingsVersion The current mappings version. This can be found using the {@code getMappingsVersion}
     *                        method in the {@code CraftMagicNumbers} class.
     */
    protected ClassGenerator(@NotNull String mappingsVersion)
    {
        this.mappingsVersion = mappingsVersion;
    }

    /**
     * Generates the class.
     *
     * @throws Exception When any kind of exception occurs during the generation process.
     */
    protected final void generate()
        throws Exception
    {
        if (isGenerated)
        {
            if (generatedClass == null || generatedConstructor == null)
                throw new IllegalStateException(getFormattedName() + " could not be generated");
            return;
        }
        isGenerated = true;
        final long startTime = System.nanoTime();
        generateImpl();
        final long duration = System.nanoTime() - startTime;
        BigDoors.get().getMyLogger().info(String.format("Generated Class %s in %dms",
                                                        getGeneratedClass().getName(), duration / 1000000));
    }

    /**
     * The implementation of the class generation code. This method is called by {@link #generate()} when needed.
     *
     * @throws Exception When any kind of exception occurs during the generation process.
     */
    protected abstract void generateImpl()
        throws Exception;

    /**
     * Creates a new {@link ByteBuddy} builder.
     * <p>
     * The returned builder will be a subclass of the provided superclass, have a properly formatted name, and implement
     * {@link IGeneratedClass}.
     *
     * @param superClass The {@link Class} the builder should be a subclass of. This can also be an interface.
     * @return The new, properly configured, {@link ByteBuddy} builder.
     */
    protected final @NotNull DynamicType.Builder<?> createBuilder(@NotNull Class<?> superClass)
    {
        return new ByteBuddy()
            .subclass(superClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .implement(IGeneratedClass.class)
            .name(getFormattedName());
    }

    /**
     * Retrieves the name the generated class should have in the correct format.
     * <p>
     * {@link #getBaseName()} is used as the basis of the formatted name.
     *
     * @return The correctly formatted name.
     */
    protected @NotNull String getFormattedName()
    {
        return String.format("Generated_%s_%s", getBaseName(), mappingsVersion);
    }

    /**
     * Retrieves the base name of the class to be generated.
     * <p>
     * This should be the simple name describing the class that is to be generated.
     * <p>
     * For example, the base name of the generated EntityFallingBlock class would be just that: "EntityFallingBlock".
     * Any additional information (such as the fact that it is a generated class) will be added later on by {@link
     * #getFormattedName()}.
     *
     * @return The base name of the class to be generated.
     */
    protected abstract @NotNull String getBaseName();

    /**
     * Retrieves the array of parameter types as the generated constructor takes them.
     *
     * @return The array of parameter types of the generated constructor.
     */
    protected abstract @NotNull Class<?>[] getConstructorArgumentTypes();

    /**
     * Finishes the builder.
     * <p>
     * This method takes care of loading the generated class, saving the generated class (when {@link
     * ConfigLoader#DEBUG} is enabled), and retrieving the class object and the constructor of the generated class.
     *
     * @param builder The builder to finish.
     */
    protected final void finishBuilder(@NotNull DynamicType.Builder<?> builder)
    {
        DynamicType.Unloaded<?> unloaded = builder.make();

        if (ConfigLoader.DEBUG)
            saveGeneratedClass(unloaded, new File(BigDoors.get().getDataFolder(), "generated"));

        this.generatedClass = unloaded.load(classLoader, ClassLoadingStrategy.Default.INJECTION).getLoaded();

        try
        {
            this.generatedConstructor = this.generatedClass.getConstructor(getConstructorArgumentTypes());
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Failed to get constructor of generated class for mapping " +
                                           mappingsVersion + " for generator: " + this, e);
        }
        Objects.requireNonNull(this.generatedClass, "Failed to construct class with generator: " + this);
        Objects.requireNonNull(this.generatedConstructor, "Failed to find constructor with generator: " + this);
    }

    /**
     * Tries to save a generated (but unloaded) class to a file.
     *
     * @param unloaded The unloaded class to write to a file.
     * @param file     The file the class will be written to.
     */
    private void saveGeneratedClass(@NotNull DynamicType.Unloaded<?> unloaded, @NotNull File file)
    {
        try
        {
            unloaded.saveIn(file);
        }
        catch (IOException e)
        {
            BigDoors.get().getMyLogger().severe("Failed to save class " + getFormattedName() + " to file: " + file);
            e.printStackTrace();
        }
    }

    /**
     * Gets the class generated by this ClassGenerator.
     *
     * @return The generated class.
     */
    public final @NotNull Class<?> getGeneratedClass()
    {
        return Objects.requireNonNull(generatedClass, "Failed to find class generated by ClassGenerator " + this);
    }

    /**
     * Gets the constructor generated by this ClassGenerator.
     *
     * @return The constructor generated by this ClassGenerator.
     */
    public final @NotNull Constructor<?> getGeneratedConstructor()
    {
        return Objects.requireNonNull(generatedConstructor,
                                      "Failed to find constructor generated by ClassGenerator " +
                                          this + " for class: " + getGeneratedClass());
    }
}
