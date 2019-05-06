package nl.pim16aap2.bigdoors.nms;

import org.bukkit.Location;

import nl.pim16aap2.bigdoors.util.RotateDirection;

public interface NMSBlock_Vall
{
    public void   rotateBlock(RotateDirection rotDir);
    public void   putBlock(Location loc);
    public void   rotateBlockUpDown(boolean NS);
    public void   rotateCylindrical(RotateDirection rotDir);
    @Override
    public String toString();
}
