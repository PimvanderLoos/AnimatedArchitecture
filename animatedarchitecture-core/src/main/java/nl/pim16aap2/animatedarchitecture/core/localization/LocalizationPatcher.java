package nl.pim16aap2.animatedarchitecture.core.localization;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Represents a class that can be used to apply user-defined localization patches.
 */
@Flogger //
final class LocalizationPatcher
{
    @Getter
    private final List<LocaleFile> patchFiles;

    LocalizationPatcher(Path directory, String baseName)
        throws IOException
    {
        // Ensure the base patch file exists.
        LocalizationUtil.ensureFileExists(directory.resolve(baseName + ".properties"));
        patchFiles = LocalizationUtil.getLocaleFilesInDirectory(directory, baseName);
    }

    /**
     * Updates the localization keys in the patch file(s).
     * <p>
     * Any root keys not already present in the patch file(s) will be appended to the file without a value.
     *
     * @param rootKeys
     *     The localization keys in the root locale file.
     */
    void updatePatchKeys(Collection<String> rootKeys)
    {
        patchFiles.forEach(patchFile -> updatePatchKeys(rootKeys, patchFile));
    }

    /**
     * Updates the localization keys in the patch file.
     * <p>
     * Any root keys not already present in the patch file will be appended to the file without a value.
     *
     * @param rootKeys
     *     The localization keys in the root locale file.
     * @param localeFile
     *     The locale file to append any missing localization keys to.
     */
    void updatePatchKeys(Collection<String> rootKeys, LocaleFile localeFile)
    {
        final Map<String, String> patchEntries = LocalizationUtil.getEntryMap(localeFile.path());
        final Set<String> appendableKeys = new HashSet<>(rootKeys);
        appendableKeys.removeAll(patchEntries.keySet());
        if (appendableKeys.isEmpty())
            return;

        appendableKeys.forEach(key -> patchEntries.put(key, ""));
        writePatchFile(localeFile, patchEntries);
    }

    /**
     * Appends localization keys to a locale file.
     *
     * @param localeFile
     *     The locale file to append the keys to.
     * @param entries
     *     The localization entries to append to the locale file.
     */
    void writePatchFile(LocaleFile localeFile, Map<String, String> entries)
    {
        if (entries.isEmpty())
            return;

        final StringBuilder sb = new StringBuilder();
        entries.forEach((key, value) -> sb.append(key).append('=').append(value).append('\n'));
        try
        {
            Files.writeString(localeFile.path(), sb.toString(),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to write localization entries to file: %s", localeFile.path());
        }
    }

    /**
     * Gets the patches for a given locale that are specified in the patch file.
     * <p>
     * A patch is defined in this context as a key/value pair with a non-empty value.
     *
     * @param localeFile
     *     The locale file to read the patches from.
     * @return A map of the patches. The keys are the localization keys and the values the full line (i.e. "key=value").
     */
    Map<String, String> getPatches(LocaleFile localeFile)
    {
        final Map<String, String> ret = new LinkedHashMap<>();
        LocalizationUtil.readFile(localeFile.path(), line ->
        {
            final @Nullable LocalizationEntry entry = LocalizationUtil.getEntryFromLine(line);
            if (isValidPatch(entry))
                ret.put(entry.key(), line);
        });
        return ret;
    }

    /**
     * Tests if a line is a valid patch.
     *
     * @param entry
     *     The localization entry
     * @return True if the patch is valid (i.e. not empty).
     */
    @Contract("null -> false")
    static boolean isValidPatch(@Nullable LocalizationEntry entry)
    {
        if (entry == null)
            return false;
        return !entry.value().isEmpty();
    }
}
