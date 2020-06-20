package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

public class TestConfigLoader implements IConfigLoader
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean debug()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String flagFormula()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dbBackup()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int coolDown()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean allowStats()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int maxDoorSize()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cacheTimeout()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String languageFile()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int maxdoorCount()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean autoDLUpdate()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long downloadDelay()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean enableRedstone()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkForUpdates()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrice(final @NotNull DoorType type)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMultiplier(final @NotNull DoorType type)
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean consoleLogging()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
    }
}
