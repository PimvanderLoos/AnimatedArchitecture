package nl.pim16aap2.bigdoors.localization;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Represents a class that can localize strings.
 *
 * @author Pim
 */
public interface ILocalizer
{
    /**
     * Retrieves a localized message.
     *
     * @param key    The key of the message.
     * @param args   The arguments of the message, if any.
     * @param locale The {@link Locale} to use.
     * @return The localized message associated with the provided key.
     */
    @NotNull String getMessage(@NotNull String key, @NotNull Locale locale, @NotNull Object... args);

    /**
     * Retrieves a localized message using the default locale.
     *
     * @param key  The key of the message.
     * @param args The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     *
     * @throws MissingResourceException When no mapping for the key can be found.
     */
    @NotNull String getMessage(@NotNull String key, @NotNull Object... args);

    /**
     * Gets a list of {@link Locale}s that are currently available.
     *
     * @return The list of available locales.
     */
    @SuppressWarnings("unused")
    @NotNull List<Locale> getAvailableLocales();
}
