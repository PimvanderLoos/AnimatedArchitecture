package nl.pim16aap2.bigDoors.reflection.asm;

import nl.pim16aap2.bigDoors.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

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
    {}

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

    /**
     * Processes an executable.
     *
     * @param executable            The executable (method or constructor) to process.
     * @param methodVisitorAppender The method visitor to append to the existing one for the target executable.
     * @param methodVisitorReplacer The method visitor to replace the existing one of the target executable with.
     * @return The created instance of the method visitor replacer, if it exists.
     * @throws IOException When the class could not be read.
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

    /**
     * Represents a {@link ClassVisitor} that is used to find fields in methods.
     */
    private static final class MyClassVisitor<T extends MethodVisitor> extends ClassVisitor
    {
        private final @NotNull Type type;
        private final @NotNull Executable executable;
        private final @Nullable IMethodVisitorAppender methodVisitorAppender;
        private final @Nullable IMethodVisitorReplacer<T> methodVisitorReplacer;

        private @Nullable T replacementVisitor;
        private boolean hasVisited = false;

        public MyClassVisitor(@NotNull ClassWriter cw, @NotNull Executable executable,
                              @Nullable IMethodVisitorAppender methodVisitorAppender,
                              @Nullable IMethodVisitorReplacer<T> methodVisitorReplacer)
        {
            super(Constants.ASM_API_VER, cw);
            this.type = executable instanceof Constructor<?> ?
                        Type.getType((Constructor<?>) executable) :
                        Type.getType((Method) executable);
            this.executable = executable;
            this.methodVisitorAppender = methodVisitorAppender;
            this.methodVisitorReplacer = methodVisitorReplacer;
        }

        @Override
        public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc,
                                         @Nullable String signature, @Nullable String[] exceptions)
        {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

            if (!hasVisited && Type.getType(desc).equals(this.type) && name.equals(this.executable.getName()))
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

    @FunctionalInterface
    public interface IMethodAnalyzer
    {
        void analyze(int access, @NotNull String name, @NotNull String desc,
                     @Nullable String signature, @Nullable String[] exceptions);
    }
}
