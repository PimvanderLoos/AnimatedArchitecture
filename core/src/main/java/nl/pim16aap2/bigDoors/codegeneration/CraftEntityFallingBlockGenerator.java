package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

public class CraftEntityFallingBlockGenerator implements IGenerator
{
    private final @NotNull String mappingsVersion;
    private boolean isGenerated = false;
    private @Nullable Class<?> generatedClass;
    private @Nullable Constructor<?> generatedConstructor;

    private final Class<?> classCraftEntity;
    private final Class<?> classCraftServer;

    public CraftEntityFallingBlockGenerator(@NotNull String mappingsVersion)
    {
        this.mappingsVersion = mappingsVersion;

        final String craftBase = ReflectionUtils.CRAFT_BASE;
        classCraftEntity = ReflectionUtils.findClass(craftBase + "entity.CraftEntity");
        classCraftServer = ReflectionUtils.findClass(craftBase + "CraftServer");
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
    public @Nullable Class<?> getGeneratedClass()
    {
        return generatedClass;
    }

    @Override
    public @Nullable Constructor<?> getGeneratedConstructor()
    {
        return generatedConstructor;
    }
}
