package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.SuperMethodCall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;

public class FallingBlockFactoryGenerator extends Generator
{
    private final Pair<Class<?>, Constructor<?>> nmsBlock;
    private final Pair<Class<?>, Constructor<?>> craftFallingBlock;
    private final Pair<Class<?>, Constructor<?>> entityFallingBlock;

    public FallingBlockFactoryGenerator(@NotNull String mappingsVersion, Pair<Class<?>, Constructor<?>> nmsBlock,
                                        Pair<Class<?>, Constructor<?>> craftFallingBlock,
                                        Pair<Class<?>, Constructor<?>> entityFallingBlock)
    {
        super(mappingsVersion);
        this.nmsBlock = nmsBlock;
        this.craftFallingBlock = craftFallingBlock;
        this.entityFallingBlock = entityFallingBlock;
    }

    @Override
    protected void generateImpl()
        throws Exception
    {
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(FallingBlockFactory.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            // TODO: Use full name
//            .name("GeneratedFallingBlockFactory_" + this.mappingsVersion);
            .name("GeneratedFallingBlockFactory");

        builder = addCTor(builder);
        builder = addFields(builder);
        builder = addFallingBlockFactoryMethod(builder);
        builder = addNMSBlockFactoryMethod(builder);

        finishBuilder(builder);
    }

    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> builder)
    {
        return builder
            .defineField("axesValues", asArrayType(classEnumDirectionAxis), Visibility.PRIVATE)
            .defineField("blockRotationValues", asArrayType(classEnumBlockRotation), Visibility.PRIVATE);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        Object[] axesValues = ReflectionUtils.getEnumValues(classEnumDirectionAxis);
        Object[] blockRotationValues = ReflectionUtils.getEnumValues(classEnumBlockRotation);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .intercept(SuperMethodCall.INSTANCE.andThen(
                FieldAccessor.ofField("axesValues").setsValue(axesValues)).andThen(
                FieldAccessor.ofField("blockRotationValues").setsValue(blockRotationValues)));
    }

    private DynamicType.Builder<?> addFallingBlockFactoryMethod(DynamicType.Builder<?> builder)
    {
        return builder;
    }

    private DynamicType.Builder<?> addNMSBlockFactoryMethod(DynamicType.Builder<?> builder)
    {
        return builder;
    }
}
