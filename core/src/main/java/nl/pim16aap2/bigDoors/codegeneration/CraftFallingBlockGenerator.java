package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

public class CraftFallingBlockGenerator extends Generator
{
    private final Class<?> classCraftEntity;
    private final Class<?> classCraftServer;

    public CraftFallingBlockGenerator(@NotNull String mappingsVersion)
    {
        super(mappingsVersion);

        final String craftBase = ReflectionUtils.CRAFT_BASE;
        classCraftEntity = ReflectionUtils.findClass(craftBase + "entity.CraftEntity");
        classCraftServer = ReflectionUtils.findClass(craftBase + "CraftServer");
    }

    public interface IGeneratedCraftFallingBlock
    {}

    @Override
    protected void generateImpl()
        throws Exception
    {
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(classCraftEntity, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .implement(org.bukkit.entity.FallingBlock.class,
                       CustomCraftFallingBlock.class,
                       IGeneratedCraftFallingBlock.class)
            // TODO: Use full name
//            .name("CustomCraftFallingBlock$" + this.mappingsVersion);
            .name("CustomCraftFallingBlock$generated");


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
