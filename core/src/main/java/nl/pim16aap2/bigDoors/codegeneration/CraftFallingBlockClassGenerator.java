package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import org.bukkit.Material;
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
        builder = builder.defineField("generated$customEntityFallingBlock", classGeneratedEntityFallingBlock);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(getConstructorArgumentTypes())
            .intercept(invoke(ctorCraftEntity).withArgument(0, 1).andThen(
                FieldAccessor.ofField("generated$customEntityFallingBlock").setsArgumentAt(1)));
    }

    private DynamicType.Builder<?> addBasicMethods(DynamicType.Builder<?> builder)
    {
        // Simple getters
        builder = builder.defineMethod("isOnGround", boolean.class).intercept(value(false));
        builder = builder.defineMethod("toString", String.class).intercept(value("CraftFallingBlock"));
        builder = builder.defineMethod("getType", EntityType.class).intercept(value(EntityType.FALLING_BLOCK));
        builder = builder.defineMethod("getDropItem", boolean.class).intercept(value(false));
        builder = builder.defineMethod("canHurtEntities", boolean.class).intercept(value(false));
        builder = builder.defineMethod("getHandle", classGeneratedEntityFallingBlock)
                         .intercept(ofField("entity").withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        builder = builder.defineMethod("getBlockData", BlockData.class)
                         .intercept(ExceptionMethod.throwing(UnsupportedOperationException.class));

        // Setters
        builder = builder.defineMethod("setDropItem", void.class).withParameters(boolean.class)
                         .intercept(StubMethod.INSTANCE);
        builder = builder.defineMethod("setHurtEntities", void.class).withParameters(boolean.class)
                         .intercept(StubMethod.INSTANCE);
        builder = builder.defineMethod("setHeadPose", void.class).withParameters(EulerAngle.class)
                         .intercept(StubMethod.INSTANCE);
        builder = builder.defineMethod("setBodyPose", void.class).withParameters(EulerAngle.class)
                         .intercept(StubMethod.INSTANCE);

        // Slightly more involved methods
        builder = builder
            .define(methodCraftEntitySetTicksLived)
            .intercept(invoke(methodCraftEntitySetTicksLived).onSuper().withArgument(0).andThen(
                invoke(named("generated$setTicksLived")).onMethodCall(invoke(named("getHandle"))).withArgument(0)));

        builder = builder
            .defineMethod("getMaterial", Material.class)
            .intercept(invoke(methodGetItemType)
                           .onMethodCall(invoke(methodCraftMagicNumbersGetMaterial)
                                             .withMethodCall(invoke(named("getBlock"))
                                                                 .onMethodCall(invoke(named("getHandle"))))));
        return builder;
    }
}
