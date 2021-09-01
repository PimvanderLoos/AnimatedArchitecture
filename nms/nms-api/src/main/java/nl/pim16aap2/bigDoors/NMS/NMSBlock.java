package nl.pim16aap2.bigDoors.NMS;

import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import org.bukkit.Location;

public interface NMSBlock
{
    public void rotateBlock(RotateDirection rotDir);
    public void putBlock(Location loc);
    public void rotateBlockUpDown(boolean ns);

    default void rotateBlockUpDown(RotateDirection upDown, DoorDirection openDirection)
    {
    }

    public void rotateCylindrical(RotateDirection rotDir);
    public boolean canRotate();

    public void deleteOriginalBlock();
    @Override
    public String toString();
}
