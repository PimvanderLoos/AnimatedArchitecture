package nl.pim16aap2.bigDoors.codegeneration;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;

public class CraftEntityFallingBlockGenerator implements IGenerator
{
    private boolean isGenerated = false;
    private final @NotNull String mappingsVersion;
    private @Nullable Class<?> generatedClass;
    private @Nullable Constructor<?> generatedConstructor;

    public CraftEntityFallingBlockGenerator(@NotNull String mappingsVersion)
        throws Exception
    {
        this.mappingsVersion = mappingsVersion;

        try
        {
            init();
        }
        catch (NullPointerException | IllegalStateException | IOException e)
        {
            throw new Exception("Failed to find all components required to generate a CraftEntityFallingBlock!", e);
        }
    }

    private void init()
        throws IOException
    {

    }

    @Override
    public synchronized @NotNull IGenerator generate()
        throws Exception
    {
        if (isGenerated)
            return this;
        isGenerated = true;

        return this;
    }

    @Override
    public Class<?> getGeneratedClass()
    {
        return generatedClass;
    }

    @Override
    public Constructor<?> getGeneratedConstructor()
    {
        return generatedConstructor;
    }
}
