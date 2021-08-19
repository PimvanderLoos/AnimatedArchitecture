package nl.pim16aap2.bigdoors.localization;

import lombok.Setter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import org.jetbrains.annotations.NotNull;
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

import static nl.pim16aap2.bigdoors.localization.LocalizationUtil.ensureZipFileExists;

/**
 * Represents a class that can be used to localize Strings.
 *
 * @author Pim
 */
final class Localizer implements ILocalizer
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
     * @param directory     The directory the translation file(s) exist in.
     * @param baseName      The base name of the localization files. For example, when you have a file
     *                      "Translations_en_US.properties", the base name would be "Translations".
     * @param defaultLocale The default {@link Locale} to use when no locale is specified when requesting a translation.
     *                      Defaults to {@link Locale#ROOT}.
     */
    Localizer(@NotNull Path directory, @NotNull String baseName, @NotNull Locale defaultLocale)
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
    Localizer(@NotNull Path directory, @NotNull String baseName)
    {
        this(directory, baseName, Locale.ROOT);
    }

    @Override public @NotNull String getMessage(@NotNull String key, @NotNull Locale locale, @NotNull Object... args)
    {
        if (classLoader == null)
        {
            BigDoors.get().getPLogger().warn("Failed to find localization key \"" + key +
                                                 "\"! Reason: ClassLoader is null!");
            return KEY_NOT_FOUND_MESSAGE + key;
        }

        try
        {
            final String msg = ResourceBundle.getBundle(baseName, locale, classLoader).getString(key);
            return args.length == 0 ? msg : MessageFormat.format(msg, args);
        }
        catch (MissingResourceException e)
        {
            BigDoors.get().getPLogger().warn("Failed to find localization key \"" + key +
                                                 "\"! Reason: Key does not exist!");
            return KEY_NOT_FOUND_MESSAGE + key;
        }
    }

    @Override public @NotNull String getMessage(@NotNull String key, @NotNull Object... args)
    {
        return getMessage(key, defaultLocale, args);
    }

    @Override @SuppressWarnings("unused")
    public @NotNull List<Locale> getAvailableLocales()
    {
        return localeList;
    }

    /**
     * Initializes this localizer.
     * <p>
     * Together with {@link #shutdown()}, this method can be used to re-initialize this localizer.
     *
     * @throws IllegalStateException When trying to initialize this localizer while the ClassLoader is not closed.
     */
    @Initializer
    void init()
    {
        if (classLoader != null)
            throw new IllegalStateException("ClassLoader is already initialized!");

        final Path bundlePath = directory.resolve(bundleName);
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
        final URLClassLoader ucl = new URLClassLoader(urls);
        // Get the base file (which we know exists) as stream. This is a hack to ensure
        // that the files accessed by the ResourceBundle are current.
        // When skipping this step, the ResourceBundle will not see any changes
        // made to the files since the last time the UCL was recreated.
        //noinspection EmptyTryBlock
        try (final InputStream ignored = ucl.getResourceAsStream(baseName + ".properties"))
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
                BigDoors.get().getPLogger()
                        .logThrowable(e, "Failed to close class loader! Localizations cannot be reloaded!");
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
