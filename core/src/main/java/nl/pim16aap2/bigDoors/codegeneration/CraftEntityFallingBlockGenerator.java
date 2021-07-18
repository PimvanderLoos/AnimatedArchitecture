package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

public class CraftEntityFallingBlockGenerator extends Generator
{
    private final Class<?> classCraftEntity;
    private final Class<?> classCraftServer;

    public CraftEntityFallingBlockGenerator(@NotNull String mappingsVersion)
    {
        super(mappingsVersion);

        final String craftBase = ReflectionUtils.CRAFT_BASE;
        classCraftEntity = ReflectionUtils.findClass(craftBase + "entity.CraftEntity");
        classCraftServer = ReflectionUtils.findClass(craftBase + "CraftServer");
    }

    @Override
    protected void generateImpl()
        throws Exception
    {

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
