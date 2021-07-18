package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import static net.bytebuddy.implementation.FieldAccessor.ofField;
import static net.bytebuddy.implementation.FixedValue.value;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static nl.pim16aap2.bigDoors.reflection.ReflectionUtils.*;

public class CraftFallingBlockGenerator extends Generator
{
    private final @NotNull Class<?> classGeneratedEntityFallingBlock;

    private final Class<?> classCraftEntity;
    private final Class<?> classCraftServer;
    private final Class<?> classNMSEntity;
    private final Class<?> classIBlockData;
    private final Class<?> classCraftMagicNumbers;

    private final Method methodCraftMagicNumbersGetMaterial;
    private final Method methodGetItemType;
    private final Method methodCraftEntitySetTicksLived;

    private final Constructor<?> ctorCraftEntity;

    public CraftFallingBlockGenerator(@NotNull String mappingsVersion,
                                      Class<?> classGeneratedEntityFallingBlock)
    {
        super(mappingsVersion);
        this.classGeneratedEntityFallingBlock = Objects
            .requireNonNull(classGeneratedEntityFallingBlock, "No classGeneratedEntityFallingBlock provided!");

        final String craftBase = ReflectionUtils.CRAFT_BASE;
        final String nmsBase = ReflectionUtils.NMS_BASE;
        classCraftEntity = findClass(craftBase + "entity.CraftEntity");
        classCraftServer = findClass(craftBase + "CraftServer");
        classNMSEntity = findFirstClass(nmsBase + "Entity", "net.minecraft.world.entity.Entity");
        classIBlockData = findFirstClass(nmsBase + "IBlockData", "net.minecraft.world.level.block.state.IBlockData");
        classCraftMagicNumbers = findClass(craftBase + "util.CraftMagicNumbers");

        methodCraftMagicNumbersGetMaterial = getMethod(classCraftMagicNumbers, "getMaterial", classIBlockData);
        methodGetItemType = getMethod(MaterialData.class, "getItemType");
        methodCraftEntitySetTicksLived = ReflectionUtils.getMethod(classCraftEntity, "setTicksLived", int.class);

        ctorCraftEntity = ReflectionUtils.findCTor(classCraftEntity, classCraftServer, classNMSEntity);
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
//            .name("GeneratedCustomCraftFallingBlock_" + this.mappingsVersion);
            .name("GeneratedCustomCraftFallingBlock");

        builder = addCTor(builder);
        builder = addBasicMethods(builder);

        finishBuilder(builder, classCraftServer, classGeneratedEntityFallingBlock);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        builder = builder.defineField("generated$customEntityFallingBlock", classGeneratedEntityFallingBlock);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(classCraftServer, classGeneratedEntityFallingBlock)
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
