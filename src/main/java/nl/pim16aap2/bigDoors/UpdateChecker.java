package nl.pim16aap2.bigDoors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import nl.pim16aap2.bigDoors.util.Util;

/**
 * A utility class to assist in checking for updates for plugins uploaded to
 * <a href="https://github.com">GitHub</a>. Before any members of this class are
 * accessed, {@link #init(JavaPlugin)} must be invoked by the plugin,
 * preferrably in its {@link JavaPlugin#onEnable()} method, though that is not a
 * requirement.
 *
 * @author Parker Hawke - 2008Choco
 */
public final class UpdateChecker
{
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

    private static final URL UPDATE_URL;
    static
    {
        final String urlStr = "https://api.github.com/repos/PimvanderLoos/BigDoors_Releases/releases/latest";
        URL url = null;
        try
        {
            url = new URL(urlStr);
        }
        catch (IOException e)
        {
            BigDoors.get().getMyLogger().severe("Failed to construct URL from: \"" + urlStr
                + "\".\nPlease contact Pim about the stacktrace below:");
            e.printStackTrace();
        }
        UPDATE_URL = url;
    }

    private static final Pattern DECIMAL_SCHEME_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");

    private static UpdateChecker instance;

    private UpdateResult lastResult = null;

    private final BigDoors plugin;
    private final VersionScheme versionScheme;
    private final File updateFile;

    private UpdateChecker(final BigDoors plugin, final VersionScheme versionScheme)
    {
        this.plugin = plugin;
        this.versionScheme = versionScheme;
        updateFile = new File(Bukkit.getUpdateFolderFile() + "/" + plugin.getName() + ".jar");
    }

    /**
     * Requests an update check to GitHub. This request is asynchronous and may not
     * complete immediately as an HTTP GET request is published to the GitHub API.
     *
     * @return a future update result
     */
    public CompletableFuture<UpdateResult> requestUpdateCheck()
    {
        if (UPDATE_URL == null)
            return CompletableFuture.completedFuture(new UpdateResult(UpdateReason.UNKNOWN_ERROR));

        return CompletableFuture.supplyAsync(() ->
        {
            int responseCode = -1;
            try
            {
                final HttpURLConnection connection = (HttpURLConnection) UPDATE_URL.openConnection();

                final InputStreamReader iSReader = new InputStreamReader(connection.getInputStream());
                responseCode = connection.getResponseCode();

                // GSon doesn't like it when the json string doesn't start and end with square
                // brackets. So, read the json text and add some square brackets before feeding
                // it to GSon.
                final BufferedReader reader = new BufferedReader(iSReader);
                final StringBuilder jsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    jsonStr.append(line);

                iSReader.close();
                reader.close();

                final JsonElement element = new JsonParser().parse("[" + jsonStr.toString() + "]");
                if (!element.isJsonArray())
                    return new UpdateResult(UpdateReason.INVALID_JSON);

                final JsonObject latestRelease = element.getAsJsonArray().get(0).getAsJsonObject();
                final long age = UpdateChecker.getAgeInSeconds(latestRelease.get("published_at").getAsString());

                String jarUrl = "", hashUrl = "";
                for (final JsonElement asset : latestRelease.get("assets").getAsJsonArray())
                {
                    final JsonObject asset_object = asset.getAsJsonObject();
                    final String downloadUrl = asset_object.get("browser_download_url").getAsString();
                    if (downloadUrl.endsWith("jar"))
                        jarUrl = downloadUrl;
                    else if (downloadUrl.endsWith("256"))
                        hashUrl = downloadUrl;
                }

                final String hash = Util.readSHA256FromURL(new URL(hashUrl));
                if (hash.isEmpty())
                    return new UpdateResult(UpdateReason.INVALID_HASH);

                final String current = Util.getCleanedVersionString();
                final String available = Util.getCleanedVersionString(latestRelease.get("name").getAsString());
                final String highest = versionScheme.compareVersions(current, available);

                if (highest == null)
                    return new UpdateResult(UpdateReason.UNSUPPORTED_VERSION_SCHEME);

                // If the latest version is the same as the current version, the plugin is
                // up-to-date. However, if the current (and therefore also highest) is not the
                // same as the available version, then this means that the current version
                // is higher than the latest available version.
                else if (highest.equals(current))
                    return new UpdateResult(current.equals(available) ? UpdateReason.UP_TO_DATE :
                        UpdateReason.UNRELEASED_VERSION, current, age, jarUrl, hash);

                else if (highest.equals(available))
                    return new UpdateResult(UpdateReason.NEW_UPDATE, highest, age, jarUrl, hash);
            }
            catch (IOException e)
            {
                return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
            }
            catch (JsonSyntaxException e)
            {
                return new UpdateResult(UpdateReason.INVALID_JSON);
            }

            return new UpdateResult(responseCode == 401 ? UpdateReason.UNAUTHORIZED_QUERY : UpdateReason.UNKNOWN_ERROR);
        });
    }

    /**
     * Gets the last update result that was queried by
     * {@link #requestUpdateCheck()}. If no update check was performed since this
     * class' initialization, this method will return null.
     *
     * @return the last update check result. null if none.
     */
    public UpdateResult getLastResult()
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
     * Downloads the latest update. If an update has already been downloaded, the
     * new file will only be downloaded in case the existing update's checksum does
     * not match the checksum found in the {@link UpdateResult}.
     *
     * @param result The result of an update check.
     *
     * @return True if the download was successful.
     * @throws IOException
     */
    public boolean downloadUpdate(final UpdateResult result) throws IOException
    {
        final File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists())
            if (!updateFolder.mkdirs())
                throw new IOException("Failed to create update folder!");

        if (updateFile.exists() && result.getChecksum().equals(Util.getSHA256(updateFile)))
            return true;

        try (InputStream in = new URL(result.getDownloadUrl()).openStream())
        {
            Files.copy(in, updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
            return false;
        }

        if (!updateFile.exists())
            throw new IOException("Failed to save file!");

        final String checksum = Util.getSHA256(updateFile);
        boolean downloadSuccessfull = result.getChecksum().equals(checksum);
        if (!downloadSuccessfull)
        {
            plugin.getMyLogger().severe("Checksum of downloaded file did not match expected checksum!");
            plugin.getMyLogger().severe("Expected: " + result.getChecksum());
            plugin.getMyLogger().severe("Found: " + checksum);
            updateFile.delete();
        }

        return downloadSuccessfull;
    }

    /**
     * Initializes this update checker with the specified values and return its
     * instance. If an instance of UpdateChecker has already been initialized, this
     * method will act similarly to {@link #get()} (which is recommended after
     * initialization).
     *
     * @param plugin        The plugin for which to check updates. Cannot be null
     * @param versionScheme a custom version scheme parser. Cannot be null.
     * @return the UpdateChecker instance
     */
    public static UpdateChecker init(final BigDoors plugin, final VersionScheme versionScheme)
    {
        return (instance == null) ? instance = new UpdateChecker(plugin, versionScheme) : instance;
    }

    /**
     * Initializes this update checker with the specified values and return its
     * instance. If an instance of UpdateChecker has already been initialized, this
     * method will act similarly to {@link #get()} (which is recommended after
     * initialization).
     *
     * @param plugin The plugin for which to check updates. Cannot be null
     * @return the UpdateChecker instance
     */
    public static UpdateChecker init(final BigDoors plugin)
    {
        return init(plugin, VERSION_SCHEME_DECIMAL);
    }

    /**
     * Gets the initialized instance of UpdateChecker. If {@link #init(JavaPlugin)}
     * has not yet been invoked, this method will throw an exception.
     *
     * @return the UpdateChecker instance
     */
    public static UpdateChecker get()
    {
        Preconditions.checkState(instance != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

    /**
     * Checks whether the UpdateChecker has been initialized or not (if
     * {@link #init(JavaPlugin)} has been invoked) and {@link #get()} is safe to
     * use.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized()
    {
        return instance != null;
    }

    /**
     * Gets the difference in seconds between a given time and the current time.
     *
     * @param updateTime A moment in time to compare the current time to. Must be
     *                   ISO 8601.
     * @return The difference in seconds between a given time and the current time.
     */
    private static long getAgeInSeconds(final String time)
    {
        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(time);
        Date date = Date.from(Instant.from(temporalAccessor));
        return ((new Date()).getTime() - date.getTime()) / 1000;
    }

    /**
     * A functional interface to compare two version Strings with similar version
     * schemes.
     */
    @FunctionalInterface
    public static interface VersionScheme
    {

        /**
         * Compare two versions and return the higher of the two. If null is returned,
         * it is assumed that at least one of the two versions are unsupported by this
         * version scheme parser.
         *
         * @param first  the first version to check
         * @param second the second version to check
         * @return the greater of the two versions. null if unsupported version schemes
         */
        public String compareVersions(String first, String second);

    }

    /**
     * A constant reason for the result of {@link UpdateResult}.
     */
    public static enum UpdateReason
    {

        /**
         * A new update is available for download on SpigotMC.
         */
        NEW_UPDATE, // The only reason that requires an update

        /**
         * A successful connection to the GitHub API could not be established.
         */
        COULD_NOT_CONNECT,

        /**
         * The JSON retrieved from GitHub was invalid or malformed.
         */
        INVALID_JSON,

        /**
         * The retrieved hash was not valid.
         */
        INVALID_HASH,

        /**
         * A 401 error was returned by the GitHub API.
         */
        UNAUTHORIZED_QUERY,

        /**
         * The version of the plugin installed on the server is greater than the one
         * uploaded to SpigotMC's resources section.
         */
        UNRELEASED_VERSION,

        /**
         * An unknown error occurred.
         */
        UNKNOWN_ERROR,

        /**
         * The plugin uses an unsupported version scheme, therefore a proper comparison
         * between versions could not be made.
         */
        UNSUPPORTED_VERSION_SCHEME,

        /**
         * The plugin is up to date with the version released on SpigotMC's resources
         * section.
         */
        UP_TO_DATE

    }

    /**
     * Represents a result for an update query performed by
     * {@link UpdateChecker#requestUpdateCheck()}.
     */
    public final class UpdateResult
    {
        private final UpdateReason reason;
        private final String newestVersion;
        private final long age;
        private final String url;
        private final String sha256;

        { // An actual use for initializer blocks. This is madness!
            lastResult = this;
        }

        private UpdateResult(final UpdateReason reason, final String newestVersion, final long age, final String url,
            final String sha256)
        {
            this.reason = reason;
            this.newestVersion = newestVersion;
            this.age = age;
            this.url = url;
            this.sha256 = sha256;
        }

        private UpdateResult(final UpdateReason reason)
        {
            Preconditions
                .checkArgument(reason != UpdateReason.NEW_UPDATE && reason != UpdateReason.UP_TO_DATE,
                               "Reasons that might require updates must also provide the latest version String");
            this.reason = reason;
            newestVersion = plugin.getDescription().getVersion();
            age = -1;
            url = "";
            sha256 = "";
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
         * Gets the latest version of the plugin. This may be the currently installed
         * version, it may not be. This depends entirely on the result of the update.
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
         * @return The number of seconds since the last update was released or -1 if
         *         unavailable.
         */
        public long getAge()
        {
            return age;
        }

        /**
         * Gets the URL where the jar can be downloaded from.
         *
         * @return The URL where the jar can be found.
         */
        public String getDownloadUrl()
        {
            return url;
        }

        /**
         * Gets the sha256 checksum of the jar file.
         *
         * @return The sha256 checksum of the jar file.
         */
        public String getChecksum()
        {
            return sha256;
        }
    }
}
