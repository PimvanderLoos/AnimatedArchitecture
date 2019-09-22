package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IConfigReader;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spigot implementation for {@link IConfigReader}.
 *
 * @author Pim
 */
public class ConfigReaderSpigot implements IConfigReader
{
    private final FileConfiguration config;

    public ConfigReaderSpigot(final @NotNull FileConfiguration config)
    {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(@NotNull String path, @Nullable Object def)
    {
        return config.get(path, def);
    }
}
