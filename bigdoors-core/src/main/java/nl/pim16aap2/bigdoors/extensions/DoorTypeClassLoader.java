package nl.pim16aap2.bigdoors.extensions;


import java.net.URL;
import java.net.URLClassLoader;

final class DoorTypeClassLoader extends URLClassLoader
{
    public DoorTypeClassLoader(ClassLoader parent)
    {
        super(new URL[]{}, parent);
    }

    // The override isn't actually useless, as it allows us to access it.
    @SuppressWarnings("PMD.UselessOverridingMethod")
    @Override
    protected void addURL(URL url)
    {
        super.addURL(url);
    }
}
