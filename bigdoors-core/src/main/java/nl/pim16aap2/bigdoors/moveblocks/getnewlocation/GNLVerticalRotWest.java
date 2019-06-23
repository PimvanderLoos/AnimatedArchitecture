package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;

@SuppressWarnings("unused")
public class GNLVerticalRotWest implements GetNewLocation
{
    private World world;
    private MyBlockFace upDown;
    private RotateDirection openDirection;
    private int xMin, yMin, zMin;
    private int xMax, yMax, zMax;

    public GNLVerticalRotWest(World world, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax,
                               MyBlockFace upDown, RotateDirection openDirection)
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

        if (upDown == MyBlockFace.UP)
            newPos = new Location(world, xMin, yMin + radius, zPos);
        else if (openDirection.equals(RotateDirection.WEST))
            newPos = new Location(world, xPos - radius, yMin, zPos);
        else if (openDirection.equals(RotateDirection.EAST))
            newPos = new Location(world, xPos + radius, yMin, zPos);
        return newPos;
    }
}
