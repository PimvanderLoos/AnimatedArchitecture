package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;

public class TestEconomyManager implements IEconomyManager
{
    public OptionalDouble price = OptionalDouble.empty();
    public boolean buyDoor = false;
    public boolean isEconomyEnabled = false;

    @Override
    public boolean buyDoor(final @NotNull IPPlayer player, final @NotNull IPWorld world, final @NotNull DoorType type,
                           final int blockCount)
    {
        return buyDoor;
    }

    @Override
    public OptionalDouble getPrice(final @NotNull DoorType type, final int blockCount)
    {
        return price;
    }

    @Override
    public boolean isEconomyEnabled()
    {
        return isEconomyEnabled;
    }
}
