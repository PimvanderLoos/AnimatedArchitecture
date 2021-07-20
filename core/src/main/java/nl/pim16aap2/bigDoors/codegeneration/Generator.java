package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Objects;

abstract class Generator
{
    private final @NotNull Object lck = new Object();
    private boolean isGenerated = false;

    protected final @NotNull String mappingsVersion;

    protected @Nullable Class<?> generatedClass;
    protected @Nullable Constructor<?> generatedConstructor;

    protected Generator(@NotNull String mappingsVersion)
    {
        this.mappingsVersion = mappingsVersion;
    }

    public final @NotNull Generator generate()
        throws Exception
    {
        synchronized (lck)
        {
            if (isGenerated)
                return this;
            isGenerated = true;
            generateImpl();
            return this;
        }
    }

    protected abstract void generateImpl()
        throws Exception;

    protected final void finishBuilder(DynamicType.Builder<?> builder, Class<?>... ctorArguments)
        throws IOException
    {
        DynamicType.Unloaded<?> unloaded = builder.make();

        if (ConfigLoader.DEBUG)
            unloaded.saveIn(new File(BigDoors.get().getDataFolder(), "generated"));

        this.generatedClass = unloaded.load(BigDoors.get().getBigDoorsClassLoader(),
                                            ClassLoadingStrategy.Default.INJECTION).getLoaded();

        try
        {
            this.generatedConstructor = this.generatedClass.getConstructor(ctorArguments);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Failed to get constructor of generated class for mapping " +
                                           mappingsVersion + " for generator: " + this, e);
        }
        Objects.requireNonNull(this.generatedClass, "Failed to construct class with generator: " + this);
        Objects.requireNonNull(this.generatedConstructor, "Failed to find constructor with generator: " + this);
    }

    public @Nullable Class<?> getGeneratedClass()
    {
        return generatedClass;
    }

    public @Nullable Constructor<?> getGeneratedConstructor()
    {
        return generatedConstructor;
    }
}
