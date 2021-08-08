package nl.pim16aap2.bigdoors.localization;

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

/**
 * Represents a class that can be used to localize Strings.
 *
 * @author Pim
 */
public class Localizer extends Restartable
{
    public static final String KEY_NOT_FOUND_MESSAGE = "Failed to localize message: ";

    private final @NotNull Path directory;
    private final @NotNull String baseName;
    private @Nullable URLClassLoader classLoader = null;
    private List<Locale> localeList;

    /**
     * @param restartableHolder The {@link IRestartableHolder} to register this class with.
     * @param directory         The directory the translation file(s) exist in.
     * @param baseName          The base name of the localization files. For example, when you have a file
     *                          "Translations_en_US.properties", the base name would be "Translations".
     */
    public Localizer(@NotNull IRestartableHolder restartableHolder, @NotNull Path directory, @NotNull String baseName)
    {
        super(restartableHolder);
        this.baseName = baseName;
        this.directory = directory;
        init();
    }

    /**
     * See {@link #Localizer(IRestartableHolder, Path, String)}.
     */
    public Localizer(@NotNull Path directory, @NotNull String baseName)
    {
        this(BigDoors.get(), directory, baseName);
    }

    /**
     * Retrieves a localized message using {@link Locale#ROOT}.
     *
     * @param key  The key of the message.
     * @param args The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     *
     * @throws MissingResourceException When no mapping for the key can be found.
     */
    public @NotNull String getMessage(@NotNull String key, @NotNull Object... args)
    {
        return getMessage(key, Locale.ROOT, args);
    }

    /**
     * Gets a list of {@link Locale}s that are currently available.
     *
     * @return The list of available locales.
     */
    public @NotNull List<Locale> getLocales()
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
        try
        {
            final URL[] urls = {directory.toUri().toURL()};
            classLoader = new URLClassLoader(urls);
            localeList = LocalizationUtil.getLocalesInDirectory(directory, baseName);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to initialize localizer!");
            classLoader = null;
            localeList = Collections.emptyList();
        }
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
