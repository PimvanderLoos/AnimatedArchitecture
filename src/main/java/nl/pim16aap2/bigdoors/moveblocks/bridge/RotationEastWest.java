package nl.pim16aap2.bigdoors.moveblocks.bridge;

import org.bukkit.World;

import nl.pim16aap2.bigdoors.util.RotateDirection;

@SuppressWarnings("unused")
public class RotationEastWest implements RotationFormulae
{
    private int xMin, xMax, zMin, zMax;
    private RotateDirection     rotDir;
    private World                world;

    public RotationEastWest(World world, int xMin, int xMax, int zMin, int zMax, RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world  = world;
        this.xMin   = xMin;
        this.xMax   = xMax;
        this.zMin   = zMin;
        this.zMax   = zMax;
    }

    public RotationEastWest()
    {}

}
