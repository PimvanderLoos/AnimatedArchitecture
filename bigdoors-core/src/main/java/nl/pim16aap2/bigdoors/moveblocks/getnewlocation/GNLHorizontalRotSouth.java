package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

public class GNLHorizontalRotSouth implements IGetNewLocation
{
    private final int zMin;
    private final @NotNull IPWorld world;
    private final @NotNull RotateDirection rotDir;
    private final @NotNull IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

    public GNLHorizontalRotSouth(final @NotNull IPWorld world, final int xMin, final int xMax, final int zMin,
                                 final int zMax, final @NotNull RotateDirection rotDir)
    {
        this.rotDir = rotDir;
        this.world = world;
        this.zMin = zMin;
    }

    @Override
    public @NotNull IPLocation getNewLocation(final double radius, final double xPos, final double yPos,
                                              final double zPos)
    {
        IPLocation newPos = locationFactory.create(world, xPos, yPos, zPos);
        newPos.setX(newPos.getX() + (rotDir == RotateDirection.CLOCKWISE ? -radius : radius));
        newPos.setZ(zMin);
        return newPos;
    }
}
