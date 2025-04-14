package nl.pim16aap2.animatedarchitecture.core.localization;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A localizer that uses a specific locale for localization.
 * <p>
 * This is used to localize messages for a specific client.
 */
@AllArgsConstructor
@EqualsAndHashCode
public final class PersonalizedLocalizer implements ILocalizer
{
    private final ILocalizer localizer;
    private final @Nullable Locale locale;

    /**
     * @deprecated Using this method will override the locale set in the constructor. Use
     * {@link #getMessage(String, Object...)} instead.
     */
    @Override
    @Deprecated
    public String getMessage(String key, @Nullable Locale clientLocale, Object... args)
    {
        return localizer.getMessage(key, clientLocale, args);
    }

    @Override
    public String getMessage(String key, Object... args)
    {
        //noinspection deprecation
        return getMessage(key, locale, args);
    }

    @Override
    public List<Locale> getAvailableLocales()
    {
        return localizer.getAvailableLocales();
    }

    @Override
    public String toString()
    {
        return "PersonalizedLocalizer{" +
            "localizer=" + localizer.getClass() +
            ", locale='" + locale +
            "'}";
    }
}
