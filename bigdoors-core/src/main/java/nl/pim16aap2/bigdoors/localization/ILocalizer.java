package nl.pim16aap2.bigdoors.localization;

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
     * @param key
     *     The key of the message.
     * @param args
     *     The arguments of the message, if any.
     * @param locale
     *     The {@link Locale} to use.
     * @return The localized message associated with the provided key.
     */
    String getMessage(String key, Locale locale, Object... args);

    /**
     * Retrieves a localized message using the default locale.
     *
     * @param key
     *     The key of the message.
     * @param args
     *     The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     *
     * @throws MissingResourceException
     *     When no mapping for the key can be found.
     */
    String getMessage(String key, Object... args);

    /**
     * Gets a list of {@link Locale}s that are currently available.
     *
     * @return The list of available locales.
     */
    @SuppressWarnings("unused")
    List<Locale> getAvailableLocales();
}
