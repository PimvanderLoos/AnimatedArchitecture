package nl.pim16aap2.bigDoors.reflection;

import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A collection of utility methods for Bukkit-related reflection operations.
 */
@SuppressWarnings("unused")
public final class BukkitReflectionUtil
{
    private static final Class<?> classNMSPlayer;
    private static final Class<?> classPlayerConnection;
    private static final Class<?> classVec3D;
    private static final Class<?> classCraftPlayer;

    private static final Field fieldPlayerConnection;
    private static final Field[] fieldsFlyingCounters;

    private static final Method methodGetHandle;

    static
    {
        final String craftBase = "org.bukkit.craftbukkit." + BigDoors.get().getPackageVersion() + ".";

        classNMSPlayer = ReflectionBuilder.findClass("net.minecraft.server.level.EntityPlayer").get();
        classPlayerConnection = ReflectionBuilder.findClass("net.minecraft.server.network.PlayerConnection").get();
        classCraftPlayer = ReflectionBuilder.findClass(craftBase + "entity.CraftPlayer").get();
        classVec3D = ReflectionBuilder.findClass("net.minecraft.world.phys.Vec3D").get();

        fieldPlayerConnection = ReflectionBuilder.findField()
                                                 .inClass(classNMSPlayer).ofType(classPlayerConnection).get();
        try
        {
            fieldsFlyingCounters = findFlyingCounters(classPlayerConnection);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to find flying counters!", e);
        }

        methodGetHandle = ReflectionBuilder.findMethod().inClass(classCraftPlayer).findMultiple()
                                           .withName("getHandle").withReturnType(classNMSPlayer).atMost(1).get().get(0);
    }

    private BukkitReflectionUtil(){}

    /**
     * Resets the flying counters for a player.
     * <p>
     * The flying counters cause a player to be kicked for flying after 80 ticks of hovering (unless flying is enabled).
     *
     * @param player The player whose flying counters to reset.
     */
    public static void resetFlyingCounters(Player player)
    {
        try
        {
            final Object playerConnection = fieldPlayerConnection.get(methodGetHandle.invoke(player));
            fieldsFlyingCounters[0].setInt(playerConnection, 0);
            fieldsFlyingCounters[1].setInt(playerConnection, 0);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException("Failed to get handle of player!", e);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to reset flying counters!", e);
        }
    }

    @SuppressWarnings({"SameParameterValue"})
    private static Field[] findFlyingCounters(Class<?> classPlayerConnection)
        throws NoSuchFieldException
    {
        final Field[] fields = new Field[2];

        switch (BigDoors.getMCVersion())
        {
            case v1_19_R1:
                // It is F&H for 1.19.0 and G&I for 1.19.1... Annoying
                fields[0] = classPlayerConnection.getDeclaredField("F");
                if (fields[0].getType() == int.class)
                {
                    fields[1] = classPlayerConnection.getDeclaredField("H");
                }
                else
                {
                    fields[0] = classPlayerConnection.getDeclaredField("G");
                    fields[1] = classPlayerConnection.getDeclaredField("I");
                }
            case v1_19_R2:
                fields[0] = classPlayerConnection.getDeclaredField("H");
                fields[1] = classPlayerConnection.getDeclaredField("J");
            default:
                discoverFlyingCounters(classPlayerConnection, fields);
        }

        for (final Field field : fields)
            field.setAccessible(true);
        return fields;
    }

    private static void discoverFlyingCounters(Class<?> classPlayerConnection, Field[] fields)
    {
        /*
        The general variable order between 1.11 and 1.19.3 (6 years) has been this:
        ...
        Vec3D ...;
        ...
        boolean related-to-first-target;
        int [firstTarget];
        boolean related-to-second-target;
        int [secondTarget];
        ...

        We use this pattern to find the two target integers.
         */
        boolean foundVec = false;
        int foundCount = 0;
        final Field[] declaredFields = classPlayerConnection.getDeclaredFields();
        for (int idx = 0; idx < declaredFields.length; ++idx)
        {
            final Field field = declaredFields[idx];
            final Class<?> type = field.getType();
            if (type == classVec3D)
            {
                foundVec = true;
                continue;
            }
            if (!foundVec)
                continue;
            if (type == boolean.class)
            {
                final Field peek = declaredFields[++idx];
                if (peek.getType() != int.class)
                    throw new IllegalStateException(
                        "Expected field type 'int' but found: '" + peek.getType().getName() + "'");
                fields[foundCount++] = peek;
                if (foundCount == 2)
                    break;
            }
        }
    }

    /**
     * Empty init method used to ensure initialization of static stuff.
     */
    public static void init() {}
}
