package nl.pim16aap2.bigdoors.extensions;


import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.logging.Level;

@Flogger//
final class MovableTypeClassLoader extends URLClassLoader implements IMovableTypeClassLoader
{
    public MovableTypeClassLoader(ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    public boolean loadJar(Path file)
    {
        log.at(Level.FINEST).log("Trying to load jar '%s' into MovableTypeClassLoader.", file);
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
    public MovableType loadMovableTypeClass(String mainClass)
        throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException
    {
        final Class<?> typeClass = loadClass(mainClass);
        final Method getter = typeClass.getDeclaredMethod("get");
        return (MovableType) getter.invoke(null);
    }
}
