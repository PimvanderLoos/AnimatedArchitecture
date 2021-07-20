package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
import nl.pim16aap2.bigDoors.util.ReflectionUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static net.bytebuddy.implementation.MethodCall.construct;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;

/**
 * Represents an implementation of a {@link ClassGenerator} to generate a subclass of {@link FallingBlockFactory}.
 *
 * @author Pim
 */
public class FallingBlockFactoryClassGenerator extends ClassGenerator
{
    private static final @NotNull Class<?>[] CONSTRUCTOR_PARAMETER_TYPES = new Class<?>[0];

    private final @NotNull ClassGenerator nmsBlockClassGenerator;
    private final @NotNull ClassGenerator craftFallingBlockClassGenerator;
    private final @NotNull ClassGenerator entityFallingBlockClassGenerator;

    public FallingBlockFactoryClassGenerator(@NotNull String mappingsVersion,
                                             @NotNull ClassGenerator nmsBlockClassGenerator,
                                             @NotNull ClassGenerator craftFallingBlockClassGenerator,
                                             @NotNull ClassGenerator entityFallingBlockClassGenerator)
        throws Exception
    {
        super(mappingsVersion);
        this.nmsBlockClassGenerator = nmsBlockClassGenerator;
        this.craftFallingBlockClassGenerator = craftFallingBlockClassGenerator;
        this.entityFallingBlockClassGenerator = entityFallingBlockClassGenerator;

        generate();
    }

    @Override
    protected @NotNull Class<?>[] getConstructorArgumentTypes()
    {
        return CONSTRUCTOR_PARAMETER_TYPES;
    }

    @Override
    protected @NotNull String getBaseName()
    {
        return "FallingBlockFactory";
    }

    @Override
    protected void generateImpl()
    {
        DynamicType.Builder<?> builder = createBuilder(FallingBlockFactory.class);

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
            .withParameters(org.bukkit.World.class, int.class, int.class, int.class)
            .intercept(construct(nmsBlockClassGenerator.getGeneratedConstructor())
                           .withArgument(0, 1, 2, 3).withMethodCall(createBlockInfo)
                           .withField("axesValues", "blockRotationValues"));

        return builder;
    }

    private DynamicType.Builder<?> addFallingBlockFactoryMethod(DynamicType.Builder<?> builder)
    {
        final String postProcessName = "generated$postProcessEntity";

        builder = builder
            .defineMethod(postProcessName, craftFallingBlockClassGenerator.getGeneratedClass(), Visibility.PRIVATE)
            .withParameters(craftFallingBlockClassGenerator.getGeneratedClass())
            .intercept(invoke(methodSetCraftEntityCustomName).onArgument(0).with("BigDoorsEntity").andThen(
                invoke(methodSetCraftEntityCustomNameVisible).onArgument(0).with(false)).andThen(
                FixedValue.argument(0)));

        final Method methodGetMyBlockData =
            ReflectionUtils.getMethod(nmsBlockClassGenerator.getGeneratedClass(), "getMyBlockData");

        final MethodCall createBlockData = (MethodCall)
            invoke(methodGetMyBlockData).onArgument(1).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall createEntity = construct(entityFallingBlockClassGenerator.getGeneratedConstructor())
            .withMethodCall(invoke(named("getWorld")).onArgument(0))
            .withMethodCall(invoke(named("getX")).onArgument(0))
            .withMethodCall(invoke(named("getY")).onArgument(0))
            .withMethodCall(invoke(named("getZ")).onArgument(0))
            .withMethodCall(createBlockData)
            .withField("enumMoveTypeValues");

        final MethodCall createCraftFallingBlock = (MethodCall)
            construct(craftFallingBlockClassGenerator.getGeneratedConstructor())
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
