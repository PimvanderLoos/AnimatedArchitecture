package nl.pim16aap2.bigdoors.core.localization;

import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;

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
     * Shortcut {@link #getMessage(String, Object...)} for {@link StructureType#getLocalizationKey()}.
     *
     * @param structureType
     *     The structure type to localize.
     * @return The localized name of the structure type.
     */
    default String getStructureType(StructureType structureType)
    {
        return getMessage(structureType.getLocalizationKey());
    }

    /**
     * See {@link #getStructureType(StructureType)}
     */
    default String getStructureType(AbstractStructure structure)
    {
        return getStructureType(structure.getType());
    }

    /**
     * Gets a list of {@link Locale}s that are currently available.
     *
     * @return The list of available locales.
     */
    @SuppressWarnings("unused")
    List<Locale> getAvailableLocales();
}
