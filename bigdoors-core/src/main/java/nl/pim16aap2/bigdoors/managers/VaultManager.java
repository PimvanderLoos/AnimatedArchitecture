package nl.pim16aap2.bigdoors.managers;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.jcalculator.JCalculator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all interactions with Vault.
 *
 * @author Pim
 */
public class VaultManager extends Restartable
{
    protected final BigDoors plugin;
    private final Map<Long, Double> menu;
    private final Map<DoorType, Double> flatPrices;
    private final boolean vaultEnabled;
    private Economy economy = null;
    private Permission perms = null;

    public VaultManager(final @NotNull BigDoors plugin)
    {
        super(plugin);
        this.plugin = plugin;
        menu = new HashMap<>();
        flatPrices = new EnumMap<>(DoorType.class);
        init();
        vaultEnabled = setupEconomy();
        if (vaultEnabled)
            setupPermissions();
    }

    /**
     * Buys a door for a player.
     *
     * @param player     The player whose bank account to use.
     * @param type       The {@link DoorType} of the door.
     * @param blockCount The number of blocks in the door.
     * @return True if the player bought the door successfully.
     */
    public boolean buyDoor(final @NotNull Player player, final @NotNull DoorType type, final int blockCount)
    {
        if (!vaultEnabled)
            return true;
        double price = getPrice(type, blockCount);
        if (withdrawPlayer(player, player.getWorld().getName(), price))
        {
            if (price > 0)
                SpigotUtil.messagePlayer(player,
                                         plugin.getMessages().getString(Message.CREATOR_GENERAL_MONEYWITHDRAWN,
                                                                        Double.toString(price)));
            return true;
        }

        SpigotUtil.messagePlayer(player,
                                 plugin.getMessages().getString(Message.CREATOR_GENERAL_INSUFFICIENTFUNDS,
                                                                Double.toString(price)));
        return false;
    }

    /**
     * Tries to get a flat price from the config for a {@link DoorType}. Useful in case the price is set to zero, so the
     * plugin won't have to parse the formula every time if it is disabled.
     *
     * @param type The {@link DoorType}.
     */
    private void getFlatPrice(final @NotNull DoorType type)
    {
        Double price;
        try
        {
            price = Double.parseDouble(plugin.getConfigLoader().getPrice(DoorType.BIGDOOR));
            flatPrices.put(type, price);
        }
        catch (Exception unhandled)
        {
        }
    }

    /**
     * Initializes the {@link VaultManager}.
     */
    private void init()
    {
        for (DoorType type : DoorType.cachedValues())
            getFlatPrice(type);
    }

    /**
     * Checks if a player has a specific permission node.
     *
     * @param player     The player.
     * @param permission The permission node.
     * @return True if the player has the node.
     */
    public boolean hasPermission(final @NotNull Player player, final @NotNull String permission)
    {
        return vaultEnabled && perms.playerHas(player.getWorld().getName(), player, permission);
    }

    /**
     * Evaluates the price formula given a specific blockCount using {@link JCalculator}
     *
     * @param formula    The formula of the price.
     * @param blockCount The number of blocks in the door.
     * @return The price of the door given the formula and the blockCount variabel.
     */
    private double evaluateFormula(final @NotNull String formula, final int blockCount)
    {
        try
        {
            return JCalculator.getResult(formula, new String[]{"blockCount"}, new double[]{blockCount});
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e, "Failed to determine door creation price! Please contact pim16aap2! "
                + "Include this: \"" + formula + "\" and this:");
            return 0.0d;
        }
    }

    /**
     * Gets the price of {@link DoorType} for a specific number of blocks.
     *
     * @param type       The {@link DoorType}.
     * @param blockCount The number of blocks.
     * @return The price of this {@link DoorType} with this number of blocks.
     */
    public double getPrice(final @NotNull DoorType type, final int blockCount)
    {
        if (!vaultEnabled)
            return 0;

        // Try cache first
        long priceID = blockCount * 100 + DoorType.getValue(type);
        if (menu.containsKey(priceID))
            return menu.get(priceID);

        double price = flatPrices
            .getOrDefault(type, evaluateFormula(plugin.getConfigLoader().getPrice(type), blockCount));

        // Negative values aren't allowed.
        price = Math.max(0, price);
        menu.put(priceID, price);
        return price;
    }

    /**
     * Checks if the player has a certain amount of money in their bank account.
     *
     * @param player The player whose bank account to check.
     * @param amount The amount of money.
     * @return True if the player has at least this much money.
     */
    private boolean has(final @NotNull OfflinePlayer player, final double amount)
    {
        try
        {
            return economy.has(player, amount);
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e, "Failed to check balance of player \"" + player.getName() +
                "\" (" + player.getUniqueId() + ")! Please contact pim16aap2!");
        }
        return true;
    }

    /**
     * Withdraw a certain amount of money from a player's bank account in a certain world.
     *
     * @param player    The player.
     * @param worldName The name of the world.
     * @param amount    The amount of money.
     * @return True if the money was successfully withdrawn from the player's accounts.
     */
    private boolean withdrawPlayer(final @NotNull OfflinePlayer player, final @NotNull String worldName,
                                   final double amount)
    {
        try
        {
            if (has(player, amount))
                return economy.withdrawPlayer(player, worldName, amount).type
                    .equals(EconomyResponse.ResponseType.SUCCESS);
            return false;
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e, "Failed to subtract money from player \"" + player.getName() +
                "\" (" + player.getUniqueId() + ")! Please contact pim16aap2!");
        }
        return true;
    }

    /**
     * Withdraw a certain amount of money from a player's bank account in a certain world.
     *
     * @param player    The player.
     * @param worldName The name of the world.
     * @param amount    The amount of money.
     * @return True if the money was successfully withdrawn from the player's accounts.
     */
    private boolean withdrawPlayer(final @NotNull Player player, final @NotNull String worldName, final double amount)
    {
        return withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), worldName, amount);
    }

    /**
     * Initialize the economy dependency.
     *
     * @return True if the initialization process was successful.
     */
    private boolean setupEconomy()
    {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(
            net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            economy = economyProvider.getProvider();

        return (economy != null);
    }

    /**
     * Initialize the permissions dependency.
     *
     * @return True if the initialization process was successful.
     */
    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager()
                                                          .getRegistration(Permission.class);
        if (rsp == null)
            return false;

        perms = rsp.getProvider();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        shutdown();
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        menu.clear();
        flatPrices.clear();
    }
}
