package nl.pim16aap2.bigdoors.managers;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.jcalculator.JCalculator;

public class VaultManager extends Restartable
{
    private final HashMap<Long, Double> menu;
    private final HashMap<DoorType, Integer> flatPrices;
    private Economy economy = null;
    private Permission perms = null;
    private final boolean vaultEnabled;
    protected final BigDoors plugin;

    public VaultManager(final BigDoors plugin)
    {
        super(plugin);
        this.plugin = plugin;
        menu = new HashMap<>();
        flatPrices = new HashMap<>();
        init();
        vaultEnabled = setupEconomy();
        if (vaultEnabled)
            setupPermissions();
    }

    public boolean buyDoor(final Player player, final DoorType type, final int blockCount)
    {
        if (!vaultEnabled)
            return true;
        double price = getPrice(type, blockCount);
        if (withdrawPlayer(player, player.getWorld().getName(), price))
        {
            if (price > 0)
                SpigotUtil.messagePlayer(player, plugin.getMessages().getString("CREATOR.GENERAL.MoneyWithdrawn") + " " + price);
            return true;
        }

        SpigotUtil.messagePlayer(player, plugin.getMessages().getString("CREATOR.GENERAL.InsufficientFunds") + " " + price);
        return false;
    }

    private void addPrice(final DoorType type)
    {
        Integer price;
        try
        {
            price = Integer.parseInt(plugin.getConfigLoader().getPrice(DoorType.BIGDOOR));
        }
        catch(Exception e)
        {
            price = null;
        }
        flatPrices.put(type, price);
    }

    private void init()
    {
        for (DoorType type : DoorType.values())
            addPrice(type);
    }

    public boolean hasPermission(final Player player, final String permission)
    {
        return vaultEnabled && perms.playerHas(player.getWorld().getName(), player, permission);
    }

    private double evaluateFormula(String formula, final int blockCount)
    {
        formula = java.util.regex.Pattern.compile("blockCount").matcher(formula).replaceAll(Integer.toString(blockCount));

        try
        {
            return JCalculator.getResult(formula);
        }
        catch (Exception e)
        {
            plugin.getMyLogger().logException(e, "Failed to determine door creation price! Please contact pim16aap2! "
                + "Include this: \"" + formula + "\" and this:");
            return 0.0d;
        }
    }

    public double getPrice(final DoorType type, final int blockCount)
    {
        if (!vaultEnabled)
            return 0;

        // Try cache first
        long priceID = blockCount * 100 + DoorType.getValue(type);
        if (menu.containsKey(priceID))
            return menu.get(priceID);

        double price = 0;

        Integer flatPrice = flatPrices.get(type);
        price = flatPrice != null ? flatPrice : evaluateFormula(plugin.getConfigLoader().getPrice(type), blockCount);

        // Negative values aren't allowed.
        price = Math.max(0, price);
        menu.put(priceID, price);
        return price;
    }

    private boolean has(final OfflinePlayer player, final double amount)
    {
        try
        {
            return economy.has(player, amount);
        }
        catch(Exception e)
        {
            plugin.getMyLogger().logException(e, "Failed to check balance of player \"" + player.getName() +
                                              "\" (" + player.getUniqueId() + ")! Please contact pim16aap2!");
        }
        return true;
    }

    private boolean withdrawPlayer(final OfflinePlayer player, final String worldName, final double amount)
    {
        try
        {
            if (has(player, amount))
                return economy.withdrawPlayer(player, worldName, amount).type.equals(EconomyResponse.ResponseType.SUCCESS);
            return false;
        }
        catch(Exception e)
        {
            plugin.getMyLogger().logException(e, "Failed to subtract money from player \"" + player.getName() +
                                              "\" (" + player.getUniqueId() + ")! Please contact pim16aap2!");
        }
        return true;
    }

    private boolean withdrawPlayer(final Player player, final String worldName, final double amount)
    {
        return withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), worldName, amount);
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

    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    @Override
    public void restart()
    {
        menu.clear();
        flatPrices.clear();
        init();
    }
}
