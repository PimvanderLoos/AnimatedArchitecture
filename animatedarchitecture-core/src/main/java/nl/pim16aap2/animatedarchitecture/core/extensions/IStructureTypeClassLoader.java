package nl.pim16aap2.animatedarchitecture.core.extensions;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public interface IStructureTypeClassLoader
{
    /**
     * Attempts to load a jar.
     *
     * @param file
     *     The jar file.
     * @return True if the jar loaded successfully.
     */
    boolean loadJar(Path file);

    StructureType loadStructureTypeClass(String mainClass)
        throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException;
}
