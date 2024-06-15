package nl.pim16aap2.animatedarchitecture.core.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Represents an object that can read values from configs.
 */
public interface IConfigReader
{
    /**
     * Gets the value of a config option at a given path. If unavailable, the default object is returned instead.
     *
     * @param path
     *     The path of the config option to read.
     * @param fallback
     *     The default value to return if the actual option in the config was unavailable.
     * @return The value of the config option if possible, otherwise the default value.
     */
    @Contract("_, !null -> !null")
    @Nullable Object get(String path, @Nullable Object fallback);

    /**
     * Gets a set containing all keys.
     */
    Set<String> getKeys();
}
