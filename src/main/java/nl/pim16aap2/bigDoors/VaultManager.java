package nl.pim16aap2.bigDoors;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.jcalculator.JCalculator;

public class VaultManager
{
    // Try to store the price of the doors as integers, because that's faster than
    // evaluating the formula.
    Integer doorPrice, drawbridgePrice, portcullisPrice, elevatorPrice, slidingDoorPrice, flagPrice;
    private final static double EPSILON = 0.00001;

    private final BigDoors plugin;
    private final HashMap<Long, Double> menu;
    private Economy economy = null;
    private Permission perms = null;
    private final boolean vaultEnabled;

    public VaultManager(BigDoors plugin)
    {
        this.plugin = plugin;
        menu = new HashMap<>();
        init();
        vaultEnabled = setupEconomy();
        if (vaultEnabled)
            setupPermissions();
    }

    public boolean buyDoor(Player player, DoorType type, int blockCount)
    {
        if (!vaultEnabled)
            return true;
        double price = getPrice(type, blockCount);
        if (price < EPSILON)
            return true;

        if (withdrawPlayer(player, player.getWorld().getName(), price))
        {
            if (price > 0)
                Util.messagePlayer(player, plugin.getMessages().getString("CREATOR.GENERAL.MoneyWithdrawn") + " " + price);
            return true;
        }

        Util.messagePlayer(player, plugin.getMessages().getString("CREATOR.GENERAL.InsufficientFunds") + " " + price);
        return false;
    }

    public void init()
    {
        menu.clear();
        try
        {
            doorPrice = Integer.parseInt(plugin.getConfigLoader().doorPrice());
        }
        catch(Exception e)
        {
            doorPrice = null;
        }

        try
        {
            drawbridgePrice = Integer.parseInt(plugin.getConfigLoader().drawbridgePrice());
        }
        catch(Exception e)
        {
            drawbridgePrice = null;
        }

        try
        {
            portcullisPrice = Integer.parseInt(plugin.getConfigLoader().portcullisPrice());
        }
        catch(Exception e)
        {
            portcullisPrice = null;
        }

        try
        {
            elevatorPrice = Integer.parseInt(plugin.getConfigLoader().elevatorPrice());
        }
        catch(Exception e)
        {
            elevatorPrice = null;
        }

        try
        {
            slidingDoorPrice = Integer.parseInt(plugin.getConfigLoader().slidingDoorPrice());
        }
        catch(Exception e)
        {
            slidingDoorPrice = null;
        }

        try
        {
            flagPrice = Integer.parseInt(plugin.getConfigLoader().flagPrice());
        }
        catch(Exception e)
        {
            flagPrice = null;
        }
    }

    public boolean hasPermission(Player player, String permission)
    {
        return vaultEnabled && perms.playerHas(player.getWorld().getName(), player, permission);
    }

    public boolean hasPermission(OfflinePlayer player, String permission, String worldName)
    {
        return vaultEnabled && perms.playerHas(worldName, player, permission);
    }

    private double evaluateFormula(String formula, int blockCount)
    {
        try
        {
            return JCalculator.getResult(formula, new String[]{"blockCount"}, new double[]{blockCount});
        }
        catch (Exception e)
        {
            plugin.getMyLogger().logMessageToConsole("Failed to determine door creation price! Please contact pim16aap2! Include this: \"" + formula + "\"");
            e.printStackTrace();
            return 0;
        }
    }

    public double getPrice(DoorType type, int blockCount)
    {
        if (!vaultEnabled)
            return 0;

        // Try cache first
        long priceID = blockCount * 100 + DoorType.getValue(type);
        if (menu.containsKey(priceID))
            return menu.get(priceID);

        double price = 0;

        switch(type)
        {
        case DOOR:
            if (doorPrice != null)
                price = doorPrice;
            else
                price = evaluateFormula(plugin.getConfigLoader().doorPrice(), blockCount);
            break;

        case DRAWBRIDGE:
            if (drawbridgePrice != null)
                price = drawbridgePrice;
            else
                price = evaluateFormula(plugin.getConfigLoader().drawbridgePrice(), blockCount);
            break;

        case ELEVATOR:
            if (elevatorPrice != null)
                price = elevatorPrice;
            else
                price = evaluateFormula(plugin.getConfigLoader().elevatorPrice(), blockCount);
            break;

        case FLAG:
            if (flagPrice != null)
                price = flagPrice;
            else
                price = evaluateFormula(plugin.getConfigLoader().flagPrice(), blockCount);
            break;

        case PORTCULLIS:
            if (portcullisPrice != null)
                price = portcullisPrice;
            else
                price = evaluateFormula(plugin.getConfigLoader().portcullisPrice(), blockCount);
            break;

        case SLIDINGDOOR:
            if (slidingDoorPrice != null)
                price = slidingDoorPrice;
            else
                price = evaluateFormula(plugin.getConfigLoader().slidingDoorPrice(), blockCount);
            break;
        }

        // Negative values aren't allowed.
        price = Math.max(0, price);
        menu.put(priceID, price);
        return price;
    }

    private boolean has(OfflinePlayer player, double amount)
    {
        if (amount < EPSILON)
            return true;
        try
        {
            return economy.has(player, amount);
        }
        catch(Exception e)
        {
            plugin.getMyLogger().warn("Failed to check balance of player \"" + player.getName() + "\" (" + player.getUniqueId() + ")! Please contact pim16aap2!");
            plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
        }
        return true;
    }

    private boolean withdrawPlayer(OfflinePlayer player, String worldName, double amount)
    {
        if (amount < EPSILON)
            return true;
        try
        {
            if (has(player, amount))
                return economy.withdrawPlayer(player, worldName, amount).type.equals(EconomyResponse.ResponseType.SUCCESS);
            return false;
        }
        catch(Exception e)
        {
            plugin.getMyLogger().warn("Failed to subtract money from player \"" + player.getName() + "\" (" + player.getUniqueId() + ")! Please contact pim16aap2!");
            plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
        }
        return true;
    }

    private boolean withdrawPlayer(Player player, String worldName, double amount)
    {
        return withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), worldName, amount);
    }

    private boolean setupEconomy()
    {
        try
        {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            economy = economyProvider.getProvider();

        return (economy != null);
        }
        catch (Exception e)
        {
            plugin.getMyLogger().logMessage("Exception encountered while initiating Vault dependency! It will be disabled! Please contact pim16aap2!", true, false);
            plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
            return false;
        }
    }

    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
}