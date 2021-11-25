package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.implementation.FieldAccessor.ofField;
import static net.bytebuddy.implementation.FixedValue.value;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;
import static nl.pim16aap2.bigDoors.reflection.ReflectionBuilder.findField;
import static nl.pim16aap2.bigDoors.reflection.ReflectionBuilder.findMethod;

/**
 * Represents an implementation of a {@link ClassGenerator} to generate a subclass of {@link CustomCraftFallingBlock}.
 *
 * @author Pim
 */
final class CraftFallingBlockClassGenerator extends ClassGenerator
{
    private static final String FIELD_CUSTOM_FBLOCK = "generated$customEntityFallingBlock";

    private static final Field FIELD_ENTITY = findField().inClass(classCraftEntity).withName("entity").get();

    private static final Method METHOD_IS_ON_GROUND =
        findMethod().inClass(Entity.class).withName("isOnGround").get();
    private static final Method METHOD_GET_TYPE =
        findMethod().inClass(Entity.class).withName("getType").get();
    private static final Method METHOD_GET_DROP_ITEM =
        findMethod().inClass(FallingBlock.class).withName("getDropItem").get();
    private static final Method METHOD_SET_DROP_ITEM =
        findMethod().inClass(FallingBlock.class).withName("setDropItem").get();
    private static final Method METHOD_CAN_HURT_ENTITIES =
        findMethod().inClass(FallingBlock.class).withName("canHurtEntities").get();
    private static final Method METHOD_SET_HURT_ENTITIES =
        findMethod().inClass(FallingBlock.class).withName("setHurtEntities").get();
    private static final Method METHOD_SET_HEAD_POSE =
        findMethod().inClass(CustomCraftFallingBlock.class).withName("setHeadPose").get();
    private static final Method METHOD_SET_BODY_POSE =
        findMethod().inClass(CustomCraftFallingBlock.class).withName("setBodyPose").get();
    private static final Method METHOD_GET_BLOCK_DATA =
        findMethod().inClass(FallingBlock.class).withName("getBlockData").get();
    private static final Method METHOD_TO_STRING =
        findMethod().inClass(Object.class).withName("toString").get();

    private final @NotNull Class<?>[] constructorParameterTypes;
    private final @NotNull Class<?> classGeneratedEntityFallingBlock;

    public CraftFallingBlockClassGenerator(@NotNull String mappingsVersion,
                                           @NotNull Class<?> classGeneratedEntityFallingBlock)
        throws Exception
    {
        super(mappingsVersion);
        this.classGeneratedEntityFallingBlock = classGeneratedEntityFallingBlock;
        this.constructorParameterTypes = new Class<?>[]{classCraftServer, this.classGeneratedEntityFallingBlock};

        generate();
    }

    @Override
    protected @NotNull Class<?>[] getConstructorArgumentTypes()
    {
        return constructorParameterTypes;
    }

    @Override
    protected @NotNull String getBaseName()
    {
        return "CraftFallingBlock";
    }

    @Override
    protected void generateImpl()
    {
        DynamicType.Builder<?> builder = createBuilder(classCraftEntity)
            .implement(org.bukkit.entity.FallingBlock.class,
                       CustomCraftFallingBlock.class,
                       IGeneratedCraftFallingBlock.class);

        builder = addCTor(builder);
        builder = addBasicMethods(builder);

        finishBuilder(builder);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        builder = builder.defineField(FIELD_CUSTOM_FBLOCK, classGeneratedEntityFallingBlock);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(getConstructorArgumentTypes())
            .intercept(invoke(ctorCraftEntity).withArgument(0, 1).andThen(
                FieldAccessor.ofField(FIELD_CUSTOM_FBLOCK).setsArgumentAt(1)));
    }

    private DynamicType.Builder<?> addBasicMethods(DynamicType.Builder<?> builder)
    {
        // Simple getters
        builder = builder.define(METHOD_IS_ON_GROUND).intercept(value(false));
        builder = builder.define(METHOD_TO_STRING).intercept(value("CraftFallingBlock"));
        builder = builder.define(METHOD_GET_TYPE).intercept(value(EntityType.FALLING_BLOCK));
        builder = builder.define(METHOD_GET_DROP_ITEM).intercept(value(false));
        builder = builder.define(METHOD_CAN_HURT_ENTITIES).intercept(value(false));
        builder = builder.defineMethod(methodGetEntityHandle.getName(), classGeneratedEntityFallingBlock)
                         .intercept(ofField(FIELD_ENTITY.getName())
                                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        builder = builder.define(METHOD_GET_BLOCK_DATA)
                         .intercept(invoke(methodCraftBockDataFromNMSBlockData)
                                        .withMethodCall(invoke(methodEntityFallingBlockGetBlock)
                                                            .onMethodCall(invoke(named(methodGetEntityHandle
                                                                                           .getName())))));

        // Setters
        builder = builder.define(METHOD_SET_DROP_ITEM).intercept(StubMethod.INSTANCE);
        builder = builder.define(METHOD_SET_HURT_ENTITIES).intercept(StubMethod.INSTANCE);
        builder = builder.define(METHOD_SET_HEAD_POSE).intercept(StubMethod.INSTANCE);
        builder = builder.define(METHOD_SET_BODY_POSE).intercept(StubMethod.INSTANCE);

        // Slightly more involved methods
        builder = builder
            .define(methodCraftEntitySetTicksLived)
            .intercept(invoke(methodCraftEntitySetTicksLived).onSuper().withArgument(0).andThen(
                invoke(named(EntityFallingBlockClassGenerator.METHOD_SET_TICKS_LIVED.getName()))
                    .onMethodCall(invoke(named(methodGetEntityHandle.getName()))).withArgument(0)));
        return builder;
    }

    public interface IGeneratedCraftFallingBlock
    {}
}
