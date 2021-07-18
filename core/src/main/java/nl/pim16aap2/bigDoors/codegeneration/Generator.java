package nl.pim16aap2.bigDoors.codegeneration;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

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

    public @Nullable Class<?> getGeneratedClass()
    {
        return generatedClass;
    }

    public @Nullable Constructor<?> getGeneratedConstructor()
    {
        return generatedConstructor;
    }
}
