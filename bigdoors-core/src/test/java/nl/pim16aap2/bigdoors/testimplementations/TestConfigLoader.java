package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

public class TestConfigLoader implements IConfigLoader
{
    public OptionalInt maxdoorCount = OptionalInt.empty();
    public OptionalInt maxDoorSize = OptionalInt.empty();
    public OptionalInt maxPowerBlockDistance = OptionalInt.empty();
    public OptionalInt maxBlocksToMove = OptionalInt.empty();

    @Override
    public boolean debug()
    {
        return false;
    }

    @Override
    @NotNull
    public String flagFormula()
    {
        return null;
    }

    @Override
    public int coolDown()
    {
        return 0;
    }

    @Override
    public boolean allowStats()
    {
        return false;
    }

    @Override
    public OptionalInt maxDoorSize()
    {
        return maxDoorSize;
    }

    @Override
    public int cacheTimeout()
    {
        return 0;
    }

    @Override
    public String languageFile()
    {
        return null;
    }

    @Override
    public OptionalInt maxDoorCount()
    {
        return maxdoorCount;
    }

    @Override
    public OptionalInt maxPowerBlockDistance()
    {
        return maxPowerBlockDistance;
    }

    @Override
    public OptionalInt maxBlocksToMove()
    {
        return maxBlocksToMove;
    }

    @Override
    public boolean autoDLUpdate()
    {
        return false;
    }

    @Override
    public long downloadDelay()
    {
        return 0;
    }

    @Override
    public boolean enableRedstone()
    {
        return false;
    }

    @Override
    public boolean checkForUpdates()
    {
        return false;
    }

    @Override
    public String getPrice(final @NotNull DoorType type)
    {
        return null;
    }

    @Override
    public double getMultiplier(final @NotNull DoorType type)
    {
        return 0;
    }

    @Override
    public boolean consoleLogging()
    {
        return false;
    }

    @Override
    public void restart()
    {
    }

    @Override
    public void shutdown()
    {
    }
}
