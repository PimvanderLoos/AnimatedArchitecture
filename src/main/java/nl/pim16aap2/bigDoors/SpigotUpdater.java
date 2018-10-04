package nl.pim16aap2.bigDoors;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import nl.pim16aap2.bigDoors.handlers.LoginMessageHandler;

public class SpigotUpdater
{
    final static String VERSION_URL = "https://api.spiget.org/v2/resources/58669/versions?size=" + Integer.MAX_VALUE + "&spiget__ua=SpigetDocs";
    final static String INFO_URL    = "https://api.spiget.org/v2/resources/58669";
	private int project             = 0;
	private String newVersion       = "";
	private boolean alreadyChecked  = false;
	private BigDoors plugin;

	public SpigotUpdater(BigDoors plugin, int projectID)
	{
		this.plugin     = plugin;
		this.newVersion = plugin.getDescription().getVersion();
		this.project    = projectID;
		
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				try 
			    {	// Check for updates, download them if allowed, otherwise inform where to dl it manually.
			        if (!alreadyChecked && checkForUpdates())
			        {
			        		alreadyChecked = true;
			        		Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(plugin, "The BigDoors plugin is out of date. Found: "  + 
		        			getLatestVersion() + ", Currently running: " + plugin.getDescription().getVersion()), plugin);
			        		if (!plugin.getConfigLoader().getBool("auto-update"))
			        			 plugin.getLogger().info("An update was found! New version: " + getLatestVersion() + " download: " + getResourceURL() +
			        	                                 ", Currently running: " + plugin.getDescription().getVersion());
			        }
			    }
			    catch (Exception exc) 
			    {
			    		plugin.getMyLogger().logMessage("Could not check for updates! Send this to pim16aap2: \n" + exc.getStackTrace().toString(), true, false);
			    }
			}
		}.runTaskTimer(plugin, 100, 12000);
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

	public boolean checkForUpdates() throws Exception
	{
		this.newVersion = getNewVersion();
		boolean isLatestVersion = plugin.getDescription().getVersion().equals(newVersion);
		if (!isLatestVersion && plugin.getConfigLoader().getBool("auto-update"))
			runUpdate();
		return !isLatestVersion;
	}
	
	private String getNewVersion() throws MalformedURLException, ParseException, IOException
	{
		JSONArray versionsArray = (JSONArray) JSONValue.parseWithException(IOUtils.toString(new URL(String.valueOf(VERSION_URL))));
		return ((JSONObject) versionsArray.get(versionsArray.size() - 1)).get("name").toString();
	}
	
	private void runUpdate()
	{
		plugin.getMyLogger().info("Attempting to download update now!");
		try
		{
			URL dlURL = new URL(INFO_URL + "/download");
			File updateFolder = Bukkit.getUpdateFolderFile();
			if (!updateFolder.exists()) 
				if (!updateFolder.mkdirs()) 
					return; // TODO: Use a proper solution;

			
            HttpURLConnection httpConnection = (HttpURLConnection) dlURL.openConnection();
            httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");

            int grabSize = 2048;

            String fileName = plugin.getName() + ".jar";
            File updateFile = new File(updateFolder + "/" + fileName);
            
            BufferedInputStream in    = new BufferedInputStream(httpConnection.getInputStream());
            FileOutputStream fos      = new FileOutputStream(updateFile);
            BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

            byte[] data = new byte[grabSize];
            int grab;
            while ((grab = in.read(data, 0, grabSize)) >= 0) 
            {
                bout.write(data, 0, grab);
            }

            bout.close();
            in.close();
            fos.close();
            plugin.getMyLogger().info("Update downloaded! Restart to apply it!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
