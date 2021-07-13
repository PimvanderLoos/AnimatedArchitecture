package nl.pim16aap2.bigDoors.util;

public class Pair<T1, T2>
{
    public T1 first;
    public T2 second;

    public Pair(T1 first, T2 second)
    {
        this.first = first;
        this.second = second;
    }

    public Pair()
    {
        this(null, null);
    }

    @Override
    public String toString()
    {
        return String.format("Pair(%s, %s)", first, second);
    }
}
