package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an object that can read values from configs.
 *
 * @author Pim
 */
public interface IConfigReader
{
    /**
     * Gets the value of a config option at a given path. If unavailable, the default object is returned instead.
     *
     * @param path The path of the config option to read.
     * @param def  The default value to return if the actual option in the config was unavailable.
     * @return The value of the config option if possible, otherwise the default value.
     */
    @NonNull Object get(final @NonNull String path, final @Nullable Object def);
}
