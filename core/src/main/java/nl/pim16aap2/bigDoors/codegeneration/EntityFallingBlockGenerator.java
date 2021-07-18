package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.bukkit.World;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.bytebuddy.implementation.FixedValue.value;
import static net.bytebuddy.implementation.MethodCall.construct;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;

final class EntityFallingBlockGenerator extends Generator
{
    private final Class<?> classEntityFallingBlock;
    private final Class<?> classIBlockData;
    private final Class<?> classCraftWorld;
    private final Class<?> classNMSWorld;
    private final Class<?> classNMSWorldServer;
    private final Class<?> classBlockPosition;
    private final Class<?> classNMSEntity;
    private final Class<?> classVec3D;
    private final Class<?> classEnumMoveType;
    private final Class<?> classNBTTagCompound;
    private final Class<?> classNBTBase;
    private final Class<?> classCrashReportSystemDetails;
    private final Class<?> classGameProfileSerializer;

    private final Constructor<?> cTorNMSFallingBlockEntity;
    private final Constructor<?> cTorBlockPosition;
    private final Constructor<?> cTorVec3D;

    private final Method methodTick;
    private final Method methodGetNMSWorld;
    private final Method methodSetPosition;
    private final Method methodSetNoGravity;
    private final Method methodGetMot;
    private final Method methodSetMot;
    private final Method methodSetMotVec;
    private final Method methodHurtEntities;
    private final Method methodMove;
    private final Method methodSaveData;
    private final Method methodLoadData;
    private final Method methodGetBlock;
    private final Method methodSetStartPos;
    private final Method methodLocX;
    private final Method methodLocY;
    private final Method methodLocZ;
    private final Method methodNMSAddEntity;
    private final Method methodAppendEntityCrashReport;
    private final Method methodCrashReportAppender;
    private final Method methodNBTTagCompoundSet;
    private final Method methodNBTTagCompoundSetInt;
    private final Method methodNBTTagCompoundSetBoolean;
    private final Method methodNBTTagCompoundSetFloat;
    private final Method methodNBTTagCompoundHasKeyOfType;
    private final Method methodNBTTagCompoundGetCompound;
    private final Method methodNBTTagCompoundGetInt;
    private final Method methodIBlockDataSerializer;
    private final Method methodIBlockDataDeserializer;
    private final Method methodIsAir;

    private final Field fieldNoClip;
    private final Field fieldTileEntityData;
    private final Field fieldTicksLived;
    private final Field fieldHurtEntities;
    private final Field fieldNMSWorld;

    private final List<Field> fieldsVec3D;

    private final Object fieldEnumMoveTypeSelf;

    public EntityFallingBlockGenerator(@NotNull String mappingsVersion)
    {
        super(mappingsVersion);

        String nmsBase = ReflectionUtils.NMS_BASE;
        classEntityFallingBlock = ReflectionUtils.findFirstClass(nmsBase + "EntityFallingBlock",
                                                                 "net.minecraft.world.entity.item.EntityFallingBlock");
        classNBTTagCompound = ReflectionUtils.findFirstClass(nmsBase + "NBTTagCompound",
                                                             "net.minecraft.nbt.NBTTagCompound");
        classNBTBase = ReflectionUtils.findFirstClass(nmsBase + "NBTBase", "net.minecraft.nbt.NBTBase");
        classIBlockData = ReflectionUtils.findFirstClass(nmsBase + "IBlockData",
                                                         "net.minecraft.world.level.block.state.IBlockData");
        classCraftWorld = ReflectionUtils.findFirstClass(ReflectionUtils.CRAFT_BASE + "CraftWorld");
        classEnumMoveType = ReflectionUtils.findFirstClass(nmsBase + "EnumMoveType",
                                                           "net.minecraft.world.entity.EnumMoveType");
        classVec3D = ReflectionUtils.findFirstClass(nmsBase + "Vec3D", "net.minecraft.world.phys.Vec3D");
        classNMSWorld = ReflectionUtils.findFirstClass(nmsBase + "World", "net.minecraft.world.level.World");
        classNMSWorldServer = ReflectionUtils.findFirstClass(nmsBase + "WorldServer",
                                                             "net.minecraft.server.level.WorldServer");
        classNMSEntity = ReflectionUtils.findFirstClass(nmsBase + "Entity", "net.minecraft.world.entity.Entity");
        classBlockPosition = ReflectionUtils.findFirstClass(nmsBase + "BlockPosition",
                                                            "net.minecraft.core.BlockPosition");
        classCrashReportSystemDetails = ReflectionUtils.findFirstClass(nmsBase + "CrashReportSystemDetails",
                                                                       "net.minecraft.CrashReportSystemDetails");
        classGameProfileSerializer = ReflectionUtils.findFirstClass(nmsBase + "GameProfileSerializer",
                                                                    "net.minecraft.nbt.GameProfileSerializer");


        cTorNMSFallingBlockEntity = ReflectionUtils.findCTor(classEntityFallingBlock, classNMSWorld, double.class,
                                                             double.class, double.class, classIBlockData);
        cTorBlockPosition = ReflectionUtils.findCTor(classBlockPosition, double.class, double.class, double.class);
        cTorVec3D = ReflectionUtils.findCTor(classVec3D, double.class, double.class, double.class);


        methodGetNMSWorld = ReflectionUtils.getMethod(classCraftWorld, "getHandle");
        methodTick = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC);
        methodSetPosition = ReflectionUtils.getMethod(classNMSEntity, "setPosition",
                                                      double.class, double.class, double.class);
        methodSetNoGravity = ReflectionUtils.getMethod(classNMSEntity, "setNoGravity", boolean.class);
        methodSetMot = ReflectionUtils.getMethod(classNMSEntity, "setMot", double.class, double.class, double.class);
        methodSetMotVec = ReflectionUtils.getMethod(classNMSEntity, "setMot", classVec3D);
        methodGetMot = ReflectionUtils.getMethod(classNMSEntity, "getMot");
        methodHurtEntities = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, boolean.class,
                                                                   Modifier.PUBLIC,
                                                                   float.class, float.class, null);
        methodMove = ReflectionUtils.getMethod(true, classNMSEntity, "move", classEnumMoveType, classVec3D);
        methodSaveData = ReflectionUtils.getMethod(classEntityFallingBlock, "saveData", classNBTTagCompound);
        methodLoadData = ReflectionUtils.getMethod(classEntityFallingBlock, "loadData", classNBTTagCompound);
        methodGetBlock = ReflectionUtils.getMethod(classEntityFallingBlock, "getBlock");
        methodSetStartPos = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, void.class,
                                                                  Modifier.PUBLIC, classBlockPosition);
        methodLocX = ReflectionUtils.getMethod(classNMSEntity, "locX");
        methodLocY = ReflectionUtils.getMethod(classNMSEntity, "locY");
        methodLocZ = ReflectionUtils.getMethod(classNMSEntity, "locZ");
        methodNMSAddEntity = ReflectionUtils.getMethod(classNMSWorldServer, "addEntity",
                                                       classNMSEntity, CreatureSpawnEvent.SpawnReason.class);
        methodAppendEntityCrashReport = ReflectionUtils
            .findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC,
                                   classCrashReportSystemDetails);
        methodCrashReportAppender = ReflectionUtils.findMethodFromProfile(classCrashReportSystemDetails,
                                                                          classCrashReportSystemDetails,
                                                                          Modifier.PUBLIC,
                                                                          String.class, Object.class);
        methodIsAir = ReflectionUtils.getMethodFullInheritance(classIBlockData, "isAir");
        methodNBTTagCompoundSet = ReflectionUtils.getMethod(classNBTTagCompound, "set", String.class, classNBTBase);

        methodNBTTagCompoundSetInt = ReflectionUtils.getMethod(classNBTTagCompound, "setInt",
                                                               String.class, int.class);
        methodNBTTagCompoundSetBoolean = ReflectionUtils.getMethod(classNBTTagCompound, "setBoolean",
                                                                   String.class, boolean.class);
        methodNBTTagCompoundSetFloat = ReflectionUtils.getMethod(classNBTTagCompound, "setFloat",
                                                                 String.class, float.class);
        methodNBTTagCompoundGetCompound = ReflectionUtils.getMethod(classNBTTagCompound, "getCompound", String.class);
        methodNBTTagCompoundGetInt = ReflectionUtils.getMethod(classNBTTagCompound, "getInt", String.class);
        methodNBTTagCompoundHasKeyOfType = ReflectionUtils.getMethod(classNBTTagCompound, "hasKeyOfType",
                                                                     String.class, int.class);
        methodIBlockDataSerializer = ReflectionUtils
            .findMethodFromProfile(classGameProfileSerializer, classNBTTagCompound,
                                   ReflectionUtils.getModifiers(Modifier.PUBLIC, Modifier.STATIC), classIBlockData);
        methodIBlockDataDeserializer = ReflectionUtils
            .findMethodFromProfile(classGameProfileSerializer, classIBlockData,
                                   ReflectionUtils.getModifiers(Modifier.PUBLIC, Modifier.STATIC),
                                   classNBTTagCompound);


        fieldHurtEntities = ReflectionUtils.getField(classEntityFallingBlock, getHurtEntitiesFieldName());
        fieldNoClip = ReflectionUtils.getField(classNMSEntity, getNoClipFieldName(), boolean.class);
        fieldTileEntityData = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, classNBTTagCompound);
        fieldTicksLived = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, int.class);
        fieldNMSWorld = ReflectionUtils.getField(classNMSEntity, Modifier.PUBLIC, classNMSWorld);
        fieldEnumMoveTypeSelf = ReflectionUtils.getEnumConstant(classEnumMoveType, 0);


        fieldsVec3D = ReflectionUtils
            .getFields(3, classVec3D, ReflectionUtils.getModifiers(Modifier.PUBLIC, Modifier.FINAL), double.class);
    }

    private @NotNull String getHurtEntitiesFieldName()
    {
        final String fieldName = getFieldName(methodHurtEntities, classEntityFallingBlock,
                                              MethodAdapterFirstIfMemberField::new);
        return Objects.requireNonNull(fieldName,
                                      "Failed to find name of HurtEntities variable in hurt entities method!");
    }

    private @NotNull String getNoClipFieldName()
    {
        final String fieldName = getFieldName(methodMove, classNMSEntity, MethodAdapterFirstIfMemberField::new);
        return Objects.requireNonNull(fieldName, "Failed to find name of noClip variable in move method!");
    }

    private @Nullable String getFieldName(@NotNull Method method, @NotNull Class<?> clz,
                                          @NotNull IFieldFinderCreator fieldFinderCreator)
    {

        final String className = clz.getName().replace('.', '/');
        final String classAsPath = className + ".class";
        final InputStream inputStream = Objects.requireNonNull(clz.getClassLoader().getResourceAsStream(classAsPath),
                                                               "Failed to get " + clz + " class resources.");

        final ClassReader classReader;
        try
        {
            classReader = new ClassReader(inputStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to construct ClassReader for class: " + clz, e);
        }
        final ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);

        final FieldFinderClassVisitor cv = new FieldFinderClassVisitor(classWriter, method, className,
                                                                       fieldFinderCreator);
        classReader.accept(cv, 0);
        return cv.getFieldName();
    }

    private static final class FieldFinderClassVisitor extends ClassVisitor
    {
        private final Type type;
        private final String fieldOwner;
        private final IFieldFinderCreator fieldFinderCreator;
        private FieldFinder methodAdapter;
        private static final boolean DEBUG = false;

        public FieldFinderClassVisitor(ClassWriter cw, Method m, String fieldOwner,
                                       IFieldFinderCreator fieldFinderCreator)
        {
            super(Opcodes.ASM9, cw);
            this.type = Type.getType(m);
            this.fieldOwner = fieldOwner;
            this.fieldFinderCreator = fieldFinderCreator;
        }

        public @Nullable String getFieldName()
        {
            return methodAdapter == null ? null : methodAdapter.getFoundFieldName();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (Type.getType(desc).equals(this.type))
            {
                methodAdapter = fieldFinderCreator.create(access, name, desc, signature, exceptions, mv, fieldOwner);
                mv = methodAdapter;
                if (DEBUG)
                    mv = getDebugVisitor(mv);
            }
            return mv;
        }

        private MethodVisitor getDebugVisitor(MethodVisitor mv)
        {
            Printer p = new Textifier(Opcodes.ASM9)
            {
                @Override
                public void visitMethodEnd()
                {
                    print(new PrintWriter(System.out));
                }
            };
            return new TraceMethodVisitor(mv, p);
        }
    }

    @FunctionalInterface
    private interface IFieldFinderCreator
    {
        FieldFinder create(int access, String name, String desc, String signature, String[] exceptions,
                           MethodVisitor mv, String fieldOwner);
    }

    private abstract static class FieldFinder extends MethodNode
    {
        public FieldFinder(int access, String name, String desc, String signature, String[] exceptions)
        {
            super(Opcodes.ASM9, access, name, desc, signature, exceptions);
        }

        abstract @Nullable String getFoundFieldName();
    }

    private static final class MethodAdapterFirstIfMemberField extends FieldFinder
    {
        private final String fieldOwner;
        private @Nullable String fieldName = null;

        public MethodAdapterFirstIfMemberField(int access, String name, String desc,
                                               String signature, String[] exceptions, MethodVisitor mv,
                                               String fieldOwner)
        {
            super(access, name, desc, signature, exceptions);
            this.mv = mv;
            this.fieldOwner = fieldOwner;
        }

        @Override
        public @Nullable String getFoundFieldName()
        {
            return fieldName;
        }

        @Override
        public void visitEnd()
        {
            for (AbstractInsnNode node : instructions)
            {
                if (node.getOpcode() != Opcodes.ALOAD
                    || ((VarInsnNode) node).var != 0)
                    continue;

                if (node.getNext() == null
                    || node.getNext().getOpcode() != Opcodes.GETFIELD)
                    continue;

                // Checks the invoked method name and signature
                FieldInsnNode next = (FieldInsnNode) node.getNext();
                if (!next.owner.equals(fieldOwner)
                    || !next.desc.equals("Z"))
                    continue;

                fieldName = next.name;
                break;
            }
            accept(mv);
        }
    }

    @Override
    protected void generateImpl()
        throws Exception
    {
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(classEntityFallingBlock,
                      ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .implement(CustomEntityFallingBlock.class, IGeneratedFallingBlockEntity.class)
            // TODO: Use full name
//            .name("GeneratedCustomEntityFallingBlock_" + this.mappingsVersion);
            .name("GeneratedCustomEntityFallingBlock");

        builder = addFields(builder);
        builder = addCTor(builder);
        builder = addSpawnMethod(builder);
        builder = addHurtEntitiesMethod(builder);
        builder = addGetBlockMethod(builder);
        builder = addLoadDataMethod(builder);
        builder = addSaveDataMethod(builder);
        builder = addAuxiliaryMethods(builder);
        builder = addTickMethod(builder);
        builder = addCrashReportMethod(builder);

        finishBuilder(builder, World.class, double.class, double.class, double.class, classIBlockData);
    }

    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> builder)
    {
        return builder
            .defineField(fieldTicksLived.getName(), fieldTicksLived.getType(), Visibility.PROTECTED)
            .defineField(fieldHurtEntities.getName(), fieldHurtEntities.getType(), Visibility.PROTECTED)
            .defineField(fieldNoClip.getName(), fieldNoClip.getType(), Visibility.PROTECTED)
            .defineField(fieldTileEntityData.getName(), fieldTileEntityData.getType(), Visibility.PROTECTED)
            .defineField("block", classIBlockData, Visibility.PRIVATE)
            .defineField("bukkitWorld", org.bukkit.World.class, Visibility.PRIVATE);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        final MethodCall worldCast = (MethodCall) invoke(methodGetNMSWorld)
            .onArgument(0).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(World.class, double.class, double.class, double.class, classIBlockData)
            .intercept(invoke(cTorNMSFallingBlockEntity).withMethodCall(worldCast).withArgument(1, 2, 3, 4).andThen(
                FieldAccessor.ofField("block").setsArgumentAt(4)).andThen(
                FieldAccessor.ofField("bukkitWorld").setsArgumentAt(0)).andThen(
                FieldAccessor.of(fieldTicksLived).setsValue(0)).andThen(
                FieldAccessor.of(fieldHurtEntities).setsValue(false)).andThen(
                FieldAccessor.of(fieldNoClip).setsValue(true)).andThen(
                invoke(methodSetNoGravity).with(true)).andThen(
                invoke(methodSetMot).with(0, 0, 0)).andThen(
                invoke(methodSetStartPos)
                    .withMethodCall(construct(cTorBlockPosition).withMethodCall(invoke(methodLocX))
                                                                .withMethodCall(invoke(methodLocY))
                                                                .withMethodCall(invoke(methodLocZ)))).andThen(
                invoke(named("spawn")))
            );
    }

    private DynamicType.Builder<?> addSpawnMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod("spawn", boolean.class, Visibility.PUBLIC)
            .intercept(invoke(methodNMSAddEntity)
                           .onField(fieldNMSWorld)
                           .with(MethodCall.ArgumentLoader.ForThisReference.Factory.INSTANCE)
                           .with(CreatureSpawnEvent.SpawnReason.CUSTOM)
                           .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
            );
    }

    private DynamicType.Builder<?> addHurtEntitiesMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod(methodHurtEntities.getName(), boolean.class, Visibility.PUBLIC)
            .withParameters(methodHurtEntities.getParameterTypes())
            .intercept(value(false));
    }

    private DynamicType.Builder<?> addCrashReportMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod(methodAppendEntityCrashReport.getName(), void.class, Visibility.PUBLIC)
            .withParameters(methodAppendEntityCrashReport.getParameterTypes())
            .intercept(invoke(methodAppendEntityCrashReport).onSuper().withArgument(0).andThen(
                invoke(methodCrashReportAppender).onArgument(0).with("Animated BigDoors block with state: ")
                                                 .withField("block")));
    }

    private DynamicType.Builder<?> addGetBlockMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod(methodGetBlock.getName(), classIBlockData, Visibility.PUBLIC)
            .intercept(FieldAccessor.ofField("block"));
    }

    public interface LoadDataDelegation
    {
        @RuntimeType
        void intercept(@This Object baseObject, @RuntimeType Object compound, boolean hasKey, String methodName);
    }

    private DynamicType.Builder<?> addLoadDataMethod(DynamicType.Builder<?> builder)
    {
        final String loadTileEntityDataName = methodLoadData.getName() + "$tileEntityData";
        final String tileEntityConditionalName = methodLoadData.getName() + "$conditional";

        builder = builder
            .defineMethod(loadTileEntityDataName, void.class, Visibility.PRIVATE)
            .withParameters(classNBTTagCompound)
            .intercept(invoke(methodNBTTagCompoundGetCompound).onArgument(0).with("TileEntityData")
                                                              .setsField(fieldTileEntityData));

        builder = builder
            .defineMethod(tileEntityConditionalName, void.class, Visibility.PRIVATE)
            .withParameters(Object.class, classNBTTagCompound, boolean.class, String.class)
            .intercept(MethodDelegation.to((LoadDataDelegation) (baseObject, compound, hasKey, methodName) ->
            {
                if (!hasKey)
                    return;
                try
                {
                    Method m = baseObject.getClass().getDeclaredMethod(methodName, classNBTTagCompound);
                    m.setAccessible(true);
                    m.invoke(baseObject, compound);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }, LoadDataDelegation.class));

        builder = builder
            .define(methodLoadData)
            .intercept(invoke(methodIBlockDataDeserializer)
                           .withMethodCall(invoke(methodNBTTagCompoundGetCompound).onArgument(0).with("BlockState"))
                           .setsField(named("block")).andThen(
                    invoke(methodNBTTagCompoundGetInt).onArgument(0).with("Time").setsField(fieldTicksLived)).andThen(
                    invoke(named(tileEntityConditionalName))
                        .withThis().withArgument(0)
                        .withMethodCall(invoke(methodNBTTagCompoundHasKeyOfType).onArgument(0)
                                                                                .with("TileEntityData", 10))
                        .with(loadTileEntityDataName))
            );

        return builder;
    }

    public interface SaveDataDelegation
    {
        @RuntimeType
        Object intercept(@This Object baseObject, @RuntimeType Object compound, @RuntimeType Object block,
                         String methodName);
    }

    private DynamicType.Builder<?> addSaveDataMethod(DynamicType.Builder<?> builder)
    {
        final String saveTileEntityDataName = methodSaveData.getName() + "$tileEntityData";
        final String tileEntityConditionalName = methodSaveData.getName() + "$conditional";

        builder = builder
            .defineMethod(saveTileEntityDataName, classNBTTagCompound, Visibility.PRIVATE)
            .withParameters(classNBTTagCompound)
            .intercept(invoke(methodNBTTagCompoundSet).onArgument(0).with("TileEntityData")
                                                      .withField(fieldTileEntityData.getName()).andThen(
                    FixedValue.argument(0)));

        builder = builder
            .defineMethod(tileEntityConditionalName, classNBTTagCompound, Visibility.PRIVATE)
            .withParameters(Object.class, classNBTTagCompound, classNBTTagCompound, String.class)
            .intercept(MethodDelegation.to((SaveDataDelegation) (baseObject, base, append, methodName) ->
            {
                if (append == null)
                    return base;
                try
                {
                    Method m = baseObject.getClass().getDeclaredMethod(methodName, classNBTTagCompound);
                    m.setAccessible(true);
                    return m.invoke(baseObject, base);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return null;
                }
            }, SaveDataDelegation.class));

        return builder
            .define(methodSaveData)
            .intercept(invoke(methodNBTTagCompoundSet)
                           .onArgument(0).with("BlockState")
                           .withMethodCall(invoke(methodIBlockDataSerializer).withField("block")).andThen(
                    invoke(methodNBTTagCompoundSetInt).onArgument(0).with("Time")
                                                      .withField(fieldTicksLived.getName())).andThen(
                    invoke(methodNBTTagCompoundSetBoolean).onArgument(0).with("DropItem", false)).andThen(
                    invoke(methodNBTTagCompoundSetBoolean).onArgument(0).with("HurtEntities")
                                                          .withField(fieldHurtEntities.getName())).andThen(
                    invoke(methodNBTTagCompoundSetFloat).onArgument(0).with("FallHurtAmount", 0.0f)).andThen(
                    invoke(methodNBTTagCompoundSetInt).onArgument(0).with("FallHurtMax", 0)).andThen(
                    invoke(named(tileEntityConditionalName)).withThis().withArgument(0)
                                                            .withField(fieldTileEntityData.getName())
                                                            .with(saveTileEntityDataName)).andThen(
                    FixedValue.argument(0)));
    }

    public interface IGeneratedFallingBlockEntity
    {
        void generated$die();

        boolean generated$isAir();

        void generated$move();

        int generated$getTicksLived();

        void generated$setTicksLived(int val);

        void generated$updateMot();

        double locY();
    }

    public interface MultiplyVec3D
    {
        @RuntimeType
        Object intercept(@RuntimeType Object vec3d, double x, double y, double z);
    }

    private DynamicType.Builder<?> addAuxiliaryMethods(DynamicType.Builder<?> builder)
    {
        builder = builder
            .defineMethod("generated$multiplyVec", classVec3D, Visibility.PRIVATE)
            .withParameters(classVec3D, double.class, double.class, double.class)
            .intercept(MethodDelegation.to((MultiplyVec3D) (vec, x, y, z) ->
            {
                try
                {
                    return cTorVec3D.newInstance((double) fieldsVec3D.get(0).get(vec) * x,
                                                 (double) fieldsVec3D.get(1).get(vec) * y,
                                                 (double) fieldsVec3D.get(2).get(vec) * z);
                }
                catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
                {
                    e.printStackTrace();
                    return vec;
                }
            }, MultiplyVec3D.class));

        builder = builder.method(named("generated$die")).intercept(invoke(named("die")).onSuper());
        builder = builder.method(named("igenerated$sAir")).intercept(invoke(methodIsAir).onField("block"));
        builder = builder
            .method(named("generated$move").and(ElementMatchers.takesArguments(Collections.emptyList())))
            .intercept(invoke(methodMove)
                           .with(value(fieldEnumMoveTypeSelf))
                           .withMethodCall(invoke(methodGetMot))
                           .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        builder = builder.method(named("generated$getTicksLived"))
                         .intercept(FieldAccessor.ofField(fieldTicksLived.getName()));
        builder = builder.method(named("generated$setTicksLived"))
                         .intercept(FieldAccessor.of(fieldTicksLived).setsArgumentAt(0));
        builder = builder
            .method(named("generated$updateMot"))
            .intercept(invoke(methodSetMotVec)
                           .withMethodCall(invoke(named("generated$multiplyVec"))
                                               .withMethodCall(invoke(methodGetMot))
                                               .with(0.9800000190734863D, 1.0D, 0.9800000190734863D)));
        return builder;
    }

    public interface ITickMethodDelegate
    {
        void intercept(IGeneratedFallingBlockEntity entity);
    }

    private DynamicType.Builder<?> addTickMethod(DynamicType.Builder<?> builder)
    {
        builder = builder
            .defineMethod("generated$tick", void.class, Visibility.PRIVATE)
            .withParameters(IGeneratedFallingBlockEntity.class)
            .intercept(MethodDelegation.to((ITickMethodDelegate) entity ->
            {
                if (entity.generated$isAir())
                {
                    entity.generated$die();
                    return;
                }

                entity.generated$move();

                double locY = entity.locY();
                int ticks = entity.generated$getTicksLived() + 1;
                entity.generated$setTicksLived(ticks);

                if (++ticks > 100 && (locY < 1 || locY > 256) || ticks > 12000)
                    entity.generated$die();

                entity.generated$updateMot();
            }, ITickMethodDelegate.class));

        builder = builder
            .define(methodTick).intercept(invoke(named("generated$tick"))
                                              .withThis().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
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
