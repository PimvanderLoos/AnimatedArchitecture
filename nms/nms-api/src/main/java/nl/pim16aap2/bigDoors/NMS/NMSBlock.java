package nl.pim16aap2.bigDoors.NMS;

import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import org.bukkit.Location;

public interface NMSBlock
{
    void rotateBlock(RotateDirection rotDir);
    void putBlock(Location loc);
    void rotateBlockUpDown(boolean ns);

    default void rotateVerticallyInDirection(DoorDirection openDirection)
    {
    }

    void rotateCylindrical(RotateDirection rotDir);
    boolean canRotate();

    void deleteOriginalBlock(boolean applyPhysics);
    @Override
    String toString();
}
