package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.NonNull;
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
private final @NonNull FileConfiguration config;

    public ConfigReaderSpigot(final @NonNull FileConfiguration config)
    {
        this.config = config;
    }

    @Override
    public @NonNull Object get(@NonNull String path, @Nullable Object def)
    {
        return config.get(path, def);
    }
}
