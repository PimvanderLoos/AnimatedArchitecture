package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.util.ReflectionUtils;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;

/**
 * Represents a class that analyzes the original EntityFallingBlock class as found in the server.
 * <p>
 * This class uses ASM to discover aspects of the class that cannot be discovered using reflection.
 *
 * @author Pim
 */
class EntityFallingBlockClassAnalyzer
{
    private static final int ASM_API_VER = Opcodes.ASM9;

    /**
     * Retrieves the hurtEntities field in the current mapping.
     *
     * @return The hurtEntities field.
     *
     * @throws IOException If a problem occurs during reading.
     */
    @NotNull Field getHurtEntitiesField()
        throws IOException
    {
        final String fieldName =
            Objects.requireNonNull(getFieldName(methodHurtEntities, classEntityFallingBlock,
                                                MethodAdapterFindFirstAccessedBooleanMemberField::new),
                                   "Failed to find name of HurtEntities variable in hurt entities method!");
        return ReflectionUtils.getField(classEntityFallingBlock, fieldName);
    }

    /**
     * Retrieves the noClip field in the current mapping.
     *
     * @return The noClip field.
     *
     * @throws IOException If a problem occurs during reading.
     */
    @NotNull Field getNoClipField()
        throws IOException
    {
        final String fieldName = Objects.requireNonNull(
            getFieldName(methodMove, classNMSEntity, MethodAdapterFindFirstAccessedBooleanMemberField::new),
            "Failed to find name of noClip variable in move method!");
        return ReflectionUtils.getField(classNMSEntity, fieldName, boolean.class);
    }

    /**
     * Finds the name of a field used in a method in a class.
     *
     * @param method             The method to analyze.
     * @param clz                The parent class of the method.
     * @param fieldFinderCreator The {@link IFieldFinderCreator} that is used to create an {@link FieldFinder} to do the
     *                           actual work to find the name of the field.
     * @return The name of the field if one could be found by the {@link FieldFinder}.
     *
     * @throws IOException If a problem occurs during reading.
     */
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

    /**
     * Represents a {@link ClassVisitor} that is used to find fields in methods.
     */
    private static final class FieldFinderClassVisitor extends ClassVisitor
    {
        private final @NotNull Type type;
        private final @NotNull String fieldOwner;
        private final @NotNull IFieldFinderCreator fieldFinderCreator;
        private @Nullable FieldFinder fieldFinder;
        private static final boolean DEBUG = false;

        /**
         * See {@link ClassVisitor#ClassVisitor(int, ClassVisitor)}.
         *
         * @param method             The method to analyze.
         * @param fieldOwner         The name of the class that should own the field. This should be the fully qualified
         *                           name (as retrieved by {@link Class#getName()}) of the class with the packages
         *                           separated by '/'. For example "java/util/List".
         * @param fieldFinderCreator The creator of an {@link FieldFinder} for the provided method.
         */
        public FieldFinderClassVisitor(@NotNull ClassWriter cw, @NotNull Method method, @NotNull String fieldOwner,
                                       @NotNull IFieldFinderCreator fieldFinderCreator)
        {
            super(ASM_API_VER, cw);
            this.type = Type.getType(method);
            this.fieldOwner = fieldOwner;
            this.fieldFinderCreator = fieldFinderCreator;
        }

        /**
         * Retrieves the result of {@link FieldFinder#getFoundFieldName()} if a {@link FieldFinder} was constructed.
         *
         * @return The name of the field that was found or null if no field could be found.
         */
        public @Nullable String getFieldName()
        {
            return fieldFinder == null ? null : fieldFinder.getFoundFieldName();
        }

        @Override
        public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc,
                                         @Nullable String signature, @Nullable String[] exceptions)
        {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (Type.getType(desc).equals(this.type))
            {
                fieldFinder = fieldFinderCreator.create(access, name, desc, signature, exceptions, fieldOwner);
                mv = fieldFinder;
                if (DEBUG)
                    mv = getDebugVisitor(mv);
            }
            return mv;
        }

        /**
         * Retrieves a {@link MethodVisitor} that prints the bytecode of the method.
         * <p>
         * This method wraps an existing {@link MethodVisitor} and can therefore be used on top of any other {@link
         * MethodVisitor}.
         *
         * @param mv The {@link MethodVisitor} for the method for which to print the bytecode.
         * @return The input {@link MethodVisitor} wrapped in a new {@link MethodVisitor} that prints the bytecode of
         * the method.
         */
        private MethodVisitor getDebugVisitor(MethodVisitor mv)
        {
            final Printer p = new Textifier(ASM_API_VER)
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

    /**
     * Represents a creator for a {@link FieldFinder}.
     */
    @FunctionalInterface
    private interface IFieldFinderCreator
    {
        /**
         * Creates a new {@link FieldFinder}.
         * <p>
         * See {@link MethodNode#MethodNode()}.
         *
         * @param fieldOwner The name of the class that should own the field this finder will look for. This should be
         *                   the fully qualified name (as retrieved by {@link Class#getName()}) of the class with the
         *                   packages separated by '/'. For example "java/util/List".
         */
        @NotNull FieldFinder create(int access, @NotNull String name, @NotNull String desc, @Nullable String signature,
                                    @Nullable String[] exceptions, @NotNull String fieldOwner);
    }

    /**
     * Represents a MethodNode that is used to extract the name of a field as used in a method.
     */
    private abstract static class FieldFinder extends MethodNode
    {
        /**
         * See {@link MethodNode#MethodNode()}.
         */
        public FieldFinder(int access, @NotNull String name, @NotNull String desc, @Nullable String signature,
                           @Nullable String[] exceptions)
        {
            super(ASM_API_VER, access, name, desc, signature, exceptions);
        }

        /**
         * Gets the name of the field that was found.
         *
         * @return The name of the field that was found or null if no field could be found matching the requirements.
         */
        abstract @Nullable String getFoundFieldName();
    }

    /**
     * Implementation of {@link FieldFinder} that retrieves the name of the first boolean member field being accessed.
     * <p>
     * Note that this implementation ONLY looks for a field from the first argument (Specifically, we only look for the
     * {@code aload_0} for the method). So, for member methods, this will be the {@code this} reference (may be
     * implicit). For static methods, however, this will be the first method argument if this argument is a reference
     * type. You can think of this as always taking argument 0 with {@code this.fun(a, b)} being {@code
     * MyClass.fun(this, a, b)}.
     * <p>
     * For example, this class would retrieve the name "hiddenValue" in the following examples:
     * <pre>{@code
     * void doSomething() {
     *     if(this.hiddenValue) return;
     *     doSomethingElse();
     * }}</pre>
     * <pre>{@code
     * void doSomething() {
     *     if(this.performCheck()) { // Not a boolean member field, so ignored.
     *         this.hiddenValue = false;
     *     }
     *     doSomethingElse();
     * }}</pre>
     * <pre>{@code
     * void doSomething() {
     *     if(this.performCheck()) return; // Not a boolean member field, so ignored.
     *     if(this.hiddenValue) return;
     *     doSomethingElse();
     * }}</pre>
     */
    private static final class MethodAdapterFindFirstAccessedBooleanMemberField extends FieldFinder
    {
        private final String fieldOwner;
        private @Nullable String fieldName = null;

        /**
         * See {@link FieldFinder#FieldFinder(int, String, String, String, String[])}.
         *
         * @param fieldOwner The name of the class that should own the field. This should be the fully qualified name
         *                   (as retrieved by {@link Class#getName()}) of the class with the packages separated by '/'.
         *                   For example "java/util/List".
         */
        public MethodAdapterFindFirstAccessedBooleanMemberField(int access, @NotNull String name, @NotNull String desc,
                                                                @Nullable String signature,
                                                                @Nullable String[] exceptions,
                                                                @NotNull String fieldOwner)
        {
            super(access, name, desc, signature, exceptions);
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
                // We only look for member variables of the current class,
                // so only look for aload_0 (get first argument: 'this' in non-static methods).
                if (node.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) node).var != 0)
                    continue;

                // We want to get the actual field being used
                if (node.getNext() == null || node.getNext().getOpcode() != Opcodes.GETFIELD)
                    continue;

                final FieldInsnNode next = (FieldInsnNode) node.getNext();
                // Ensure that the field being accessed is
                // 1) Owned by the specified fieldOwner (i.e. exists in the correct class).
                // 2) Is of type "Z" (i.e. boolean).
                if (!next.owner.equals(fieldOwner) || !next.desc.equals("Z"))
                    continue;

                // Gotcha!
                fieldName = next.name;
                break;
            }
        }
    }
}
