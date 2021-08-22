package nl.pim16aap2.bigDoors;

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
        try
        {
            // World#getMinHeight() doesn't exist in 1.11 (our base version), but it does exist in later versions.
            //noinspection JavaReflectionMemberAccess
            Method methodMinWorldHeight = World.class.getDeclaredMethod("getMinHeight");
            return world -> WorldHeightManager.getMinWorldHeight(methodMinWorldHeight, world);
        }
        catch (NoSuchMethodException e)
        {
            return ignored -> 0;
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
