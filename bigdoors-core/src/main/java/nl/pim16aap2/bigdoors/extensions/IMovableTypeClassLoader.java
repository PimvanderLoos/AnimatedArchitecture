package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.movabletypes.MovableType;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public interface IMovableTypeClassLoader
{
    /**
     * Attempts to load a jar.
     *
     * @param file
     *     The jar file.
     * @return True if the jar loaded successfully.
     */
    boolean loadJar(Path file);

    MovableType loadMovableTypeClass(String mainClass)
        throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException;
}
