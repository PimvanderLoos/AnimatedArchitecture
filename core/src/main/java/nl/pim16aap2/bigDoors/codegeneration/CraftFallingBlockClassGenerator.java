package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

import static net.bytebuddy.implementation.FieldAccessor.ofField;
import static net.bytebuddy.implementation.FixedValue.value;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;

/**
 * Represents an implementation of a {@link ClassGenerator} to generate a subclass of {@link CustomCraftFallingBlock}.
 *
 * @author Pim
 */
final class CraftFallingBlockClassGenerator extends ClassGenerator
{
    private static final String FIELD_CUSTOM_FBLOCK = "generated$customEntityFallingBlock";
    private static final String FIELD_ENTITY = "entity";

    // Overriding methods
    private static final String METHOD_IS_ON_GROUND = "isOnGround";
    private static final String METHOD_TO_STRING = "toString";
    private static final String METHOD_GET_TYPE = "getType";
    private static final String METHOD_GET_DROP_ITEM = "getDropItem";
    private static final String METHOD_SET_DROP_ITEM = "setDropItem";
    private static final String METHOD_CAN_HURT_ENTITIES = "canHurtEntities";
    private static final String METHOD_SET_HURT_ENTITIES = "setHurtEntities";
    private static final String METHOD_GET_HANDLE = "getHandle";
    private static final String METHOD_GET_BLOCK_DATA = "getBlockData";
    private static final String METHOD_SET_HEAD_POSE = "setHeadPose";
    private static final String METHOD_SET_BODY_POSE = "setBodyPose";

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

    public interface IGeneratedCraftFallingBlock
    {}

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
        builder = builder.defineMethod(METHOD_IS_ON_GROUND, boolean.class).intercept(value(false));
        builder = builder.defineMethod(METHOD_TO_STRING, String.class).intercept(value("CraftFallingBlock"));
        builder = builder.defineMethod(METHOD_GET_TYPE, EntityType.class).intercept(value(EntityType.FALLING_BLOCK));
        builder = builder.defineMethod(METHOD_GET_DROP_ITEM, boolean.class).intercept(value(false));
        builder = builder.defineMethod(METHOD_CAN_HURT_ENTITIES, boolean.class).intercept(value(false));
        builder = builder.defineMethod(METHOD_GET_HANDLE, classGeneratedEntityFallingBlock)
                         .intercept(ofField(FIELD_ENTITY).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        builder = builder.defineMethod(METHOD_GET_BLOCK_DATA, BlockData.class)
                         .intercept(invoke(methodFromData)
                                        .withMethodCall(invoke(methodEntityFallingBlockGetBlock)
                                                            .onMethodCall(invoke(named(METHOD_GET_HANDLE)))));

        // Setters
        builder = builder.defineMethod(METHOD_SET_DROP_ITEM, void.class).withParameters(boolean.class)
                         .intercept(StubMethod.INSTANCE);
        builder = builder.defineMethod(METHOD_SET_HURT_ENTITIES, void.class).withParameters(boolean.class)
                         .intercept(StubMethod.INSTANCE);
        builder = builder.defineMethod(METHOD_SET_HEAD_POSE, void.class).withParameters(EulerAngle.class)
                         .intercept(StubMethod.INSTANCE);
        builder = builder.defineMethod(METHOD_SET_BODY_POSE, void.class).withParameters(EulerAngle.class)
                         .intercept(StubMethod.INSTANCE);

        // Slightly more involved methods
        builder = builder
            .define(methodCraftEntitySetTicksLived)
            .intercept(invoke(methodCraftEntitySetTicksLived).onSuper().withArgument(0).andThen(
                invoke(named(EntityFallingBlockClassGenerator.METHOD_SET_TICKS_LIVED))
                    .onMethodCall(invoke(named(METHOD_GET_HANDLE))).withArgument(0)));
        return builder;
    }
}
