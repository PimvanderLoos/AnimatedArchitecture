package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.util.RotateDirection;

public class GNLHorizontalRotNorth implements GetNewLocation
{
    @SuppressWarnings("unused")
    private int xMin, xMax, zMin, zMax;
    private World world;
    private RotateDirection rotDir;

    public GNLHorizontalRotNorth(World world, int xMin, int xMax, int zMin, int zMax, RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world  = world;
        this.xMin   = xMin;
        this.xMax   = xMax;
        this.zMin   = zMin;
        this.zMax   = zMax;
    }

    @Override
    public Location getNewLocation(double radius, double xPos, double yPos, double zPos)
    {
        Location oldPos = new Location(world, xPos, yPos, zPos);
        Location newPos = oldPos;

        newPos.setX(oldPos.getX() + (rotDir == RotateDirection.CLOCKWISE ? radius : -radius));
        newPos.setY(oldPos.getY());
        newPos.setZ(zMax);
        return newPos;
    }
}
