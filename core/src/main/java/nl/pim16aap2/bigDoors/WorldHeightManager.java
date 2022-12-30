package nl.pim16aap2.bigDoors;

import nl.pim16aap2.bigDoors.reflection.ReflectionBuilder;
import nl.pim16aap2.bigDoors.util.WorldHeightLimits;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class WorldHeightManager
{
    private static final MinWorldHeightFinder MIN_WORLD_HEIGHT_FINDER = getMinWorldHeightFinder();

    private final Map<World, WorldHeightLimits> limitsMap = new HashMap<>();

    /**
     * Gets the world height limits for a specific world.
     *
     * @param world The world for which to determine the min/max world heights.
     * @return The world height limits for a specific world.
     */
    public WorldHeightLimits getWorldHeightLimits(World world)
    {
        return limitsMap.computeIfAbsent(world, WorldHeightManager::calculateWorldHeightLimits);
    }

    private static WorldHeightLimits calculateWorldHeightLimits(World world)
    {
        final int max = world.getMaxHeight();
        final int min = MIN_WORLD_HEIGHT_FINDER.find(world);
        return new WorldHeightLimits(min, max);
    }

    private static MinWorldHeightFinder getMinWorldHeightFinder()
    {
        if (!BigDoors.getMCVersion().isAtLeast(BigDoors.MCVersion.v1_18_R1))
            return legacy -> 0;

        try
        {
            final Method methodMinWorldHeight = ReflectionBuilder
                .findMethod().inClass(World.class).withName("getMinHeight").checkSuperClasses().checkInterfaces().get();
            return world -> WorldHeightManager.getMinWorldHeight(methodMinWorldHeight, world);
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
            Bukkit.getLogger().severe("Failed to find getMinHeight method! " +
                                          "All worlds are considered to have a min height of 0!");
            return fallback -> 0;
        }
    }

    private static int getMinWorldHeight(Method method, World world)
    {
        try
        {
            return (int) method.invoke(world);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            Bukkit.getLogger().severe("Failed to determine minimum world height for world: " + world);
            e.printStackTrace();
            return 0; // 0 is always going to be a safe fallback value.
        }
    }

    @FunctionalInterface
    private interface MinWorldHeightFinder
    {
        int find(World world);
    }
}
