package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IConfigReader;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Spigot implementation for {@link IConfigReader}.
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
