package nl.pim16aap2.bigdoors.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import nl.pim16aap2.bigdoors.BigDoors;

public class ConfigLoader
{
    private static final List<String> DEFAULTPOWERBLOCK = new ArrayList<>(Arrays.asList("GOLD_BLOCK"));

    private String dbFile;
    private final String header;
    private int coolDown;
    private boolean makeBackup;
    private boolean allowStats;
    private int maxDoorSize;
    private double pcMultiplier = 1.0;
    private double dbMultiplier = 1.0;
    private double bdMultiplier = 1.0;
    private double sdMultiplier = 1.0;
    private double flMultiplier = 1.0;
    private double elMultiplier = 1.0;
    private String resourcePack;
    private String languageFile;
    private int maxDoorCount;
    private int cacheTimeout;
    private boolean autoDLUpdate;
    private int downloadDelay;
    private boolean enableRedstone;
    private HashSet<Material> powerBlockTypesMap;
    private boolean worldGuardHook;
    private boolean checkForUpdates;
    private boolean plotSquaredHook;
    private int headCacheTimeout;
    private String doorPrice, drawbridgePrice, portcullisPrice, elevatorPrice, slidingDoorPrice, flagPrice;
    private final ArrayList<ConfigOption<?>> configOptionsList;
    public static boolean DEBUG = false;
    private final BigDoors plugin;

    public ConfigLoader(BigDoors plugin)
    {
        this.plugin = plugin;
        configOptionsList = new ArrayList<>();
        powerBlockTypesMap = new HashSet<>();
        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";
        makeConfig();
    }

    // Read the current config, the make a new one based on the old one or default
    // values, whichever is applicable.
    private void makeConfig()
    {
        String defResPackUrl = "https://www.dropbox.com/s/0q6h8jkfjqrn1tp/BigDoorsResourcePack.zip?dl=1";
        String defResPackUrl1_13 = "https://www.dropbox.com/s/al4idl017ggpnuq/BigDoorsResourcePack-1_13.zip?dl=1";

        String[] enableRedstoneComment = { "Allow doors to be opened using redstone signals." };
        String[] powerBlockTypeComment = { "Choose the type of the power block that is used to open doors using redstone.",
                                           "A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
                                           "This is the block that will open the door attached to it when it receives a redstone signal.",
                                           "Multiple types are allowed."};
        String[] maxDoorCountComment = { "Maximum number of doors a player can own. -1 = infinite." };
        String[] languageFileComment = { "Specify a language file to be used. Note that en_US.txt will get regenerated!" };
        String[] dbFileComment = { "Pick the name (and location if you want) of the database." };
        String[] checkForUpdatesComment = { "Allow this plugin to check for updates on startup. It will not download new versions!" };
        String[] downloadDelayComment = { "Time (in minutes) to delay auto downloading updates after their release.",
                                          "Setting it to 1440 means that updates will be downloaded 24h after their release.",
                                          "This is useful, as it will mean that the update won't get downloaded if I decide to pull it for some reason",
                                          "(within the specified timeframe, of course)." };
        String[] autoDLUpdateComment = { "Allow this plugin to automatically download new updates. They will be applied on restart." };
        String[] allowStatsComment = { "Allow this plugin to send (anonymised) stats using bStats. Please consider keeping it enabled.",
                                       "It has a negligible impact on performance and more users on stats keeps me more motivated to support this plugin!" };
        String[] maxDoorSizeComment = { "Max. number of blocks allowed in a door.",
                                        "If this number is exceeded, doors will open instantly and skip the animation." };
        String[] resourcePackComment = { "This plugin uses a support resource pack for things suchs as sound.",
                                         "You can let this plugin load the resource pack for you or load it using your server.properties if you prefer that.",
                                         "Of course, you can also disable the resource pack altogether as well. Just put \"NONE\" (without quotation marks) as url.",
                                         "The default resource pack for 1.11.x/1.12.x is: \'" + defResPackUrl + "'",
                                         "The default resource pack for 1.13.x is: \'" + defResPackUrl1_13 + "\'" };
        String[] multiplierComment = { "These multipliers affect the opening/closing speed of their respective door types.",
                                       "Note that the maximum speed is limited, so beyond a certain point rasising these values won't have any effect.",
                                       "To use the default values, set them to \"0.0\" or \"1.0\" (without quotation marks).",
                                       "bd = Big Door, pc = PortCullis, db = DrawBridge, sd = Sliding Door, fl = FLag, el = ELevator",
                                       "Note that everything is optimized for default values, so it's recommended to leave this setting as-is." };
        String[] compatibilityHooks = { "Enable or disable compatibility hooks for certain plugins. If the plugins aren't installed, these options do nothing.",
                                        "When enabled, doors cannot be opened or created in areas not owned by the door's owner." };
        String[] coolDownComment = { "Cooldown on using doors. Time is measured in seconds." };
        String[] cacheTimeoutComment = { "Amount of time (in minutes) to cache powerblock positions. -1 means no caching (not recommended!), 0 = infinite cache.",
                                         "Doesn't take up a lot of RAM, so it's recommended to leave this value high. It'll get updated automatically when needed anyway." };
        String[] pricesComment = { "When Vault is present, you can set the price of door creation here for every type of door.",
                                   "You can use the word \"blockCount\" (without quotationmarks, case sensitive) as a variable that will be replaced by the actual blockCount.",
                                   "Furthermore, you can use these operators: -, +, *, /, sqrt(), ^, %, min(a,b), max(a,b), abs(), and parentheses.",
                                   "For example: \"doorPrice='max(10, sqrt(16)^4/100*blockCount)'\" would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.",
                                   "You must always put the formula or simple value or whatever in quotation marks! Also, these settings do nothing if Vault isn't installed!" };

//        String[] headCacheTimeoutComment = { "Amount of time (in minutes) to cache player heads. -1 means no caching (not recommended!), 0 = infinite cache.",
//                                             "Takes up a bit more space than the powerblock caching, but makes GUI much faster." };

        String[] debugComment = { "Don't use this. Just leave it on false." };
        String[] backupComment = { "Make a backup of the database before upgrading it. I'd recommend leaving this on true. ",
                                   "In case anything goes wrong, you can just revert to the old version! Only the most recent backup will be kept." };

        FileConfiguration config = plugin.getConfig();

        enableRedstone = addNewConfigOption(config, "allowRedstone", true, enableRedstoneComment);
        readPowerBlockConfig(config, powerBlockTypeComment);
        maxDoorCount = addNewConfigOption(config, "maxDoorCount", -1, maxDoorCountComment);
        maxDoorSize = addNewConfigOption(config, "maxDoorSize", -1, maxDoorSizeComment);
        languageFile = addNewConfigOption(config, "languageFile", "en_US", languageFileComment);
        dbFile = addNewConfigOption(config, "dbFile", "doorDB.db", dbFileComment);
        checkForUpdates = addNewConfigOption(config, "checkForUpdates", true, checkForUpdatesComment);
        autoDLUpdate = addNewConfigOption(config, "auto-update", true, autoDLUpdateComment);
        downloadDelay = addNewConfigOption(config, "downloadDelay", 1440, downloadDelayComment);
        allowStats = addNewConfigOption(config, "allowStats", true, allowStatsComment);
        worldGuardHook = addNewConfigOption(config, "worldGuard", true, compatibilityHooks);
        plotSquaredHook = addNewConfigOption(config, "plotSquared", true, null);
        resourcePack = addNewConfigOption(config, "resourcePack", plugin.is1_13() ? defResPackUrl1_13 : defResPackUrl,
                                          resourcePackComment);
        bdMultiplier = addNewConfigOption(config, "bdMultiplier", 0.0D, multiplierComment);
        pcMultiplier = addNewConfigOption(config, "pcMultiplier", 0.0D, null);
        dbMultiplier = addNewConfigOption(config, "dbMultiplier", 0.0D, null);
        sdMultiplier = addNewConfigOption(config, "sdMultiplier", 0.0D, null);
        flMultiplier = addNewConfigOption(config, "flMultiplier", 0.0D, null);
        elMultiplier = addNewConfigOption(config, "elMultiplier", 0.0D, null);
        coolDown = addNewConfigOption(config, "coolDown", 0, coolDownComment);
        makeBackup = addNewConfigOption(config, "makeBackup", true, backupComment);
        cacheTimeout = addNewConfigOption(config, "cacheTimeout", 0, cacheTimeoutComment);
        doorPrice = addNewConfigOption(config, "doorPrice", "0", pricesComment);
        drawbridgePrice = addNewConfigOption(config, "drawbridgePrice", "0", null);
        portcullisPrice = addNewConfigOption(config, "portcullisPrice", "0", null);
        elevatorPrice = addNewConfigOption(config, "elevatorPrice", "0", null);
        slidingDoorPrice = addNewConfigOption(config, "slidingDoorPrice", "0", null);
        flagPrice = addNewConfigOption(config, "flagPrice", "0", null);

        // This is a bit special, as it's public static (for util debug messages).
        ConfigLoader.DEBUG = addNewConfigOption(config, "DEBUG", false, debugComment);

        writeConfig();
    }

    private void readPowerBlockConfig(FileConfiguration config, String[] powerBlockTypeComment)
    {
        List<String> materials;

        {
            List<?> materialsTmp = config.getList("powerBlockTypes", DEFAULTPOWERBLOCK);
            // If the user is illiterate and failed to read the part saying it should be an
            // enum string and used
            // non-String values, put those in a new String list.
            materials = new ArrayList<>();
            materialsTmp.forEach(K -> materials.add(K.toString()));
        }

        // Try to put all the found materials into a String map.
        // Only valid and solid material Strings are allowed.
        // Everything else is thrown out.
        Iterator<String> it = materials.iterator();
        while (it.hasNext())
        {
            String str = it.next();
            try
            {
                Material mat = Material.valueOf(str);
                if (mat.isSolid())
                    powerBlockTypesMap.add(mat);
                else
                {
                    plugin.getMyLogger()
                        .logMessage("Failed to add material: \"" + str + "\". Only solid materials are allowed!", true,
                                    false);
                    it.remove();
                }
            }
            catch (Exception e)
            {
                plugin.getMyLogger().logMessage("Failed to parse material: \"" + str + "\"", true, false);
                it.remove();
            }
        }

        // If the user didn't supply even a single valid block, use the default.
        if (powerBlockTypesMap.size() == 0)
        {
            StringBuilder sb = new StringBuilder();
            DEFAULTPOWERBLOCK.forEach(K -> sb.append(K + " "));
            plugin.getMyLogger().logMessage("No materials found for powerBlockType! Defaulting to:" + sb.toString(),
                                            true, false);
            DEFAULTPOWERBLOCK.forEach(K ->
            {
                powerBlockTypesMap.add(Material.valueOf(K));
                materials.add(K);
            });
        }

        addNewConfigOption(config, "powerBlockTypes", materials, powerBlockTypeComment);


        plugin.getMyLogger().logMessageToConsoleOnly("Power Block Types:");
        powerBlockTypesMap.forEach(K -> plugin.getMyLogger().logMessageToConsoleOnly(" - " + K.toString()));
    }

    private <T> T addNewConfigOption(FileConfiguration config, String optionName, T defaultValue, String[] comment)
    {
        ConfigOption<T> option = new ConfigOption<>(plugin, config, optionName, defaultValue, comment);
        configOptionsList.add(option);
        return option.getValue();
    }

    // Write new config file.
    private void writeConfig()
    {
        // Write all the config options to the config.yml.
        try
        {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists())
                dataFolder.mkdir();

            File saveTo = new File(plugin.getDataFolder(), "config.yml");
            if (!saveTo.exists())
                saveTo.createNewFile();
            else
            {
                saveTo.delete();
                saveTo.createNewFile();
            }
            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);

            if (header != null)
                pw.println("# " + header + "\n");

            for (int idx = 0; idx < configOptionsList.size(); ++idx)
                pw.println(configOptionsList.get(idx).toString() +
                // Only print an additional newLine if the next config option has a comment.
                    (idx < configOptionsList.size() - 1 && configOptionsList.get(idx + 1).getComment() == null ? ""
                                                                                                               : "\n"));

            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            Bukkit.getLogger()
                .log(Level.SEVERE,
                     "Could not save config.yml! Please contact pim16aap2 and show him the following code:");
            e.printStackTrace();
        }
    }

    public String dbFile()
    {
        return dbFile;
    }

    public boolean dbBackup()
    {
        return makeBackup;
    }

    public int coolDown()
    {
        return coolDown;
    }

    public boolean allowStats()
    {
        return allowStats;
    }

    public int maxDoorSize()
    {
        return maxDoorSize;
    }

    public int cacheTimeout()
    {
        return cacheTimeout;
    }

    public double pcMultiplier()
    {
        return pcMultiplier;
    }

    public double dbMultiplier()
    {
        return dbMultiplier;
    }

    public double bdMultiplier()
    {
        return bdMultiplier;
    }

    public double sdMultiplier()
    {
        return sdMultiplier;
    }

     public double flMultiplier()
    {
        return flMultiplier;
    }

     public double elMultiplier()
    {
        return elMultiplier;
    }

    public String resourcePack()
    {
        return resourcePack;
    }

    public String languageFile()
    {
        return languageFile;
    }

    public int maxdoorCount()
    {
        return maxDoorCount;
    }

    public boolean autoDLUpdate()
    {
        return autoDLUpdate;
    }

    public int downloadDelay()
    {
        return downloadDelay;
    }

    public boolean enableRedstone()
    {
        return enableRedstone;
    }

    public HashSet<Material> powerBlockTypes()
    {
        return powerBlockTypesMap;
    }

    public boolean worldGuardHook()
    {
        return worldGuardHook;
    }

    public boolean checkForUpdates()
    {
        return checkForUpdates;
    }

    public boolean plotSquaredHook()
    {
        return plotSquaredHook;
    }

    public int headCacheTimeout()
    {
        return headCacheTimeout;
    }

    public String doorPrice()
    {
        return doorPrice;
    }

    public String drawbridgePrice()
    {
        return drawbridgePrice;
    }

    public String portcullisPrice()
    {
        return portcullisPrice;
    }

    public String elevatorPrice()
    {
        return elevatorPrice;
    }

    public String slidingDoorPrice()
    {
        return slidingDoorPrice;
    }

    public String flagPrice()
    {
        return flagPrice;
    }
}