package nl.pim16aap2.animatedarchitecture.core.localization;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Represents a class that can localize strings.
 */
public interface ILocalizer
{
    /**
     * A dummy localizer that returns the key, locale and arguments in a string.
     */
    ILocalizer NULL = new DummyLocalizer();

    /**
     * Retrieves a localized message.
     *
     * @param key
     *     The key of the message.
     * @param clientLocale
     *     The Locale configured for the client. If the configuration does not allow client-specific locales or if the
     *     provided Locale is null, this parameter is ignored.
     * @param args
     *     The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     */
    String getMessage(String key, @Nullable Locale clientLocale, Object... args);

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
    default String getMessage(String key, Object... args)
    {
        return getMessage(key, null, args);
    }

    /**
     * Gets a list of {@link Locale}s that are currently available.
     *
     * @return The list of available locales.
     */
    @SuppressWarnings("unused")
    List<Locale> getAvailableLocales();

    /**
     * A dummy localizer that returns the key, locale and arguments in a string.
     */
    final class DummyLocalizer implements ILocalizer
    {
        @Override
        public String getMessage(String key, @Nullable Locale clientLocale, Object... args)
        {
            return key + " [" + clientLocale + "] " + Arrays.toString(args);
        }

        @Override
        public List<Locale> getAvailableLocales()
        {
            return List.of();
        }
    }
}
