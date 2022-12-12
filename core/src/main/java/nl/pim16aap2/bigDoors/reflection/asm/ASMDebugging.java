package nl.pim16aap2.bigDoors.reflection.asm;

import nl.pim16aap2.bigDoors.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceAnnotationVisitor;
import org.objectweb.asm.util.TraceFieldVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Executable;

@SuppressWarnings("unused")
public final class ASMDebugging
{
    private ASMDebugging()
    {
    }

    public static void printClassBytecode(Class<?> clz)
    {
        try
        {
            final PrintWriter pw = new PrintWriter(System.out);
            pw.println("Going to print bytecode for class: '" + clz + "'\n");
            final int debugTypes = DebugType.ANNOTATION | DebugType.METHOD | DebugType.FIELD;

            final ClassReader cr = ASMUtil.getClassReader(clz);
            final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            final DebugClassVisitor cv = new DebugClassVisitor(cw, debugTypes, pw);
            cr.accept(cv, 0);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to print bytecode for class '" + clz + "'", e);
        }
    }

    public static void printMethodBytecode(Executable executable)
        throws IOException
    {
        printMethodBytecode(executable, new PrintWriter(System.out));
    }

    /**
     * Prints the bytecode of a method.
     *
     * @param executable  The executable (method or constructor) whose bytecode to print.
     * @param printWriter The print writer to write the bytecode to.
     * @throws IOException When the class could not be read.
     */
    public static void printMethodBytecode(Executable executable, PrintWriter printWriter)
        throws IOException
    {
        ASMUtil.processMethod(executable, mv -> getDebugMethodVisitor(mv, printWriter), null);
    }

    private static MethodVisitor getDebugMethodVisitor(MethodVisitor visitor, PrintWriter printWriter)
    {
        return new TraceMethodVisitor(visitor, new Textifier(Constants.ASM_API_VER)
        {
            @Override
            public void visitMethodEnd()
            {
                print(printWriter);
                printWriter.flush();
            }
        });
    }

    private static FieldVisitor getDebugFieldVisitor(FieldVisitor visitor, PrintWriter printWriter)
    {
        return new TraceFieldVisitor(visitor, new Textifier(Constants.ASM_API_VER)
        {
            @Override
            public void visitFieldEnd()
            {
                print(printWriter);
                printWriter.flush();
            }
        });
    }

    private static AnnotationVisitor getDebugAnnotationVisitor(AnnotationVisitor visitor, PrintWriter printWriter)
    {
        return new TraceAnnotationVisitor(visitor, new Textifier(Constants.ASM_API_VER)
        {
            @Override
            public void visitAnnotationEnd()
            {
                print(printWriter);
                printWriter.flush();
            }
        });
    }

    private static final class DebugClassVisitor extends ClassVisitor
    {
        private final int debugTypes;
        private final PrintWriter printWriter;

        DebugClassVisitor(@NotNull ClassWriter cw, int debugTypes, PrintWriter printWriter)
        {
            super(Constants.ASM_API_VER, cw);
            this.debugTypes = debugTypes;
            this.printWriter = printWriter;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            final AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if ((debugTypes & DebugType.ANNOTATION) > 0)
                return getDebugAnnotationVisitor(av, printWriter);
            return av;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
        {
            final FieldVisitor fv = super.visitField(access, name, desc, signature, value);
            if ((debugTypes & DebugType.FIELD) > 0)
                return getDebugFieldVisitor(fv, printWriter);
            return fv;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ((debugTypes & DebugType.METHOD) > 0)
                return getDebugMethodVisitor(mv, printWriter);
            return mv;
        }
    }

    public static final class DebugType
    {
        public static final int METHOD          = 1;
        public static final int FIELD           = 1 << 1;
        public static final int ANNOTATION      = 1 << 2;
    }
}
