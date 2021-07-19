package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.implementation.MethodCall.construct;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
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
            .defineField("blockRotationValues", asArrayType(classEnumBlockRotation), Visibility.PRIVATE)
            .defineField("enumMoveTypeValues", asArrayType(classEnumMoveType), Visibility.PRIVATE);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        Object[] axesValues = ReflectionUtils.getEnumValues(classEnumDirectionAxis);
        Object[] blockRotationValues = ReflectionUtils.getEnumValues(classEnumBlockRotation);
        Object[] enumMoveTypeValues = ReflectionUtils.getEnumValues(classEnumMoveType);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .intercept(SuperMethodCall.INSTANCE.andThen(
                FieldAccessor.ofField("axesValues").setsValue(axesValues)).andThen(
                FieldAccessor.ofField("blockRotationValues").setsValue(blockRotationValues)).andThen(
                FieldAccessor.ofField("enumMoveTypeValues").setsValue(enumMoveTypeValues)));
    }

    private DynamicType.Builder<?> addNMSBlockFactoryMethod(DynamicType.Builder<?> builder)
    {
        final MethodCall getNMSWorld = (MethodCall)
            invoke(methodGetNMSWorld).onArgument(0).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall createBlockPosition = construct(cTorBlockPosition).withArgument(1, 2, 3);

        final MethodCall getType = invoke(methodGetTypeFromBlockPosition)
            .onMethodCall(getNMSWorld).withMethodCall(createBlockPosition);

        final MethodCall getBlock = invoke(methodGetBlockFromBlockData).onMethodCall(getType);

        final MethodCall createBlockInfo = (MethodCall)
            invoke(methodBlockInfoFromBlockBase).withMethodCall(getBlock)
                                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        builder = builder
            .defineMethod("nmsBlockFactory", NMSBlock.class)
            .withParameters(World.class, int.class, int.class, int.class)
            .intercept(construct(nmsBlock.second).withArgument(0, 1, 2, 3)
                                                 .withMethodCall(createBlockInfo)
                                                 .withField("axesValues", "blockRotationValues"));

        return builder;
    }

    private DynamicType.Builder<?> addFallingBlockFactoryMethod(DynamicType.Builder<?> builder)
    {
        final String postProcessName = "generated$postProcessEntity";

        builder = builder
            .defineMethod(postProcessName, craftFallingBlock.first, Visibility.PRIVATE)
            .withParameters(craftFallingBlock.first)
            .intercept(invoke(methodSetCraftEntityCustomName).onArgument(0).with("BigDoorsEntity").andThen(
                invoke(methodSetCraftEntityCustomNameVisible).onArgument(0).with(false)).andThen(
                FixedValue.argument(0)));

        final Method methodGetMyBlockData = ReflectionUtils.getMethod(nmsBlock.first, "getMyBlockData");

        final MethodCall createBlockData = (MethodCall)
            invoke(methodGetMyBlockData).onArgument(1).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall createEntity = construct(entityFallingBlock.second)
            .withMethodCall(invoke(named("getWorld")).onArgument(0))
            .withMethodCall(invoke(named("getX")).onArgument(0))
            .withMethodCall(invoke(named("getY")).onArgument(0))
            .withMethodCall(invoke(named("getZ")).onArgument(0))
            .withMethodCall(createBlockData)
            .withField("enumMoveTypeValues");

        final MethodCall createCraftFallingBlock = (MethodCall)
            construct(craftFallingBlock.second)
                .withMethodCall(invoke(methodGetBukkitServer))
                .withMethodCall(createEntity)
                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        builder = builder
            .defineMethod("fallingBlockFactory", CustomCraftFallingBlock.class)
            .withParameters(Location.class, NMSBlock.class, byte.class, Material.class)
            .intercept(invoke(named(postProcessName)).withMethodCall(createCraftFallingBlock)
                                                     .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));

        return builder;
    }
}
