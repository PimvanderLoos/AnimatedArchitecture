package nl.pim16aap2.bigdoors.spigot.util;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.logging.PLogger;
import nl.pim16aap2.bigdoors.util.Util;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to assist in checking for updates for plugins uploaded to
 * <a href="https://spigotmc.org/resources/">SpigotMC</a>. Before any members of
 * this class are accessed, {@link #init(JavaPlugin, int, IPLogger)} must be invoked by the plugin, preferably in its
 * {@link JavaPlugin#onEnable()} method, though that is not a requirement.
 * <p>
 * This class performs asynchronous queries to
 * <a href="https://spiget.org">SpiGet</a>, an REST server which is updated
 * periodically. If the results of {@link #requestUpdateCheck()} are inconsistent with what is published on SpigotMC, it
 * may be due to SpiGet's cache. Results will be updated in due time.
 * <p>
 * Some modifications were made to support downloading of updates and storing the age of an update.
 *
 * @author Parker Hawke - 2008Choco
 */
public final class UpdateChecker
{
    public static final VersionScheme VERSION_SCHEME_DECIMAL = (first, second) ->
    {
        final String @Nullable [] firstSplit = splitVersionInfo(first);
        final String @Nullable [] secondSplit = splitVersionInfo(second);
        if (firstSplit == null || secondSplit == null)
            return null;

        for (int i = 0; i < Math.min(firstSplit.length, secondSplit.length); i++)
        {
            final int currentValue = NumberUtils.toInt(firstSplit[i]), newestValue = NumberUtils.toInt(secondSplit[i]);

            if (newestValue > currentValue)
                return second;
            else if (newestValue < currentValue)
                return first;
        }

        return (secondSplit.length > firstSplit.length) ? second : first;
    };

    private static final String USER_AGENT = "BigDoors-update-checker";
    private static final String UPDATE_URL = "https://api.spiget.org/v2/resources/%d/versions?size=1&sort=-releaseDate";
    private static final Pattern DECIMAL_SCHEME_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
    private final String downloadURL;

    private static @Nullable UpdateChecker INSTANCE;

    private @Nullable UpdateResult lastResult = null;

    private final JavaPlugin plugin;
    private final int pluginID;
    private final VersionScheme versionScheme;
    private final IPLogger logger;

    private UpdateChecker(JavaPlugin plugin, int pluginID, VersionScheme versionScheme, IPLogger logger)
    {
        this.plugin = plugin;
        this.pluginID = pluginID;
        this.versionScheme = versionScheme;
        this.logger = logger;
        downloadURL = "https://api.spiget.org/v2/resources/" + pluginID + "/download";
    }

    /**
     * Requests an update check to SpiGet. This request is asynchronous and may not complete immediately as an HTTP GET
     * request is published to the SpiGet API.
     *
     * @return a future update result
     */
    public CompletableFuture<UpdateResult> requestUpdateCheck()
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                int responseCode;
                try
                {
                    final URL url = new URL(String.format(UPDATE_URL, pluginID));
                    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.addRequestProperty("User-Agent", USER_AGENT);

                    final InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    responseCode = connection.getResponseCode();

                    final JsonElement element = new JsonParser().parse(reader);
                    if (!element.isJsonArray())
                        return new UpdateResult(UpdateReason.INVALID_JSON);

                    reader.close();

                    final JsonObject versionObject = element.getAsJsonArray().get(0).getAsJsonObject();

                    final String ageString = versionObject.get("releaseDate").getAsString();
                    final OptionalLong ageOpt = Util.parseLong(ageString);
                    if (ageOpt.isEmpty())
                        throw new IllegalArgumentException("Invalid age string: \"" + ageString + "\"");
                    final long age = ageOpt.getAsLong();

                    final String current = plugin.getDescription().getVersion(), newest = versionObject.get("name")
                                                                                                       .getAsString();
                    final @Nullable String latest = versionScheme.compareVersions(current, newest);

                    if (latest == null)
                        return new UpdateResult(UpdateReason.UNSUPPORTED_VERSION_SCHEME);
                    else if (latest.equals(current))
                        return new UpdateResult(current.equals(newest) ? UpdateReason.UP_TO_DATE :
                                                UpdateReason.UNRELEASED_VERSION, current, age);
                    else if (latest.equals(newest))
                        return new UpdateResult(UpdateReason.NEW_UPDATE, latest, age);
                }
                catch (IOException e)
                {
                    return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
                }
                catch (JsonSyntaxException e)
                {
                    return new UpdateResult(UpdateReason.INVALID_JSON);
                }

                return new UpdateResult(
                    responseCode == 401 ? UpdateReason.UNAUTHORIZED_QUERY : UpdateReason.UNKNOWN_ERROR);
            }).exceptionally(ex -> Util.exceptionally(ex, new UpdateResult(UpdateReason.UNKNOWN_ERROR)));
    }

    /**
     * Gets the last update result that was queried by {@link #requestUpdateCheck()}. If no update check was performed
     * since this class' initialization, this method will return null.
     *
     * @return the last update check result. null if none.
     */
    public @Nullable UpdateResult getLastResult()
    {
        return lastResult;
    }

    private static String @Nullable [] splitVersionInfo(String version)
    {
        final Matcher matcher = DECIMAL_SCHEME_PATTERN.matcher(version);
        if (!matcher.find())
            return null;

        return matcher.group().split("\\.");
    }

    /**
     * Gets the url to download the latest version from.
     *
     * @return The url to download the latest version from.
     */
    public String getDownloadUrl()
    {
        return downloadURL;
    }

    /**
     * Downloads the latest update.
     *
     * @return True if the download was successful.
     */
    public boolean downloadUpdate()
    {
        boolean downloadSuccessfull = false;
        try
        {
            final File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists() && !updateFolder.mkdirs())
                throw new RuntimeException("Failed to create update folder!");

            final String fileName = plugin.getName() + ".jar";
            final File updateFile = new File(updateFolder + "/" + fileName);

            // Follow any and all redirects until we've finally found the actual file.
            String location = downloadURL;
            HttpURLConnection httpConnection;
            for (; ; )
            {
                final URL url = new URL(location);
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setInstanceFollowRedirects(false);
                httpConnection.setRequestProperty("User-Agent", "BigDoorsUpdater");
                final String redirectLocation = httpConnection.getHeaderField("Location");
                if (redirectLocation == null)
                    break;
                location = redirectLocation;
                httpConnection.disconnect();
            }

            if (httpConnection.getResponseCode() != 200)
            {
                logger.logThrowable(
                    new RuntimeException("Download returned status #" + httpConnection.getResponseCode() +
                                             "\n for URL: " + downloadURL));
                return false;
            }

            final int grabSize = 4096;

            try (BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
                 FileOutputStream fos = new FileOutputStream(updateFile);
                 BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize))
            {
                final byte[] data = new byte[grabSize];
                int grab;
                while ((grab = in.read(data, 0, grabSize)) >= 0)
                    bout.write(data, 0, grab);

                bout.flush();
                fos.flush();
                downloadSuccessfull = true;
            }
        }
        catch (Exception e)
        {
            logger.logThrowable(e);
        }
        return downloadSuccessfull;
    }

    /**
     * Initializes this update checker with the specified values and return its instance. If an instance of
     * UpdateChecker has already been initialized, this method will act similarly to {@link #get()} (which is
     * recommended after initialization).
     *
     * @param plugin
     *     the plugin for which to check updates. Cannot be null.
     * @param pluginID
     *     the ID of the plugin as identified in the SpigotMC resource link. For example,
     *     "https://www.spigotmc.org/resources/veinminer.<b>12038</b>/" would expect "12038" as a value. The value must
     *     be greater than 0
     * @param versionScheme
     *     a custom version scheme parser. Cannot be null
     * @param logger
     *     The {@link PLogger} to use for logging.
     * @return The {@link UpdateChecker} instance.
     */
    public static UpdateChecker init(JavaPlugin plugin, int pluginID, VersionScheme versionScheme, IPLogger logger)
    {
        Preconditions.checkArgument(pluginID > 0, "Plugin ID must be greater than 0");

        return (INSTANCE == null) ? INSTANCE = new UpdateChecker(plugin, pluginID, versionScheme, logger) : INSTANCE;
    }

    /**
     * Initializes this update checker with the specified values and return its instance. If an instance of
     * UpdateChecker has already been initialized, this method will act similarly to {@link #get()} (which is
     * recommended after initialization).
     *
     * @param plugin
     *     the plugin for which to check updates. Cannot be null
     * @param pluginID
     *     the ID of the plugin as identified in the SpigotMC resource link. For example,
     *     "https://www.spigotmc.org/resources/veinminer.<b>12038</b>/" would expect "12038" as a value. The value must
     *     be greater than 0
     * @param logger
     *     The {@link IPLogger} to use for logging.
     * @return The {@link UpdateChecker} instance.
     */
    public static UpdateChecker init(JavaPlugin plugin, int pluginID, IPLogger logger)
    {
        return init(plugin, pluginID, Objects.requireNonNull(VERSION_SCHEME_DECIMAL, "Scheme cannot be null!"), logger);
    }

    /**
     * Gets the initialized instance of UpdateChecker. If {@link #init(JavaPlugin, int, IPLogger)} has not yet been
     * invoked, this method will throw an exception.
     *
     * @return The {@link UpdateChecker} instance.
     */
    public static UpdateChecker get()
    {
        return Util.requireNonNull(INSTANCE, "Instance");
    }

    /**
     * Checks whether the UpdateChecker has been initialized or not (if {@link #init(JavaPlugin, int, IPLogger)} has
     * been invoked) and {@link #get()} is safe to use.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized()
    {
        return INSTANCE != null;
    }

    /**
     * A functional interface to compare two version Strings with similar version schemes.
     */
    @FunctionalInterface
    public interface VersionScheme
    {
        /**
         * Compare two versions and return the higher of the two. If null is returned, it is assumed that at least one
         * of the two versions are unsupported by this version scheme parser.
         *
         * @param first
         *     the first version to check
         * @param second
         *     the second version to check
         * @return the greater of the two versions. null if unsupported version schemes
         */
        @Nullable String compareVersions(String first, String second);
    }

    /**
     * A constant reason for the result of {@link UpdateResult}.
     */
    public enum UpdateReason
    {

        /**
         * A new update is available for download on SpigotMC.
         */
        NEW_UPDATE, // The only reason that requires an update

        /**
         * A successful connection to the SpiGet API could not be established.
         */
        COULD_NOT_CONNECT,

        /**
         * The JSON retrieved from SpiGet was invalid or malformed.
         */
        INVALID_JSON,

        /**
         * A 401 error was returned by the SpiGet API.
         */
        UNAUTHORIZED_QUERY,

        /**
         * The version of the plugin installed on the server is greater than the one uploaded to SpigotMC's resources
         * section.
         */
        UNRELEASED_VERSION,

        /**
         * An unknown error occurred.
         */
        UNKNOWN_ERROR,

        /**
         * The plugin uses an unsupported version scheme, therefore a proper comparison between versions could not be
         * made.
         */
        UNSUPPORTED_VERSION_SCHEME,

        /**
         * The plugin is up to date with the version released on SpigotMC's resources section.
         */
        UP_TO_DATE

    }

    /**
     * Represents a result for an update query performed by {@link UpdateChecker#requestUpdateCheck()}.
     */
    public final class UpdateResult
    {
        private final UpdateReason reason;
        private final String newestVersion;
        private final long age;

        { // An actual use for initializer blocks. This is madness!
            lastResult = this;
        }

        private UpdateResult(UpdateReason reason, String newestVersion, long age)
        {
            this.reason = reason;
            this.newestVersion = newestVersion;
            this.age = age;
        }

        private UpdateResult(UpdateReason reason)
        {
            Preconditions.checkArgument(reason != UpdateReason.NEW_UPDATE && reason != UpdateReason.UP_TO_DATE,
                                        "Reasons that might require updates must also " +
                                            "provide the latest version String");
            this.reason = reason;
            newestVersion = plugin.getDescription().getVersion();
            age = -1;
        }

        /**
         * Gets the constant reason of this result.
         *
         * @return the reason
         */
        public UpdateReason getReason()
        {
            return reason;
        }

        /**
         * Checks whether or not this result requires the user to update.
         *
         * @return true if requires update, false otherwise
         */
        public boolean requiresUpdate()
        {
            return reason == UpdateReason.NEW_UPDATE;
        }

        /**
         * Gets the latest version of the plugin. This may be the currently installed version, it may not be. This
         * depends entirely on the result of the update.
         *
         * @return the newest version of the plugin
         */
        public String getNewestVersion()
        {
            return newestVersion;
        }

        /**
         * Gets the number of seconds since the last update was released.
         *
         * @return The number of seconds since the last update was released or -1 if unavailable.
         */
        public long getAge()
        {
            return age;
        }
    }
}
