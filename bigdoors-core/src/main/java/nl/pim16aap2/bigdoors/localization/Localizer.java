package nl.pim16aap2.bigdoors.localization;

import lombok.Setter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * Represents a class that can be used to localize Strings.
 *
 * @author Pim
 */
@Flogger //
final class Localizer implements ILocalizer
{
    static final String KEY_NOT_FOUND_MESSAGE = "Failed to localize message: ";

    private Path directory;
    private String baseName;
    private String bundleName;

    /**
     * The default {@link Locale} to use when no locale is specified when requesting a translation. Defaults to {@link
     * Locale#ROOT}.
     */
    @Setter
    private Locale defaultLocale;
    private @Nullable URLClassLoader classLoader = null;
    private List<Locale> localeList;

    /**
     * @param directory
     *     The directory the translation file(s) exist in.
     * @param baseName
     *     The base name of the localization files. For example, when you have a file "Translations_en_US.properties",
     *     the base name would be "Translations".
     * @param defaultLocale
     *     The default {@link Locale} to use when no locale is specified when requesting a translation. Defaults to
     *     {@link Locale#ROOT}.
     */
    Localizer(Path directory, String baseName, Locale defaultLocale)
    {
        this.baseName = baseName;
        this.directory = directory;
        this.defaultLocale = defaultLocale;
        bundleName = baseName + ".bundle";
        init();
    }

    /**
     * See {@link #Localizer(Path, String, Locale)}.
     */
    Localizer(Path directory, String baseName)
    {
        this(directory, baseName, Locale.ROOT);
    }

    /**
     * Updates the location of the localization bundle being used.
     * <p>
     * Don't forget to use {@link #reInit()} to apply the changes.
     *
     * @param directory
     *     The directory the translation file(s) exist in.
     * @param baseName
     *     The base name of the localization files. For example, when you have a file "Translations_en_US.properties",
     *     the base name would be "Translations".
     */
    void updateBundleLocation(Path directory, String baseName)
    {
        this.baseName = baseName;
        this.directory = directory;
        bundleName = baseName + ".bundle";
    }

    @Override
    public String getMessage(String key, Locale locale, Object... args)
    {
        if (classLoader == null)
        {
            log.at(Level.WARNING).log("Failed to find localization key \"" + key + "\"! Reason: ClassLoader is null!");
            return KEY_NOT_FOUND_MESSAGE + key;
        }

        try
        {
            final String msg = ResourceBundle.getBundle(baseName, locale, classLoader).getString(key);
            return args.length == 0 ? msg : MessageFormat.format(msg, args);
        }
        catch (MissingResourceException e)
        {
            log.at(Level.WARNING).log("Failed to find localization key \"" + key + "\"! Reason: Key does not exist!");
            return KEY_NOT_FOUND_MESSAGE + key;
        }
    }

    @Override
    public String getMessage(String key, Object... args)
    {
        return getMessage(key, defaultLocale, args);
    }

    @Override @SuppressWarnings("unused")
    public List<Locale> getAvailableLocales()
    {
        return localeList;
    }

    /**
     * Initializes this localizer.
     * <p>
     * Together with {@link #shutdown()}, this method can be used to re-initialize this localizer.
     *
     * @throws IllegalStateException
     *     When trying to initialize this localizer while the ClassLoader is not closed.
     */
    @Initializer
    void init()
    {
        if (classLoader != null)
            throw new IllegalStateException("ClassLoader is already initialized!");

        final Path bundlePath = directory.resolve(bundleName);
        LocalizationUtil.ensureZipFileExists(bundlePath);
        try
        {
            classLoader = getNewURLClassLoader(bundlePath, baseName);
            localeList = LocalizationUtil.getLocalesInZip(bundlePath, baseName);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to initialize localizer!");
            classLoader = null;
            localeList = Collections.emptyList();
        }
    }

    private static URLClassLoader getNewURLClassLoader(Path bundlePath, String baseName)
        throws IOException
    {
        final URL[] urls = {bundlePath.toUri().toURL()};
        final URLClassLoader ucl = new URLClassLoader(urls);
        // Get the base file (which we know exists) as stream. This is a hack to ensure
        // that the files accessed by the ResourceBundle are current.
        // When skipping this step, the ResourceBundle will not see any changes
        // made to the files since the last time the UCL was recreated.
        //noinspection EmptyTryBlock
        try (@Nullable InputStream ignored = ucl.getResourceAsStream(baseName + ".properties"))
        {
            // ignored
        }
        return ucl;
    }

    /**
     * Shuts down this localizer by closing the {@link #classLoader}.
     * <p>
     * After calling this method, all requests for localized messages will fail until {@link #init()} is called.
     */
    void shutdown()
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
                log.at(Level.SEVERE).withCause(e)
                   .log("Failed to close class loader! Localizations cannot be reloaded!");
            }
        }
    }

    /**
     * Re-initializes this Localizer.
     * <p>
     * See {@link #shutdown()} and {@link #init()}.
     */
    void reInit()
    {
        shutdown();
        init();
    }
}
