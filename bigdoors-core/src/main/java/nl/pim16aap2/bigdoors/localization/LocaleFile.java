package nl.pim16aap2.bigdoors.localization;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Represents a translation file for a specific {@link Locale}.
 *
 * @param path   The path of the file.
 * @param locale The {@link Locale} this file represents.
 */
record LocaleFile(@NotNull Path path, @NotNull String locale) {}
