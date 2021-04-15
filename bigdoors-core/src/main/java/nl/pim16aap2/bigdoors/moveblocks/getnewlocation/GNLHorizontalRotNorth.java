package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class GNLHorizontalRotNorth implements IGetNewLocation
{
    private final int zMax;
private final @NonNull IPWorld world;
private final @NonNull RotateDirection rotDir;
private final @NonNull IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

    public GNLHorizontalRotNorth(final @NonNull IPWorld world, final int xMin, final int xMax, final int zMin,
                                 final int zMax, final @NonNull RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world = world;
        this.zMax = zMax;
    }

    @Override
    public @NonNull IPLocation getNewLocation(final double radius, final double xPos, final double yPos,
                                              final double zPos)
    {
        IPLocation newPos = locationFactory.create(world, xPos, yPos, zPos);
        newPos.setX(newPos.getX() + (rotDir == RotateDirection.CLOCKWISE ? radius : -radius));
        newPos.setZ(zMax);
        return newPos;
    }
}
