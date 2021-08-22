package nl.pim16aap2.bigdoors.extensions;


import java.net.URL;
import java.net.URLClassLoader;

class DoorTypeClassLoader extends URLClassLoader
{
    public DoorTypeClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }

    public DoorTypeClassLoader(ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    protected void addURL(URL url)
    {
        super.addURL(url);
    }
}
