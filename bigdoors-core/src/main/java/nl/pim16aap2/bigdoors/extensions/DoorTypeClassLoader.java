package nl.pim16aap2.bigdoors.extensions;


import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.doortypes.DoorType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.logging.Level;

@Flogger//
final class DoorTypeClassLoader extends URLClassLoader implements IDoorTypeClassLoader
{
    public DoorTypeClassLoader(ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    public boolean loadJar(Path file)
    {
        log.at(Level.FINEST).log("Trying to load jar '%s' into DoorTypeClassLoader.", file);
        try
        {
            addURL(file.toUri().toURL());
        }
        catch (Exception e)
        {
            log.at(Level.WARNING).withCause(e).log();
            return false;
        }
        return true;
    }

    @Override
    public DoorType loadDoorTypeClass(String mainClass)
        throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException
    {
        final Class<?> typeClass = loadClass(mainClass);
        final Method getter = typeClass.getDeclaredMethod("get");
        return (DoorType) getter.invoke(null);
    }
}
