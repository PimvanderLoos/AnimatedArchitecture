package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.core.api.IConfigReader;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Spigot implementation for {@link IConfigReader}.
 *
 * @author Pim
 */
public record ConfigReaderSpigot(FileConfiguration config) implements IConfigReader
{
    @Override
    public @Nullable Object get(String path, @Nullable Object fallback)
    {
        return config.get(path, fallback);
    }

    @Override
    public Set<String> getKeys()
    {
        return config.getKeys(false);
    }
}
