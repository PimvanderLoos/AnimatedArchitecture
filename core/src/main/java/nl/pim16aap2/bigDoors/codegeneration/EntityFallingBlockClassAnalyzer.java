package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionBuilder;
import nl.pim16aap2.bigDoors.reflection.asm.ASMUtil;
import nl.pim16aap2.bigDoors.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.Nullable;
import java.io.IOException;
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
            Objects.requireNonNull(getFieldName(methodHurtEntities, classEntityFallingBlock),
                                   "Failed to find name of HurtEntities variable in hurt entities method!");
        return ReflectionBuilder.findField().inClass(classEntityFallingBlock).withName(fieldName).get();
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
        final String fieldName = Objects.requireNonNull(getFieldName(methodMove, classNMSEntity),
                                                        "Failed to find name of noClip variable in move method!");
        return ReflectionBuilder.findField().inClass(classNMSEntity).withName(fieldName).ofType(boolean.class).get();
    }

    /**
     * Finds the name of a field used in a method in a class.
     *
     * @param method             The method to analyze.
     * @param clz                The parent class of the method.
     * @return The name of the field if one could be found by the {@link FieldFinder}.
     *
     * @throws IOException If a problem occurs during reading.
     */
    private @Nullable String getFieldName(@NotNull Method method, @NotNull Class<?> clz)
        throws IOException
    {
        final FieldFinder fieldFinder =
            ASMUtil.processMethod(method, null, (a, n, d, s, e) -> FieldFinder.create(a, n, d, s, e, clz));
        if (fieldFinder == null)
            throw new IllegalStateException("Failed to create location method analyzer using method: " +
                                                method + " in class: " + clz.getName());
        return fieldFinder.fieldName;
    }

    /**
     * Implementation of {@link MethodNode} that retrieves the name of the first boolean member field being accessed.
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
    private static final class FieldFinder extends MethodNode
    {
        private final String fieldOwner;

        private @Nullable String fieldName;

        public FieldFinder(int access, @NotNull String name, @NotNull String desc, @Nullable String signature,
                           @Nullable String[] exceptions, Class<?> fieldOwner)
        {
            super(Constants.ASM_API_VER, access, name, desc, signature, exceptions);
            this.fieldOwner = ASMUtil.getClassName(fieldOwner);
        }

        public static FieldFinder create(int access, @NotNull String name, @NotNull String desc,
                                         @Nullable String signature, @Nullable String[] exceptions, Class<?> fieldOwner)
        {
            return new FieldFinder(access, name, desc, signature, exceptions, fieldOwner);
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
