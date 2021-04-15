package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class GNLHorizontalRotWest implements IGetNewLocation
{
    private final int xMax;
    private final @NonNull IPWorld world;
    private final @NonNull RotateDirection rotDir;
    private final @NonNull IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

    public GNLHorizontalRotWest(final @NonNull IPWorld world, final int xMin, final int xMax, final int zMin,
                                final int zMax, final @NonNull RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world = world;
        this.xMax = xMax;
    }


    @Override
    public @NonNull IPLocation getNewLocation(final double radius, final double xPos, final double yPos,
                                              final double zPos)
    {
        IPLocation newPos = locationFactory.create(world, xPos, yPos, zPos);
        newPos.setX(xMax);
        newPos.setZ(newPos.getZ() + (rotDir == RotateDirection.CLOCKWISE ? -radius : radius));
        return newPos;
    }
}
