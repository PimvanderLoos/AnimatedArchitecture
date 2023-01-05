package nl.pim16aap2.bigdoors.localization;

/**
 * Represents an entry in a localization file.
 *
 * @param key
 *     The key of the localization entry. E.g. "start_menu.button.sleep.help".
 * @param value
 *     The value of the localization entry. E.g. "Press this button to make the computer go to sleep."
 */
record LocalizationEntry(String key, String value)
{
}
