package nl.pim16aap2.bigdoors.extensions;


import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;

class DoorTypeClassLoader extends URLClassLoader
{
    public DoorTypeClassLoader(final @NotNull URL[] urls, final @NotNull ClassLoader parent)
    {
        super(urls, parent);
    }

    public DoorTypeClassLoader(final @NotNull ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    protected void addURL(URL url)
    {
        super.addURL(url);
    }
}
