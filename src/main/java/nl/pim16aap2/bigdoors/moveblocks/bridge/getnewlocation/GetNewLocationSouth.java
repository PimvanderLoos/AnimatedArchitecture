package nl.pim16aap2.bigdoors.moveblocks.bridge.getnewlocation;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;

@SuppressWarnings("unused")
public class GetNewLocationSouth implements GetNewLocation
{
    private World world;
    private RotateDirection upDown;
    private MyBlockFace openDirection;
    private int xMin, yMin, zMin;
    private int xMax, yMax, zMax;

    public GetNewLocationSouth(World world, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax,
        RotateDirection upDown, MyBlockFace openDirection)
    {
        this.openDirection = openDirection;
        this.upDown = upDown;
        this.world = world;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
    }

    @Override
    public Location getNewLocation(double radius, double xPos, double yPos, double zPos)
    {
        Location newPos = null;

        if (upDown == RotateDirection.UP)
            newPos = new Location(world, xPos, yMin + radius, zMax);
        else if (openDirection.equals(MyBlockFace.NORTH))
            newPos = new Location(world, xPos, yMin, zPos - radius);
        else if (openDirection.equals(MyBlockFace.SOUTH))
            newPos = new Location(world, xPos, yMin, zPos + radius);
        return newPos;
    }
}
