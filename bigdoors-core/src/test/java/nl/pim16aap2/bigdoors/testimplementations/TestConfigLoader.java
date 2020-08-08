package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

public class TestConfigLoader implements IConfigLoader
{
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
    public boolean dbBackup()
    {
        return true;
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
    public int maxDoorSize()
    {
        return 0;
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
    public int maxdoorCount()
    {
        return 0;
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
