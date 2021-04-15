package nl.pim16aap2.bigdoors.extensions;


import lombok.NonNull;

import java.net.URL;
import java.net.URLClassLoader;

public class DoorTypeClassLoader extends URLClassLoader
{
    public DoorTypeClassLoader(final @NonNull URL[] urls, final @NonNull ClassLoader parent)
    {
        super(urls, parent);
    }

    public DoorTypeClassLoader(final @NonNull ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    @Override
    protected void addURL(URL url)
    {
        super.addURL(url);
    }
}
