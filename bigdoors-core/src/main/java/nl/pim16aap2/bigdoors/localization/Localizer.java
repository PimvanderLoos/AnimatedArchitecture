package nl.pim16aap2.bigdoors.localization;

import lombok.Setter;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static nl.pim16aap2.bigdoors.localization.LocalizationUtil.ensureZipFileExists;

/**
 * Represents a class that can be used to localize Strings.
 *
 * @author Pim
 */
public class Localizer extends Restartable
{
    static final String KEY_NOT_FOUND_MESSAGE = "Failed to localize message: ";

    private final @NotNull Path directory;
    private final @NotNull String baseName;
    private final @NotNull String bundleName;

    /**
     * The default {@link Locale} to use when no locale is specified when requesting a translation. Defaults to {@link
     * Locale#ROOT}.
     */
    @Setter
    private @NotNull Locale defaultLocale;
    private @Nullable URLClassLoader classLoader = null;
    private List<Locale> localeList;

    /**
     * @param restartableHolder The {@link IRestartableHolder} to register this class with.
     * @param directory         The directory the translation file(s) exist in.
     * @param baseName          The base name of the localization files. For example, when you have a file
     *                          "Translations_en_US.properties", the base name would be "Translations".
     * @param defaultLocale     The default {@link Locale} to use when no locale is specified when requesting a
     *                          translation. Defaults to {@link Locale#ROOT}.
     */
    public Localizer(@NotNull IRestartableHolder restartableHolder, @NotNull Path directory, @NotNull String baseName,
                     @NotNull Locale defaultLocale)
    {
        super(restartableHolder);
        this.baseName = baseName;
        this.directory = directory;
        this.defaultLocale = defaultLocale;
        bundleName = baseName + ".bundle";
        init();
    }

    public Localizer(@NotNull IRestartableHolder restartableHolder, @NotNull Path directory, @NotNull String baseName)
    {
        this(restartableHolder, directory, baseName, Locale.ROOT);
    }

    /**
     * See {@link #Localizer(IRestartableHolder, Path, String, Locale)}.
     */
    public Localizer(@NotNull Path directory, @NotNull String baseName, @NotNull Locale defaultLocale)
    {
        this(BigDoors.get(), directory, baseName, defaultLocale);
    }

    /**
     * See {@link #Localizer(IRestartableHolder, Path, String, Locale)}.
     */
    public Localizer(@NotNull Path directory, @NotNull String baseName)
    {
        this(BigDoors.get(), directory, baseName);
    }

    /**
     * Retrieves a localized message using {@link #defaultLocale}.
     *
     * @param key  The key of the message.
     * @param args The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     *
     * @throws MissingResourceException When no mapping for the key can be found.
     */
    public @NotNull String getMessage(@NotNull String key, @NotNull Object... args)
    {
        return getMessage(key, defaultLocale, args);
    }

    /**
     * Gets a list of {@link Locale}s that are currently available.
     *
     * @return The list of available locales.
     */
    @SuppressWarnings("unused")
    public @NotNull List<Locale> getAvailableLocales()
    {
        return localeList;
    }

    /**
     * Retrieves a localized message.
     *
     * @param key    The key of the message.
     * @param args   The arguments of the message, if any.
     * @param locale The {@link Locale} to use.
     * @return The localized message associated with the provided key.
     */
    public @NotNull String getMessage(@NotNull String key, @NotNull Locale locale, @NotNull Object... args)
    {
        if (classLoader == null)
            return KEY_NOT_FOUND_MESSAGE + key;

        try
        {
            val msg = ResourceBundle.getBundle(baseName, locale, classLoader).getString(key);
            return args.length == 0 ? msg : MessageFormat.format(msg, args);
        }
        catch (MissingResourceException e)
        {
            return KEY_NOT_FOUND_MESSAGE + key;
        }
    }

    @Initializer
    private void init()
    {
        val bundlePath = directory.resolve(bundleName);
        ensureZipFileExists(bundlePath);
        try
        {
            classLoader = getNewURLClassLoader(bundlePath, baseName);
            localeList = LocalizationUtil.getLocalesInZip(bundlePath, baseName);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to initialize localizer!");
            classLoader = null;
            localeList = Collections.emptyList();
        }
    }

    private static @NotNull URLClassLoader getNewURLClassLoader(@NotNull Path bundlePath, @NotNull String baseName)
        throws IOException
    {
        final URL[] urls = {bundlePath.toUri().toURL()};
        val ucl = new URLClassLoader(urls);
        // Get the base file (which we know exists) as stream. This is a hack to ensure
        // that the files accessed by the ResourceBundle are current.
        // When skipping this step, the ResourceBundle will not see any changes
        // made to the files since the last time the UCL was recreated.
        //noinspection EmptyTryBlock
        try (val ignored = ucl.getResourceAsStream(baseName + ".properties"))
        {
            // ignored
        }
        return ucl;
    }

    @Override
    public void restart()
    {
        shutdown();
        init();
    }

    @Override
    public void shutdown()
    {
        if (classLoader != null)
        {
            try
            {
                classLoader.close();
                classLoader = null;
            }
            catch (IOException e)
            {
                BigDoors.get().getPLogger()
                        .logThrowable(e, "Failed to close class loader! Localizations cannot be reloaded!");
            }
        }
    }
}
