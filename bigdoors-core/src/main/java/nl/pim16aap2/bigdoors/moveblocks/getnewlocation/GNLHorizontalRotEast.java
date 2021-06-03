package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

public class GNLHorizontalRotEast implements IGetNewLocation
{
    private final int xMin;
    private final @NotNull IPWorld world;

    private final @NotNull RotateDirection rotDir;

    private final @NotNull IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

    public GNLHorizontalRotEast(final @NotNull IPWorld world, final int xMin, final int xMax, final int zMin,
                                final int zMax, final @NotNull RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world = world;
        this.xMin = xMin;
    }

    @Override
    public @NotNull IPLocation getNewLocation(final double radius, final double xPos, final double yPos,
                                              final double zPos)
    {
        IPLocation newPos = locationFactory.create(world, xPos, yPos, zPos);
        newPos.setX(xMin);
        newPos.setZ(newPos.getZ() + (rotDir == RotateDirection.CLOCKWISE ? radius : -radius));
        return newPos;
    }
}
