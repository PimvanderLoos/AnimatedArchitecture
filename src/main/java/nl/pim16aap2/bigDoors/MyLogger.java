package nl.pim16aap2.bigDoors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.util.Util;

public class MyLogger
{
    private final BigDoors plugin;
    private File logFile;
    private int debugLevel = 100;
    
    public MyLogger(BigDoors plugin, File logFile)
    {
        this.plugin  = plugin;
        this.logFile = logFile;
        loadLog();
    }
    
    // Change debug level.
    public void setDebugLevel(int level)
    {
        this.debugLevel = level;
    }
    
    // Initialise log
    public void loadLog()
    {
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                myLogger(Level.INFO, "New file created at " + logFile);
            }
            catch (IOException e)
            {
                myLogger(Level.SEVERE, "File write error: " + logFile);
            }
        }
    }
    
    // Print a string to the console.
    public void myLogger(Level level, String str)
    {
        Bukkit.getLogger().log(level, "[" + plugin.getName() + "] " + str);
    }
    
    // Send a message to whomever issued a command.
    public void returnToSender(CommandSender sender, Level level, ChatColor color, String str)
    {
        if (sender instanceof Player)
            Util.messagePlayer((Player) sender, color + str);
        else
            myLogger(level, str);
    }

    // Log a message to the log file. Can print to console and/or 
    // add some new lines before the message in the logfile to make it stand out.
    public void logMessage(String msg, boolean printToConsole, boolean startSkip)
    {
        if (printToConsole)
            myLogger(Level.WARNING, msg);
        BufferedWriter bw = null;
        try
        {
            bw = new BufferedWriter(new FileWriter(logFile, true));
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            if (startSkip)
                bw.write("\n\n[" + format.format(now) + "] " + msg);
            else
                bw.write("[" + format.format(now) + "] " + msg);
            bw.newLine();
            bw.flush();
        }
        catch (IOException e)
        {
            myLogger(Level.SEVERE, "Logging error! Could not log to logFile!");    
        }
    }
    
    // Log a message to the logfile. Does not print to console or add newlines in front of the actual message.
    public void logMessageToLogFile(String msg)
    {
        logMessage(msg, false, false);
    }
    
    public void logMessageToConsole(String msg)
    {
        logMessage(msg, true, false);
    }
    
    // Prints a debugmessage. Deprecated because there are much better systems now.
    @Deprecated
    public void debugMsg(int level, Level lvl, String msg)
    {
        if (level <= debugLevel)
            Bukkit.broadcastMessage("" + msg);
    }
    
    @Deprecated // Do not use this. Use the proper logging tools instead.
    public void debugMsg(Level lvl, String msg)
    {
        debugMsg(0, lvl, msg);
    }

    public void info(String str)
    {
        this.myLogger(Level.INFO, str);
    }
    
    public void warn(String str)
    {
        this.myLogger(Level.WARNING, str);
    }
}
