package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.bukkit.Bukkit;
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
    private static final String NMS_BASE =
        "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
    private static final String CRAFT_BASE =
        "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";

    private Class<?> classEntityFallingBlock;
    private Class<?> classIBlockData;
    private Class<?> classCraftWorld;
    private Class<?> classNMSWorld;
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

    private Field fieldNoClip;
    private Field fieldTileEntityData;
    private Field fieldTicksLived;
    private Field fieldHurtEntities;

    /**
     * The public boolean fields are dropItem and hurtEntities (as of 1.17.0).
     */
    private List<Field> fieldsBooleans;

    public EntityFallingBlockGenerator()
        throws Exception
    {
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
        classEntityFallingBlock = ReflectionUtils.findFirstClass(NMS_BASE + "EntityFallingBlock",
                                                                 "net.minecraft.world.entity.item.EntityFallingBlock");
        classNBTTagCompound = ReflectionUtils.findFirstClass(NMS_BASE + "NBTTagCompound",
                                                             "net.minecraft.nbt.NBTTagCompound");
        classIBlockData = ReflectionUtils.findFirstClass(NMS_BASE + "IBlockData",
                                                         "net.minecraft.world.level.block.state.IBlockData");
        classCraftWorld = ReflectionUtils.findFirstClass(CRAFT_BASE + "CraftWorld");
        classEnumMoveType = ReflectionUtils.findFirstClass(NMS_BASE + "EnumMoveType",
                                                           "net.minecraft.world.entity.EnumMoveType");
        classVec3D = ReflectionUtils.findFirstClass(NMS_BASE + "Vec3D", "net.minecraft.world.phys.Vec3D");
        classNMSWorld = ReflectionUtils.findFirstClass(NMS_BASE + "World", "net.minecraft.world.level.World");
        classNMSEntity = ReflectionUtils.findFirstClass(NMS_BASE + "Entity", "net.minecraft.world.entity.Entity");
        classBlockPosition = ReflectionUtils.findFirstClass(NMS_BASE + "BlockPosition",
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


        fieldsBooleans = ReflectionUtils.getFields(classEntityFallingBlock, Modifier.PUBLIC, boolean.class);
        if (fieldsBooleans.size() < 2)
            throw new IllegalStateException("Failed to find expected number of booleans in class \"" +
                                                classEntityFallingBlock + "\"! Found: " + fieldsBooleans.size() +
                                                ", but expected at least 2!");

        fieldHurtEntities = ReflectionUtils.getField(classEntityFallingBlock, getHurtEntitiesFieldName());
        fieldNoClip = ReflectionUtils.getField(classNMSEntity, getNoClipFieldName(), boolean.class);
        fieldTileEntityData = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, classNBTTagCompound);
        fieldTicksLived = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, int.class);

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
        new ByteBuddy()
            .subclass(CustomEntityFallingBlock.class)
            .name("GeneratedCustomEntityFallingBlock")
            .make()
            .saveIn(new File(BigDoors.get().getDataFolder(), "Generated.class"));
//            .
        // TODO: Actually generate everything.


        return null;
    }
}
