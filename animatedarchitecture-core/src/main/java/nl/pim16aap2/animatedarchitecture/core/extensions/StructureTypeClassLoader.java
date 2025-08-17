package nl.pim16aap2.animatedarchitecture.core.extensions;


import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@CustomLog
final class StructureTypeClassLoader extends URLClassLoader implements IStructureTypeClassLoader
{
    public StructureTypeClassLoader(ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    public boolean loadJar(Path file)
    {
        log.atTrace().log("Trying to load jar '%s' into StructureTypeClassLoader.", file);
        try
        {
            addURL(file.toUri().toURL());
        }
        catch (Exception e)
        {
            log.atWarn().withCause(e).log("Failed to load jar '%s' into StructureTypeClassLoader.", file);
            return false;
        }
        return true;
    }

    @Override
    public StructureType loadStructureTypeClass(String mainClass)
        throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException
    {
        final Class<?> typeClass = loadClass(mainClass);
        final Method getter = typeClass.getDeclaredMethod("get");
        return (StructureType) getter.invoke(null);
    }
}
