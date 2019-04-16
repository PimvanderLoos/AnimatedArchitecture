package nl.pim16aap2.bigDoors;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.jcalculator.JCalculator;

public class EconomyManager
{
    // Try to store the price of the doors as integers, because that's faster than
    // evaluating the formula.
    Integer doorPrice, drawbridgePrice, portcullisPrice, elevatorPrice, slidingDoorPrice, flagPrice;

    private final BigDoors plugin;
    private final HashMap<Long, Double> menu;
    private Economy economy;
    private final boolean vaultEnabled;

    public EconomyManager(BigDoors plugin)
    {
        this.plugin = plugin;
        menu = new HashMap<>();
        init();
        vaultEnabled = setupEconomy();
    }

    public boolean buyDoor(Player player, DoorType type, int blockCount)
    {
        if (!vaultEnabled)
            return true;
        double price = getPrice(type, blockCount);
        if (withdrawPlayer(player, price))
            return true;

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

    private double evaluateFormula(String formula, int blockCount)
    {
        formula = java.util.regex.Pattern.compile("blockCount").matcher(formula).replaceAll(Integer.toString(blockCount));

        try
        {
            return JCalculator.getResult(formula);
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
            price = evaluateFormula(plugin.getConfigLoader().doorPrice(), blockCount);
            break;

        case DRAWBRIDGE:
            if (drawbridgePrice != null)
                price = drawbridgePrice;
            price = evaluateFormula(plugin.getConfigLoader().drawbridgePrice(), blockCount);
            break;

        case ELEVATOR:
            if (elevatorPrice != null)
                price = elevatorPrice;
            price = evaluateFormula(plugin.getConfigLoader().elevatorPrice(), blockCount);
            break;

        case FLAG:
            if (flagPrice != null)
                price = flagPrice;
            price = evaluateFormula(plugin.getConfigLoader().flagPrice(), blockCount);
            break;

        case PORTCULLIS:
            if (portcullisPrice != null)
                price = portcullisPrice;
            price = evaluateFormula(plugin.getConfigLoader().portcullisPrice(), blockCount);
            break;

        case SLIDINGDOOR:
            if (slidingDoorPrice != null)
                price = slidingDoorPrice;
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
        return economy.has(player, amount);
    }

    private boolean withdrawPlayer(OfflinePlayer player, double amount)
    {
        if (has(player, amount))
            return economy.withdrawPlayer(player, amount).type.equals(EconomyResponse.ResponseType.SUCCESS);
        return false;
    }

    private boolean withdrawPlayer(Player player, double amount)
    {
        return withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), amount);
    }

    private boolean setupEconomy()
    {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            economy = economyProvider.getProvider();

        return (economy != null);
    }
}