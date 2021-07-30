package nl.pim16aap2.bigdoors.localization;

import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
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
    private final @NotNull String baseName;
    private final @NotNull File directory;
    private @Nullable URLClassLoader classLoader = null;

    /**
     * @param restartableHolder The {@link IRestartableHolder} to register this class with.
     * @param baseName          The base name of the localization files. For example, when you have a file
     *                          "Translations_en_US.properties", the base name would be "Translations".
     * @param directory         The directory the translation file(s) exist in.
     */
    public Localizer(@NotNull IRestartableHolder restartableHolder, @NotNull String baseName, @NotNull File directory)
    {
        super(restartableHolder);
        this.baseName = baseName;
        this.directory = directory;
        init();
    }

    /**
     * See {@link #Localizer(IRestartableHolder, String, File)}.
     */
    public Localizer(@NotNull String baseName, @NotNull File directory)
    {
        this(BigDoors.get(), baseName, directory);
    }

    /**
     * Retrieves a localized message using {@link Locale#getDefault()}.
     *
     * @param key  The key of the message.
     * @param args The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     *
     * @throws MissingResourceException When no mapping for the key can be found.
     */
    public @NotNull String getMessage(@NotNull String key, @NotNull Object... args)
    {
        return getMessage(key, Locale.getDefault(), args);
    }

    /**
     * Retrieves a localized message.
     *
     * @param key    The key of the message.
     * @param args   The arguments of the message, if any.
     * @param locale The {@link Locale} to use.
     * @return The localized message associated with the provided key.
     *
     * @throws MissingResourceException When no mapping for the key can be found.
     */
    public @NotNull String getMessage(@NotNull String key, @NotNull Locale locale, @NotNull Object... args)
    {
        if (classLoader == null)
            return "Failed to localize message: " + key;

        val msg = ResourceBundle.getBundle(baseName, locale, classLoader).getString(key);

        return args.length == 0 ? msg : MessageFormat.format(msg, args);
    }

    @Initializer
    private void init()
    {
        try
        {
            final URL[] urls = {directory.toURI().toURL()};
            classLoader = new URLClassLoader(urls);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to initialize localizer!");
            classLoader = null;
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
