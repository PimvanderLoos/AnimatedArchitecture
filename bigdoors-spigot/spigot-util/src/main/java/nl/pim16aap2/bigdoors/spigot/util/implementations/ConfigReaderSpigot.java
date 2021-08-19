package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IConfigReader;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Spigot implementation for {@link IConfigReader}.
 *
 * @author Pim
 */
public class ConfigReaderSpigot implements IConfigReader
{
    private final FileConfiguration config;

    public ConfigReaderSpigot(final FileConfiguration config)
    {
        this.config = config;
    }

    @Override
    public Object get(String path, @Nullable Object def)
    {
        return config.get(path, def);
    }
}
