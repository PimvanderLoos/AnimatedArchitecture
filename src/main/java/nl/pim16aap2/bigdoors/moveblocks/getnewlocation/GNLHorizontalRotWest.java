package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.util.RotateDirection;

public class GNLHorizontalRotWest implements GetNewLocation
{
    @SuppressWarnings("unused")
    private int xMin, xMax, zMin, zMax;
    private World world;
    private RotateDirection rotDir;

    public GNLHorizontalRotWest(World world, int xMin, int xMax, int zMin, int zMax, RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world  = world;
        this.xMin   = xMin;
        this.xMax   = xMax;
        this.zMin   = zMin;
        this.zMax   = zMax;
    }

    public GNLHorizontalRotWest()
    {}

    @Override
    public Location getNewLocation(double radius, double xPos, double yPos, double zPos)
    {
        Location oldPos = new Location(world, xPos, yPos, zPos);
        Location newPos = oldPos;

        newPos.setX(xMax);
        newPos.setY(oldPos.getY());
        newPos.setZ(oldPos.getZ() + (rotDir == RotateDirection.CLOCKWISE ? -radius : radius));
        return newPos;
    }
}
