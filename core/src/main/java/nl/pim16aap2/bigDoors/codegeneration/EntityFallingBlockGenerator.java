package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

public class EntityFallingBlockGenerator
{
    private final @NotNull String mappingsVersion;

    private Class<?> classEntityFallingBlock;
    private Class<?> classIBlockData;
    private Class<?> classCraftWorld;
    private Class<?> classNMSWorld;
    private Class<?> classNMSWorldServer;
    private Class<?> classBlockPosition;
    private Class<?> classNMSEntity;
    private Class<?> classVec3D;
    private Class<?> classEnumMoveType;
    private Class<?> classNBTTagCompound;

    private Constructor<?> cTorNMSFallingBlockEntity;
    private Constructor<?> cTorBlockPosition;

    private Method methodTick;
    private Method methodGetNMSClass;
    private Method methodSetPosition;
    private Method methodSetNoGravity;
    private Method methodSetMot;
    private Method methodHurtEntities;
    private Method methodSaveData;
    private Method methodLoadData;
    private Method methodGetBlock;
    private Method methodSetStartPos;
    private Method methodLocX;
    private Method methodLocY;
    private Method methodLocZ;
    private Method methodNMSAddEntity;

    private Method testMethod;

    private Field fieldNoClip;
    private Field fieldTileEntityData;
    private Field fieldTicksLived;
    private Field fieldHurtEntities;
    private Field fieldNMSWorld;

    /**
     * The public boolean fields are dropItem and hurtEntities (as of 1.17.0).
     */
    private List<Field> fieldsBooleans;

    public EntityFallingBlockGenerator(@NotNull String mappingsVersion)
        throws Exception
    {
        this.mappingsVersion = mappingsVersion;

        try
        {
            init();
        }
        catch (NullPointerException | IllegalStateException | IOException e)
        {
            throw new Exception("Failed to find all components required to generate an EntityFallingBlock!", e);
        }
    }

    private void init()
        throws IOException
    {
        classEntityFallingBlock = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "EntityFallingBlock",
                                                                 "net.minecraft.world.entity.item.EntityFallingBlock");
        classNBTTagCompound = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "NBTTagCompound",
                                                             "net.minecraft.nbt.NBTTagCompound");
        classIBlockData = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "IBlockData",
                                                         "net.minecraft.world.level.block.state.IBlockData");
        classCraftWorld = ReflectionUtils.findFirstClass(ReflectionUtils.CRAFT_BASE + "CraftWorld");
        classEnumMoveType = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "EnumMoveType",
                                                           "net.minecraft.world.entity.EnumMoveType");
        classVec3D = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "Vec3D",
                                                    "net.minecraft.world.phys.Vec3D");
        classNMSWorld = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "World",
                                                       "net.minecraft.world.level.World");
        classNMSWorldServer = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "WorldServer",
                                                             "net.minecraft.server.level.WorldServer");
        classNMSEntity = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "Entity",
                                                        "net.minecraft.world.entity.Entity");
        classBlockPosition = ReflectionUtils.findFirstClass(ReflectionUtils.NMS_BASE + "BlockPosition",
                                                            "net.minecraft.core.BlockPosition");

        cTorNMSFallingBlockEntity = ReflectionUtils.findCTor(classEntityFallingBlock, classNMSWorld, double.class,
                                                             double.class, double.class, classIBlockData);
        cTorBlockPosition = ReflectionUtils.findCTor(classBlockPosition, double.class, double.class, double.class);


        methodGetNMSClass = ReflectionUtils.getMethod(classCraftWorld, "getHandle");
        methodTick = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC);
        methodSetPosition = ReflectionUtils.getMethod(classNMSEntity, "setPosition",
                                                      double.class, double.class, double.class);
        methodSetNoGravity = ReflectionUtils.getMethod(classNMSEntity, "setNoGravity", boolean.class);
        methodSetMot = ReflectionUtils.getMethod(classNMSEntity, "setMot",
                                                 double.class, double.class, double.class);
        methodHurtEntities = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, boolean.class,
                                                                   Modifier.PUBLIC,
                                                                   float.class, float.class, null);
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
        testMethod = ReflectionUtils.getMethod(classNMSWorld, "a", classBlockPosition, classNMSEntity);
//        public boolean a(BlockPosition blockposition, Entity entity)

        fieldsBooleans = ReflectionUtils.getFields(classEntityFallingBlock, Modifier.PUBLIC, boolean.class);
        if (fieldsBooleans.size() < 2)
            throw new IllegalStateException("Failed to find expected number of booleans in class \"" +
                                                classEntityFallingBlock + "\"! Found: " + fieldsBooleans.size() +
                                                ", but expected at least 2!");

        fieldHurtEntities = ReflectionUtils.getField(classEntityFallingBlock, getHurtEntitiesFieldName());
        fieldNoClip = ReflectionUtils.getField(classNMSEntity, getNoClipFieldName(), boolean.class);
        fieldTileEntityData = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, classNBTTagCompound);
        fieldTicksLived = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, int.class);
        fieldNMSWorld = ReflectionUtils.getField(classNMSEntity, Modifier.PUBLIC, classNMSWorld);
    }

    private @NotNull String getHurtEntitiesFieldName()
        throws IOException
    {
        final String fieldName = getFieldName(methodHurtEntities, classEntityFallingBlock,
                                              MethodAdapterFirstIfMemberField::new);
        return Objects.requireNonNull(fieldName,
                                      "Failed to find name of HurtEntities variable in hurt entities method!");
    }

    private @NotNull String getNoClipFieldName()
        throws IOException
    {
        final Method m = ReflectionUtils.getMethod(true, classNMSEntity, "move", classEnumMoveType, classVec3D);
        final String fieldName = getFieldName(m, classNMSEntity, MethodAdapterFirstIfMemberField::new);
        return Objects.requireNonNull(fieldName, "Failed to find name of noClip variable in move method!");
    }

    private @Nullable String getFieldName(@NotNull Method method, @NotNull Class<?> clz,
                                          @NotNull IFieldFinderCreator fieldFinderCreator)
        throws IOException
    {
        final String className = clz.getName().replace('.', '/');
        final String classAsPath = className + ".class";
        final InputStream inputStream = Objects.requireNonNull(clz.getClassLoader().getResourceAsStream(classAsPath),
                                                               "Failed to get " + clz + " class resources.");

        final ClassReader classReader = new ClassReader(inputStream);
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

    public Class<?> generate()
        throws IOException
    {
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(classEntityFallingBlock, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .implement(CustomEntityFallingBlock.class)
            // TODO: Use full name
//            .name("CustomEntityFallingBlock$" + this.mappingsVersion);
            .name("CustomEntityFallingBlock$generated");

        builder = addFields(builder);
        builder = addSpawnMethod(builder);
        builder = addCTor(builder);

        DynamicType.Unloaded<?> unloaded = builder.make();

        // TODO: Remove this
        unloaded.saveIn(new File("/home/pim/Documents/workspace/BigDoors/generated"));

        if (ConfigLoader.DEBUG)
            unloaded.saveIn(new File(BigDoors.get().getDataFolder(), "generated"));

        return unloaded.load(BigDoors.get().getClass().getClassLoader()).getLoaded();
    }

    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> builder)
    {
        return builder
            .define(fieldTicksLived)
            .define(fieldHurtEntities)
            .define(fieldNoClip)
            .define(fieldTileEntityData)
            .defineField("block", classIBlockData, Visibility.PRIVATE)
            .defineField("bukkitWorld", org.bukkit.World.class, Visibility.PRIVATE)
            .defineField("worldServer", classNMSWorldServer, Visibility.PRIVATE);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(classNMSWorld, double.class, double.class, double.class, classIBlockData, World.class,
                            classNMSWorldServer)
            .intercept(MethodCall.invoke(cTorNMSFallingBlockEntity).withArgument(0, 1, 2, 3, 4)
                                 .andThen(FieldAccessor.ofField("block").setsArgumentAt(4))
                                 .andThen(FieldAccessor.ofField("bukkitWorld").setsArgumentAt(5))
                                 .andThen(FieldAccessor.of(fieldTicksLived).setsValue(0))
                                 .andThen(FieldAccessor.of(fieldHurtEntities).setsValue(false))
                                 .andThen(FieldAccessor.of(fieldNoClip).setsValue(true))
                                 .andThen(FieldAccessor.ofField("worldServer").setsArgumentAt(6))
                                 .andThen(MethodCall.invoke(methodSetStartPos).withMethodCall(
                                     MethodCall.construct(cTorBlockPosition)
                                               .withMethodCall(MethodCall.invoke(methodLocX))
                                               .withMethodCall(MethodCall.invoke(methodLocY))
                                               .withMethodCall(MethodCall.invoke(methodLocZ))))
                                 .andThen(MethodCall.invoke(ElementMatchers.named("spawn")))
            );
    }

    private DynamicType.Builder<?> addSpawnMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod("spawn", boolean.class, Visibility.PUBLIC)
            .intercept(MethodCall.invoke(methodNMSAddEntity)
                                 .onField("worldServer")
                                 .with(MethodCall.ArgumentLoader.ForThisReference.Factory.INSTANCE)
                                 .with(CreatureSpawnEvent.SpawnReason.CUSTOM));
    }
}
