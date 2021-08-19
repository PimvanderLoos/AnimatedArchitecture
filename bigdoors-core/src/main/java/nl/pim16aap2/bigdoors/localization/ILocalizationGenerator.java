package nl.pim16aap2.bigdoors.localization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a type that can be used to generate localization files using properties files from various sources.
 *
 * @author Pim
 */
interface ILocalizationGenerator
{
    /**
     * Adds a new set of resources to the current localization set.
     * <p>
     * When the provided path is a directory, only files of the format "directory/basename[_locale].properties" are
     * included. "[_locale]" is optional here.
     * <p>
     * When the provided path is a file, it is assumed to be a zip file (regardless of its extension, jars would also
     * work, for example). Only files in the top-level directory of the zip file are considered and of those, only if
     * they have the format "baseName[_locale].properties". When no specific baseName is provided, the "baseName" part
     * used as example in the format definition can only contain letters, numbers, and hyphens.
     *
     * @param path     The path to a directory containing properties files or the path to a  zip file containing
     *                 properties files in its top-level directory.
     * @param baseName The base name of the translation files. When this is null, this property will be ignored and
     *                 locale files will be appended purely based on their locale.
     *                 <p>
     *                 When a baseName is provided, only those files whose names contain only those exact characters or
     *                 those characters followed by an underscore and the locale are considered. The ".properties" file
     *                 extension requirement stays either way.
     */
    void addResources(@NotNull Path path, @Nullable String baseName);

    /**
     * Adds locale files from multiple paths.
     * <p>
     * This method does not support using base names.
     * <p>
     * See {@link #addResources(Path, String)}
     */
    void addResources(@NotNull List<Path> paths);

    /**
     * Loads the locale files from the jar file a specific {@link Class} was loaded from.
     * <p>
     * See {@link #addResources(Path, String)}.
     */
    void addResourcesFromClass(@NotNull Class<?> clz, @Nullable String baseName);

    /**
     * Loads the locale files from the jars file specific {@link Class}es was loaded from.
     * <p>
     * This method does not support using base names.
     * <p>
     * See {@link #addResourcesFromClass(Class, String)}.
     */
    void addResourcesFromClass(@NotNull List<Class<?>> classes);
}
