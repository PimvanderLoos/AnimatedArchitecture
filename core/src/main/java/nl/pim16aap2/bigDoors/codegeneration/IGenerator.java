package nl.pim16aap2.bigDoors.codegeneration;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

interface IGenerator
{
    @NotNull IGenerator generate()
        throws Exception;

    @Nullable Class<?> getGeneratedClass();

    @Nullable Constructor<?> getGeneratedConstructor();
}
