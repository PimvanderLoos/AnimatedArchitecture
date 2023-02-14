package nl.pim16aap2.bigdoors.core.extensions;


import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.structures.StructureType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@Flogger//
final class StructureTypeClassLoader extends URLClassLoader implements IStructureTypeClassLoader
{
    public StructureTypeClassLoader(ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    public boolean loadJar(Path file)
    {
        log.atFinest().log("Trying to load jar '%s' into StructureTypeClassLoader.", file);
        try
        {
            addURL(file.toUri().toURL());
        }
        catch (Exception e)
        {
            log.atWarning().withCause(e).log();
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
