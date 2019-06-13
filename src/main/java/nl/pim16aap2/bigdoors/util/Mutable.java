package nl.pim16aap2.bigdoors.util;

/**
 * Class used to wrap immutable objects and as such make them mutable.
 *
 * @author Pim
 */
public class Mutable<T>
{
    private T val;

    public Mutable(T val)
    {
        this.val = val;
    }

    public T getVal()
    {
        return val;
    }

    public void setVal(T val)
    {
        this.val = val;
    }
}
