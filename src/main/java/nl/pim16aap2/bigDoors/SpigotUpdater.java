package nl.pim16aap2.bigDoors;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import nl.pim16aap2.bigDoors.util.Util;

public class SpigotUpdater
{
    final static String DOWNLOAD_URL = "https://api.spiget.org/v2/resources/58669/versions/";
    final static String VERSIONS_URL = "https://api.spiget.org/v2/resources/58669/versions?size=" +
                                        Integer.MAX_VALUE + "&spiget__ua=SpigetDocs";
    final static String INFO_URL     = "https://api.spiget.org/v2/resources/58669";
    private int project              = 0;
    private int versionsAgo          = 0;
    private String newVersion        = "";
    private int newVersionID         = -1;
    private boolean success          = false;
    private BigDoors plugin;

    public SpigotUpdater(BigDoors plugin, int projectID)
    {
        this.plugin = plugin;
        newVersion  = plugin.getDescription().getVersion();
        project     = projectID;

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // Check for updates, download them if allowed, otherwise inform where to dl it manually.
                    if (newerVersionAvailable())
                    {
                        if (!plugin.getConfigLoader().autoDLUpdate() || versionsAgo > 1)
                        {
                             plugin.getLogger().info("An update was found! New version: " +
                                     getLatestVersion() + " download: " + getResourceURL() +
                                     ", Currently running: " + plugin.getDescription().getVersion());
                             plugin.setLoginString("The BigDoors plugin is out of date. Found: " +
                                     getLatestVersion() + ", Currently running: " +
                                     plugin.getDescription().getVersion());
                        }
                        else if (success)
                            plugin.setLoginString("Update for BigDoors has been downloaded! Restart to apply it!");
                    }
                }
                catch (Exception exc)
                {
                    plugin.getMyLogger().logMessageToConsole("Failed to check for updates!");
                    plugin.getMyLogger().logMessage("Could not check for updates! Send this to pim16aap2: \n" +
                            Util.exceptionToString(exc), false, false);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 50, 288000);
    }

    public int getProjectID()
    {
        return project;
    }

    public JavaPlugin getPlugin()
    {
        return plugin;
    }

    public String getLatestVersion()
    {
        return newVersion;
    }

    public String getResourceURL()
    {
        return "https://www.spigotmc.org/resources/" + project;
    }

    private int getAge(long updateTime)
    {
        long currentTime   = Instant.now().getEpochSecond();
        return (int) (currentTime - updateTime);
    }

    // Get update history. Then go through all of them until on is found that is both newer than the current version
    // And old enough that it's allowed to be downloaded (as set in the config file).
    private boolean getNewVersion() throws MalformedURLException, ParseException, IOException
    {
        int MIN_AGE = plugin.getConfigLoader().downloadDelay() * 60;
        int age = -1000; // Default 1 second, so 0 or negative seconds = don't wait.
        JSONArray versionsArray = (JSONArray) JSONValue.parseWithException(IOUtils.toString(new URL(String.valueOf(VERSIONS_URL))));
        int count = 0;
        newVersion   = "";
        newVersionID = -1;
        versionsAgo  = 0;
        if (MIN_AGE > 0)
        {
            while (age < MIN_AGE && count < versionsArray.size())
            {
                ++count;
                String versionName = ((JSONObject) versionsArray.get(versionsArray.size() - count)).get("name").toString();
                if (plugin.getDescription().getVersion().equals(versionName))
                    return false;
                age = getAge(Long.parseLong(((JSONObject) versionsArray.get(versionsArray.size() - count)).get("releaseDate").toString()));
            }
        }
        else
            ++count;
        if (versionsArray.size() != count)
        {
            versionsAgo  = count;
            newVersion   = ((JSONObject) versionsArray.get(versionsArray.size() - count)).get("name").toString();
            newVersionID = Integer.parseInt(((JSONObject) versionsArray.get(versionsArray.size() - count)).get("id").toString());
        }
        return true;
    }

    public boolean newerVersionAvailable() throws Exception
    {
        if (!getNewVersion())
            return false;
        boolean isLatestVersion = plugin.getDescription().getVersion().equals(newVersion);
        if (!isLatestVersion && plugin.getConfigLoader().autoDLUpdate())
            runUpdate();
        return !isLatestVersion;
    }

    private URL getLatestAllowedURL() throws MalformedURLException
    {
//        return new URL(DOWNLOAD_URL + this.newVersionID + "/download");
//        return this.versionsAgo == 1 ? new URL(SpigotUpdater.INFO_URL + "/download") : null;
        return new URL(SpigotUpdater.INFO_URL + "/download");
    }

    private void runUpdate() throws IOException
    {
        URL dlURL = getLatestAllowedURL();
        if (versionsAgo > 1)
        {
            success = false;
            plugin.getMyLogger().logMessage("Update available! (" + newVersion
                    + ", current version = " + plugin.getDescription().getVersion() + ")\n"
                    + "Cannot be downloaded automatically, as it is not the most recent version "
                    + "(the config doesn't allow me to download that!).\n"
                    + "You can download it manually at: " + (SpigotUpdater.DOWNLOAD_URL
                    + newVersionID + "/download"), true, false);
            return;
        }
        File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists())
            if (!updateFolder.mkdirs())
                throw new RuntimeException("Failed to create update folder!");

        String fileName = plugin.getName() + ".jar";
        File updateFile = new File(updateFolder + "/" + fileName);

        HttpURLConnection httpConnection = (HttpURLConnection) dlURL.openConnection();
        httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
        if (httpConnection.getResponseCode() != 200)
            throw new RuntimeException("Download returned status #" + httpConnection.getResponseCode() + "\n for URL: " + dlURL);

//        // CloudFlare blocks direct access to spigotmc. Getting a specific version using Spiget,
//        // You get a redirect to that specific version. Might be useful as a return message,
//        // but it can't be used to download it automatically.
//        httpConnection.setInstanceFollowRedirects(false);
//        while ((responseCode / 100) == 3)
//        { /* codes 3XX are redirections */
//           String newLocationHeader = httpConnection.getHeaderField("Location");
//           httpConnection.disconnect();
//           httpConnection = (HttpURLConnection) (new URL(newLocationHeader)).openConnection();
//           httpConnection.setInstanceFollowRedirects(false);
//           httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
//           responseCode = httpConnection.getResponseCode();
//        }

        int grabSize = 512;
        BufferedInputStream  in   = new BufferedInputStream(httpConnection.getInputStream());
        FileOutputStream     fos  = new FileOutputStream(updateFile);
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
        success = true;
        plugin.getMyLogger().info("Update downloaded! Restart to apply it! New version is " +
                newVersion + ", Currently running " + plugin.getDescription().getVersion());
    }
}
