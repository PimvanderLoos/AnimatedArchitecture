package nl.pim16aap2.bigdoors.nms;

import org.bukkit.Location;

import nl.pim16aap2.bigdoors.util.RotateDirection;

public interface NMSBlock_Vall
{
    public boolean canRotate();

    public void rotateBlock(RotateDirection rotDir);

    public void putBlock(Location loc);

    @Override
    public String toString();

    public void deleteOriginalBlock();
}
