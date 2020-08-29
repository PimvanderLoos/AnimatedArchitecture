package nl.pim16aap2.bigdoors.spigot.util;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to assist in checking for updates for plugins uploaded to
 * <a href="https://spigotmc.org/resources/">SpigotMC</a>. Before any members of
 * this class are accessed, {@link #init(JavaPlugin, int, PLogger)} must be invoked by the plugin, preferrably in its
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
    @NotNull
    public static final VersionScheme VERSION_SCHEME_DECIMAL = (first, second) ->
    {
        String[] firstSplit = splitVersionInfo(first), secondSplit = splitVersionInfo(second);
        if (firstSplit == null || secondSplit == null)
            return null;

        for (int i = 0; i < Math.min(firstSplit.length, secondSplit.length); i++)
        {
            int currentValue = NumberUtils.toInt(firstSplit[i]), newestValue = NumberUtils.toInt(secondSplit[i]);

            if (newestValue > currentValue)
                return second;
            else if (newestValue < currentValue)
                return first;
        }

        return (secondSplit.length > firstSplit.length) ? second : first;
    };

    @NotNull
    private static final String USER_AGENT = "BigDoors-update-checker";
    @NotNull
    private static final String UPDATE_URL = "https://api.spiget.org/v2/resources/%d/versions?size=1&sort=-releaseDate";
    @NotNull
    private static final Pattern DECIMAL_SCHEME_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
    @NotNull
    private final String downloadURL;

    @Nullable
    private static UpdateChecker instance;

    @Nullable
    private UpdateResult lastResult = null;

    @NotNull
    private final JavaPlugin plugin;
    private final int pluginID;
    @NotNull
    private final VersionScheme versionScheme;
    @NotNull
    private final PLogger logger;

    private UpdateChecker(final @NotNull JavaPlugin plugin, final int pluginID,
                          final @NotNull VersionScheme versionScheme, final @NotNull PLogger logger)
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
    public @NotNull CompletableFuture<UpdateResult> requestUpdateCheck()
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                int responseCode = -1;
                try
                {
                    URL url = new URL(String.format(UPDATE_URL, pluginID));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.addRequestProperty("User-Agent", USER_AGENT);

                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    responseCode = connection.getResponseCode();

                    JsonElement element = new JsonParser().parse(reader);
                    if (!element.isJsonArray())
                        return new UpdateResult(UpdateReason.INVALID_JSON);

                    reader.close();

                    JsonObject versionObject = element.getAsJsonArray().get(0).getAsJsonObject();

                    long age = -1;
                    String ageString = versionObject.get("releaseDate").getAsString();
                    try
                    {
                        age = getAge(Long.parseLong(ageString));
                    }
                    catch (NumberFormatException e)
                    {
                        PLogger.get()
                               .logThrowable(e, "Failed to obtain age of update from ageString: \"" + ageString + "\"");
                    }


                    String current = plugin.getDescription().getVersion(), newest = versionObject.get("name")
                                                                                                 .getAsString();
                    String latest = versionScheme.compareVersions(current, newest);

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
            });
    }

    /**
     * Gets the difference in seconds between a given time and the current time.
     *
     * @param updateTime A moment in time to compare the current time to.
     * @return The difference in seconds between a given time and the current time.
     */
    private long getAge(final long updateTime)
    {
        long currentTime = Instant.now().getEpochSecond();
        return currentTime - updateTime;
    }

    /**
     * Gets the last update result that was queried by {@link #requestUpdateCheck()}. If no update check was performed
     * since this class' initialization, this method will return null.
     *
     * @return the last update check result. null if none.
     */
    public @NotNull UpdateResult getLastResult()
    {
        return lastResult;
    }

    private static String[] splitVersionInfo(String version)
    {
        Matcher matcher = DECIMAL_SCHEME_PATTERN.matcher(version);
        if (!matcher.find())
            return null;

        return matcher.group().split("\\.");
    }

    /**
     * Gets the url to download the latest version from.
     *
     * @return The url to download the latest version from.
     */
    public @NotNull String getDownloadUrl()
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
            File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists())
                if (!updateFolder.mkdirs())
                    throw new RuntimeException("Failed to create update folder!");

            String fileName = plugin.getName() + ".jar";
            File updateFile = new File(updateFolder + "/" + fileName);

            // Follow any and all redirects until we've finally found the actual file.
            String location = downloadURL;
            HttpURLConnection httpConnection = null;
            for (; ; )
            {
                URL url = new URL(location);
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setInstanceFollowRedirects(false);
                httpConnection.setRequestProperty("User-Agent", "BigDoorsUpdater");
                String redirectLocation = httpConnection.getHeaderField("Location");
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

            int grabSize = 4096;
            BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
            FileOutputStream fos = new FileOutputStream(updateFile);
            BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

            byte[] data = new byte[grabSize];
            int grab;
            while ((grab = in.read(data, 0, grabSize)) >= 0)
                bout.write(data, 0, grab);

            bout.flush();
            bout.close();
            in.close();
            fos.flush();
            fos.close();
            downloadSuccessfull = true;
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
     * @param plugin        the plugin for which to check updates. Cannot be null.
     * @param pluginID      the ID of the plugin as identified in the SpigotMC resource link. For example,
     *                      "https://www.spigotmc.org/resources/veinminer.<b>12038</b>/" would expect "12038" as a
     *                      value. The value must be greater than 0
     * @param versionScheme a custom version scheme parser. Cannot be null
     * @param logger        The {@link PLogger} to use for logging.
     * @return The {@link UpdateChecker} instance.
     */
    public static @NotNull UpdateChecker init(final @NotNull JavaPlugin plugin, final int pluginID,
                                              final @NotNull VersionScheme versionScheme, final @NotNull PLogger logger)
    {
        Preconditions.checkArgument(pluginID > 0, "Plugin ID must be greater than 0");

        return (instance == null) ? instance = new UpdateChecker(plugin, pluginID, versionScheme, logger) : instance;
    }

    /**
     * Initializes this update checker with the specified values and return its instance. If an instance of
     * UpdateChecker has already been initialized, this method will act similarly to {@link #get()} (which is
     * recommended after initialization).
     *
     * @param plugin   the plugin for which to check updates. Cannot be null
     * @param pluginID the ID of the plugin as identified in the SpigotMC resource link. For example,
     *                 "https://www.spigotmc.org/resources/veinminer.<b>12038</b>/" would expect "12038" as a value. The
     *                 value must be greater than 0
     * @param logger   The {@link PLogger} to use for logging.
     * @return The {@link UpdateChecker} instance.
     */
    public static @NotNull UpdateChecker init(final @NotNull JavaPlugin plugin, final int pluginID,
                                              final @NotNull PLogger logger)
    {
        return init(plugin, pluginID, VERSION_SCHEME_DECIMAL, logger);
    }

    /**
     * Gets the initialized instance of UpdateChecker. If {@link #init(JavaPlugin, int, PLogger)} has not yet been
     * invoked, this method will throw an exception.
     *
     * @return The {@link UpdateChecker} instance.
     */
    public static @NotNull UpdateChecker get()
    {
        Preconditions.checkState(instance != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

    /**
     * Checks whether the UpdateChecker has been initialized or not (if {@link #init(JavaPlugin, int, PLogger)} has been
     * invoked) and {@link #get()} is safe to use.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized()
    {
        return instance != null;
    }

    /**
     * A functional interface to compare two version Strings with similar version schemes.
     */
    @FunctionalInterface
    public static interface VersionScheme
    {

        /**
         * Compare two versions and return the higher of the two. If null is returned, it is assumed that at least one
         * of the two versions are unsupported by this version scheme parser.
         *
         * @param first  the first version to check
         * @param second the second version to check
         * @return the greater of the two versions. null if unsupported version schemes
         */
        @NotNull String compareVersions(String first, String second);

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
        @NotNull
        private final UpdateReason reason;
        @NotNull
        private final String newestVersion;
        private final long age;

        { // An actual use for initializer blocks. This is madness!
            lastResult = this;
        }

        private UpdateResult(final @NotNull UpdateReason reason, final @NotNull String newestVersion, final long age)
        {
            this.reason = reason;
            this.newestVersion = newestVersion;
            this.age = age;
        }

        private UpdateResult(final @NotNull UpdateReason reason)
        {
            Preconditions.checkArgument(reason != UpdateReason.NEW_UPDATE && reason != UpdateReason.UP_TO_DATE,
                                        "Reasons that might require updates must also provide the latest version String");
            this.reason = reason;
            newestVersion = plugin.getDescription().getVersion();
            age = -1;
        }

        /**
         * Gets the constant reason of this result.
         *
         * @return the reason
         */
        public @NotNull UpdateReason getReason()
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
        public @NotNull String getNewestVersion()
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
