package nl.pim16aap2.bigDoors.reflection.asm;

import nl.pim16aap2.bigDoors.util.Constants;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Executable;

public final class Debugging
{
    private Debugging()
    {
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

    public static MethodVisitor getDebugMethodVisitor(MethodVisitor methodVisitor, PrintWriter printWriter)
    {
        return new TraceMethodVisitor(methodVisitor, new Textifier(Constants.ASM_API_VER)
        {
            @Override
            public void visitMethodEnd()
            {
                print(printWriter);
                printWriter.flush();
            }
        });
    }

    public static MethodVisitor getDebugMethodVisitor(MethodVisitor methodVisitor)
    {
        return getDebugMethodVisitor(methodVisitor, new PrintWriter(System.out));
    }
}
