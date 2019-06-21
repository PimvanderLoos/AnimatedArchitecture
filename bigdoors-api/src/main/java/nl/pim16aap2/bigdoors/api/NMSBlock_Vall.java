package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;

public interface NMSBlock_Vall
{
    public boolean canRotate();

    public void rotateBlock(RotateDirection rotDir);

    public void putBlock(Location loc);

    @Override
    public String toString();

    public void deleteOriginalBlock();
}
