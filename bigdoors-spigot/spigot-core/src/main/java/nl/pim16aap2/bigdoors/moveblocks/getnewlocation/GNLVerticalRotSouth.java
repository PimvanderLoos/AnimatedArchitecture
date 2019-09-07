package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GNLVerticalRotSouth implements IGetNewLocation
{
    private final IPWorld world;
    private final PBlockFace upDown;
    private final RotateDirection openDirection;
    private final int yMin, zMax;
    private final IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

    public GNLVerticalRotSouth(final @NotNull IPWorld world, final int xMin, final int xMax, final int yMin,
                               final int yMax, final int zMin, final int zMax, final @NotNull PBlockFace upDown,
                               final @NotNull RotateDirection openDirection)
    {
        this.openDirection = openDirection;
        this.upDown = upDown;
        this.world = world;
        this.yMin = yMin;
        this.zMax = zMax;
    }

    @NotNull
    @Override
    public IPLocation getNewLocation(final double radius, final double xPos, final double yPos, final double zPos)
    {
        IPLocation newPos = null;

        if (upDown == PBlockFace.UP)
            newPos = locationFactory.create(world, xPos, yMin + radius, zMax);
        else if (openDirection.equals(RotateDirection.NORTH))
            newPos = locationFactory.create(world, xPos, yMin, zPos - radius);
        else if (openDirection.equals(RotateDirection.SOUTH))
            newPos = locationFactory.create(world, xPos, yMin, zPos + radius);
        return newPos;
    }
}
