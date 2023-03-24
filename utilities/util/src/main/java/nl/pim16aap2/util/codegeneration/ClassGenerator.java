package nl.pim16aap2.util.codegeneration;

import lombok.ToString;
import lombok.extern.flogger.Flogger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an abstract super class that can generate new classes and insert them into a specific
 * {@link ClassLoader}.
 */
@SuppressWarnings("unused")
@Flogger
public abstract class ClassGenerator
{
    private boolean isGenerated = false;

    @ToString.Exclude
    private final ClassLoader classLoader;

    protected @Nullable Class<?> generatedClass;
    protected @Nullable Constructor<?> generatedConstructor;

    /**
     * @param classLoader
     *     The {@link ClassLoader} to insert generated classes into.
     */
    protected ClassGenerator(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    /**
     * Generates the class.
     *
     * @throws Exception
     *     When any kind of exception occurs during the generation process.
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
        log.atInfo().log("Generated Class %s in %dms.", getGeneratedClass().getName(), duration / 1_000_000);
    }

    /**
     * The implementation of the class generation code. This method is called by {@link #generate()} when needed.
     *
     * @throws Exception
     *     When any kind of exception occurs during the generation process.
     */
    protected abstract void generateImpl()
        throws Exception;

    /**
     * Creates a new {@link ByteBuddy} builder.
     * <p>
     * The returned builder will be a subclass of the provided superclass, have a properly formatted name, and implement
     * {@link IGeneratedClass}.
     *
     * @param superClass
     *     The {@link Class} the builder should be a subclass of. This can also be an interface.
     * @return The new, properly configured, {@link ByteBuddy} builder.
     */
    protected final DynamicType.Builder<?> createBuilder(Class<?> superClass)
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
    protected String getFormattedName()
    {
        return String.format("%s$Generated", getBaseName());
    }

    /**
     * Retrieves the base name of the class to be generated.
     * <p>
     * This should be the simple name describing the class that is to be generated.
     * <p>
     * For example, the base name of the generated EntityFallingBlock class would be just that: "EntityFallingBlock".
     * Any additional information (such as the fact that it is a generated class) will be added later on by
     * {@link #getFormattedName()}.
     *
     * @return The base name of the class to be generated.
     */
    protected abstract String getBaseName();

    /**
     * Retrieves the array of parameter types as the generated constructor takes them.
     *
     * @return The array of parameter types of the generated constructor.
     */
    protected abstract Class<?>[] getConstructorArgumentTypes();

    /**
     * Finishes the builder.
     * <p>
     * This method takes care of loading the generated class, and retrieving the class object and the constructor of the
     * generated class.
     * <p>
     * The generated class will not be saved to a file.
     *
     * @param builder
     *     The builder to finish.
     */
    protected final void finishBuilder(DynamicType.Builder<?> builder)
    {
        finishBuilder(builder, null);
    }

    /**
     * Finishes the builder.
     * <p>
     * This method takes care of loading the generated class, and retrieving the class object and the constructor of the
     * generated class.
     *
     * @param builder
     *     The builder to finish.
     * @param savePath
     *     The path to save the generated class to. If this is {@code null}, the class will not be saved.
     */
    protected final void finishBuilder(DynamicType.Builder<?> builder, @Nullable Path savePath)
    {
        try (DynamicType.Unloaded<?> unloaded = builder.make())
        {
            if (savePath != null)
                saveGeneratedClass(unloaded, savePath);

            this.generatedClass = unloaded.load(classLoader, ClassLoadingStrategy.Default.INJECTION).getLoaded();
            this.generatedConstructor = this.generatedClass.getConstructor(getConstructorArgumentTypes());

            Objects.requireNonNull(this.generatedClass, "Failed to construct class with generator: " + this);
            Objects.requireNonNull(this.generatedConstructor, "Failed to find constructor with generator: " + this);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to finish class generator: " + this, e);
        }
    }

    /**
     * Tries to save a generated (but unloaded) class to a file.
     *
     * @param unloaded
     *     The unloaded class to write to a file.
     * @param file
     *     The file the class will be written to.
     */
    private void saveGeneratedClass(DynamicType.Unloaded<?> unloaded, Path file)
    {
        try
        {
            unloaded.saveIn(file.toFile());
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to save class '%s' to file: '%s'", getFormattedName(), file);
        }
    }

    /**
     * Gets the class generated by this ClassGenerator.
     *
     * @return The generated class.
     */
    public final Class<?> getGeneratedClass()
    {
        return Objects.requireNonNull(generatedClass, "Failed to find class generated by ClassGenerator " + this);
    }

    /**
     * Gets the constructor generated by this ClassGenerator.
     *
     * @return The constructor generated by this ClassGenerator.
     */
    public final Constructor<?> getGeneratedConstructor()
    {
        return Objects.requireNonNull(generatedConstructor,
                                      "Failed to find constructor generated by ClassGenerator " +
                                          this + " for class: " + getGeneratedClass());
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() +
            "{generatedClass=" + generatedClass +
            ", generatedConstructor=" + generatedConstructor +
            ", className=" + getBaseName() +
            ", ctorArgumentTypes=" + Arrays.toString(getConstructorArgumentTypes()) +
            '}';
    }
}
