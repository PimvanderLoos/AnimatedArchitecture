package nl.pim16aap2.bigDoors.reflection.asm;

import nl.pim16aap2.bigDoors.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Objects;

public final class ASMUtil
{
    private ASMUtil()
    {
    }

    public static String getClassName(Class<?> clz)
    {
        return clz.getName().replace('.', '/');
    }

    public static ClassReader getClassReader(Class<?> clz)
        throws IOException
    {
        final String classAsPath = getClassName(clz) + ".class";
        final InputStream inputStream = Objects.requireNonNull(clz.getClassLoader().getResourceAsStream(classAsPath),
                                                               "Failed to get " + clz + " class resources.");
        return new ClassReader(inputStream);
    }

    public static boolean executableContainsMethodCall(Executable executable, Method method)
    {
        try
        {
            return Objects.requireNonNull(
                ASMUtil.processMethod(executable, null, (a, n, d, s, e) ->
                    MethodInCallFinder.create(a, n, d, s, e, method.getDeclaringClass(), Type.getType(method), 1))
            ).methods.length > 0;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to analyze executable: " + executable.toGenericString(), e);
        }
    }

    public static String getMethodNameFromMethodCall(Executable executable, @Nullable Class<?> ownerClass,
                                                     Class<?> returnType, Class<?>... parameters)
    {
        return getMethodNamesFromMethodCall(executable, 1, ownerClass, returnType, parameters)[0];
    }

    public static String[] getMethodNamesFromMethodCall(Executable executable, int limit, @Nullable Class<?> ownerClass,
                                                        Class<?> returnType, Class<?>... parameters)
    {
        final Type[] paramTypes = new Type[parameters.length];
        for (int idx = 0; idx < parameters.length; ++idx)
            paramTypes[idx] = Type.getType(parameters[idx]);
        final Type type = Type.getMethodType(Type.getType(returnType), paramTypes);

        try
        {
            return Objects.requireNonNull(
                ASMUtil.processMethod(executable, null, (a, n, d, s, e) ->
                    MethodInCallFinder.create(a, n, d, s, e, ownerClass, type, limit))
            ).methods;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to find method call in method of type: " + type, e);
        }
    }

    /**
     * Processes an executable.
     *
     * @param executable
     *     The executable (method or constructor) to process.
     * @param methodVisitorAppender
     *     The method visitor to append to the existing one for the target executable.
     * @param methodVisitorReplacer
     *     The method visitor to replace the existing one of the target executable with.
     * @return The created instance of the method visitor replacer, if it exists.
     *
     * @throws IOException
     *     When the class could not be read.
     */
    public static @Nullable <T extends MethodVisitor> T processMethod(
        Executable executable, @Nullable IMethodVisitorAppender methodVisitorAppender,
        @Nullable IMethodVisitorReplacer<T> methodVisitorReplacer)
        throws IOException
    {
        final ClassReader cr = getClassReader(executable.getDeclaringClass());
        final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        final MyClassVisitor<T> cv = new MyClassVisitor<>(cw, executable, methodVisitorAppender, methodVisitorReplacer);
        cr.accept(cv, 0);

        return cv.replacementVisitor;
    }

    @FunctionalInterface
    public interface IMethodVisitorAppender
    {
        MethodVisitor append(MethodVisitor methodVisitor);
    }

    @FunctionalInterface
    public interface IMethodVisitorReplacer<T extends MethodVisitor>
    {
        /**
         * Creates a new MethodVisitor.
         *
         * @return A new MethodVisitor.
         */
        T create(int access, @NotNull String name, @NotNull String desc,
                 @Nullable String signature, @Nullable String[] exceptions);
    }

    /**
     * Represents a {@link MethodNode} that is used to retrieve the names of methods called in a method that match a
     * specific signature.
     *
     * @author Pim
     */
    private static final class MethodInCallFinder extends MethodNode
    {
        private final @Nullable String ownerClassName;
        private final Type type;
        private final int limit;
        private final String[] methods;

        public MethodInCallFinder(int access, @NotNull String name, @NotNull String desc,
                                  @Nullable String signature, @Nullable String[] exceptions,
                                  @Nullable Class<?> ownerClass, Type type, int limit)
        {
            super(Constants.ASM_API_VER, access, name, desc, signature, exceptions);
            this.type = type;
            this.limit = limit;
            ownerClassName = ownerClass == null ? null : ASMUtil.getClassName(ownerClass);
            methods = new String[limit];
        }

        public static MethodInCallFinder create(int access, @NotNull String name, @NotNull String desc,
                                                @Nullable String signature, @Nullable String[] exceptions,
                                                @Nullable Class<?> ownerClass, Type type, int limit)
        {
            return new MethodInCallFinder(access, name, desc, signature, exceptions, ownerClass, type, limit);
        }

        @Override
        public void visitEnd()
        {
            int idx = 0;
            for (AbstractInsnNode node : instructions)
            {
                if (idx >= limit)
                    break;

                if (!(node instanceof MethodInsnNode))
                    continue;

                if (node.getOpcode() != Opcodes.INVOKEVIRTUAL)
                    continue;

                final MethodInsnNode invoke = (MethodInsnNode) node;

                if (ownerClassName != null && !invoke.owner.equals(ownerClassName))
                    continue;

                if (!Type.getType(invoke.desc).equals(type))
                    continue;

                this.methods[idx++] = invoke.name;
            }
        }
    }

    /**
     * Represents a {@link ClassVisitor} that is used to find fields in methods.
     */
    private static final class MyClassVisitor<T extends MethodVisitor> extends ClassVisitor
    {
        private final @NotNull Type type;
        private final @NotNull String executableName;
        private final @Nullable IMethodVisitorAppender methodVisitorAppender;
        private final @Nullable IMethodVisitorReplacer<T> methodVisitorReplacer;

        private @Nullable T replacementVisitor;
        private boolean hasVisited = false;

        public MyClassVisitor(@NotNull ClassWriter cw, @NotNull Executable executable,
                              @Nullable IMethodVisitorAppender methodVisitorAppender,
                              @Nullable IMethodVisitorReplacer<T> methodVisitorReplacer)
        {
            super(Constants.ASM_API_VER, cw);
            this.methodVisitorAppender = methodVisitorAppender;
            this.methodVisitorReplacer = methodVisitorReplacer;

            if (executable instanceof Constructor<?>)
            {
                this.type = Type.getType((Constructor<?>) executable);
                this.executableName = "<init>";
            }
            else
            {
                this.type = Type.getType((Method) executable);
                this.executableName = executable.getName();
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc,
                                         @Nullable String signature, @Nullable String[] exceptions)
        {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (!hasVisited && Type.getType(desc).equals(this.type) && name.equals(this.executableName))
            {
                if (methodVisitorReplacer != null)
                    mv = replacementVisitor = methodVisitorReplacer.create(access, name, desc, signature, exceptions);
                if (methodVisitorAppender != null)
                    mv = methodVisitorAppender.append(mv);
                hasVisited = true;
            }

            return mv;
        }
    }
}
