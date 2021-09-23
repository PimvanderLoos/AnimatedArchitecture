package nl.pim16aap2.bigdoors.spigot.managers;

import lombok.extern.flogger.Flogger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.jcalculator.JCalculator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages all interactions with Vault.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class VaultManager implements IRestartable, IEconomyManager, IPermissionsManager
{
    private final Map<DoorType, Double> flatPrices;
    private boolean economyEnabled = false;
    private boolean permissionsEnabled = false;
    private @Nullable Economy economy = null;
    private @Nullable Permission perms = null;
    private final ILocalizer localizer;
    private final IConfigLoader configLoader;
    private final DoorTypeManager doorTypeManager;

    @Inject
    public VaultManager(ILocalizer localizer, IConfigLoader configLoader, DoorTypeManager doorTypeManager)
    {
        this.localizer = localizer;
        this.configLoader = configLoader;
        this.doorTypeManager = doorTypeManager;

        flatPrices = new HashMap<>();
        if (isVaultInstalled())
        {
            economyEnabled = setupEconomy();
            permissionsEnabled = setupPermissions();
        }
    }

    @Override
    public boolean buyDoor(IPPlayer player, IPWorld world, DoorType type, int blockCount)
    {
        if (!economyEnabled)
            return true;

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            log.at(Level.SEVERE).withCause(
                new NullPointerException("Failed to obtain Spigot player: " + player.getUUID())).log();
            return false;
        }

        final OptionalDouble priceOpt = getPrice(type, blockCount);
        if (priceOpt.isEmpty())
            return true;

        final double price = priceOpt.getAsDouble();
        if (withdrawPlayer(spigotPlayer, world.worldName(), price))
        {
            player.sendMessage(localizer.getMessage("creator.base.money_withdrawn", Double.toString(price)));
            return true;
        }

        player.sendMessage(localizer.getMessage("creator.base.error.insufficient_funds", Double.toString(price)));
        return false;
    }

    @Override
    public boolean isEconomyEnabled()
    {
        return economyEnabled;
    }

    /**
     * Tries to get a flat price from the config for a {@link DoorType}. Useful in case the price is set to zero, so the
     * plugin won't have to parse the formula every time if it is disabled.
     *
     * @param type
     *     The {@link DoorType}.
     */
    private void getFlatPrice(DoorType type)
    {
        Util.parseDouble(configLoader.getPrice(type)).ifPresent(price -> flatPrices.put(type, price));
    }

    /**
     * Initializes the {@link VaultManager}.
     */
    private void init()
    {
        for (final DoorType type : doorTypeManager.getEnabledDoorTypes())
            getFlatPrice(type);
    }

    /**
     * Checks if a player has a specific permission node.
     *
     * @param player
     *     The player.
     * @param permission
     *     The permission node.
     * @return True if the player has the node.
     */
    public boolean hasPermission(Player player, String permission)
    {
        return permissionsEnabled && perms != null && perms.playerHas(player.getWorld().getName(), player, permission);
    }

    /**
     * Evaluates the price formula given a specific blockCount using {@link JCalculator}
     *
     * @param formula
     *     The formula of the price.
     * @param blockCount
     *     The number of blocks in the door.
     * @return The price of the door given the formula and the blockCount variable.
     */
    private double evaluateFormula(String formula, int blockCount)
    {
        try
        {
            return JCalculator.getResult(formula, new String[]{"blockCount"}, new double[]{blockCount});
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to determine door creation price! Please contact pim16aap2! "
                                                      + "Include this: '%s' and stacktrace:", formula);
            return 0.0d;
        }
    }

    @Override
    public OptionalDouble getPrice(DoorType type, int blockCount)
    {
        if (!economyEnabled)
            return OptionalDouble.empty();

        // TODO: Store flat prices as OptionalDoubles.
        final double price = flatPrices
            .getOrDefault(type, evaluateFormula(configLoader.getPrice(type), blockCount));

        return price <= 0 ? OptionalDouble.empty() : OptionalDouble.of(price);
    }

    /**
     * Checks if the player has a certain amount of money in their bank account.
     *
     * @param player
     *     The player whose bank account to check.
     * @param amount
     *     The amount of money.
     * @return True if the player has at least this much money.
     */
    private boolean has(OfflinePlayer player, double amount)
    {
        final boolean defaultValue = true;
        if (economy == null)
        {
            log.at(Level.WARNING).log("Economy not enabled! Could not subtract %f from the balance of player: %s!",
                                      amount, player);
            return defaultValue;
        }

        try
        {
            return economy.has(player, amount);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e)
               .log("Failed to check balance of player %s! Please contact pim16aap2!", player);
        }
        return defaultValue;
    }

    /**
     * Withdraw a certain amount of money from a player's bank account in a certain world.
     *
     * @param player
     *     The player.
     * @param worldName
     *     The name of the world.
     * @param amount
     *     The amount of money.
     * @return True if the money was successfully withdrawn from the player's accounts.
     */
    private boolean withdrawPlayer(OfflinePlayer player, String worldName, double amount)
    {
        final boolean defaultValue = true;
        if (economy == null)
        {
            log.at(Level.WARNING)
               .log("Economy not enabled! Could not subtract %f from the balance of player: %s in world: %s!",
                    amount, player, worldName);
            return defaultValue;
        }

        try
        {
            if (has(player, amount))
                return economy.withdrawPlayer(player, worldName, amount).type
                    .equals(EconomyResponse.ResponseType.SUCCESS);
            return false;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e)
               .log("Failed to subtract %f money from player %s! Please contact pim16aap2!", amount, player);
        }
        return defaultValue;
    }

    /**
     * Withdraw a certain amount of money from a player's bank account in a certain world.
     *
     * @param player
     *     The player.
     * @param worldName
     *     The name of the world.
     * @param amount
     *     The amount of money.
     * @return True if the money was successfully withdrawn from the player's accounts.
     */
    private boolean withdrawPlayer(Player player, String worldName, double amount)
    {
        return withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), worldName, amount);
    }

    /**
     * Checks if Vault is installed on this server.
     *
     * @return True if vault is installed on this server.
     */
    private boolean isVaultInstalled()
    {
        try
        {
            return Bukkit.getServer().getPluginManager().getPlugin("Vault") != null;
        }
        catch (NullPointerException e)
        {
            return false;
        }
    }

    /**
     * Initialize the economy dependency. Assumes Vault is installed on this server. See {@link #isVaultInstalled()}.
     *
     * @return True if the initialization process was successful.
     */
    private boolean setupEconomy()
    {
        try
        {
            final @Nullable RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

            if (economyProvider == null)
                return false;

            economy = economyProvider.getProvider();
            return true;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            return false;
        }
    }

    /**
     * Initialize the "permissions" dependency. Assumes Vault is installed on this server. See {@link
     * #isVaultInstalled()}.
     *
     * @return True if the initialization process was successful.
     */
    private boolean setupPermissions()
    {
        try
        {
            final @Nullable RegisteredServiceProvider<Permission> permissionProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

            if (permissionProvider == null)
                return false;

            perms = permissionProvider.getProvider();
            return true;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            return false;
        }
    }

    @Override
    public void restart()
    {
        shutdown();
        init();
    }

    @Override
    public void shutdown()
    {
        flatPrices.clear();
    }

    @Override
    public OptionalInt getMaxPermissionSuffix(IPPlayer player, String permissionBase)
    {
        final @Nullable Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            log.at(Level.SEVERE).withCause(
                new IllegalArgumentException("Failed to obtain BukkitPlayer for player: " + player.asString())).log();
            return OptionalInt.empty();
        }

        final int permissionBaseLength = permissionBase.length();
        final Set<PermissionAttachmentInfo> playerPermissions = bukkitPlayer.getEffectivePermissions();
        int ret = -1;
        for (final PermissionAttachmentInfo permission : playerPermissions)
            if (permission.getPermission().startsWith(permissionBase))
            {
                final OptionalInt suffix = Util.parseInt(permission.getPermission().substring(permissionBaseLength));
                if (suffix.isPresent())
                    ret = Math.max(ret, suffix.getAsInt());
            }
        return ret > 0 ? OptionalInt.of(ret) : OptionalInt.empty();
    }

    @Override
    public boolean hasPermission(IPPlayer player, String permissionNode)
    {
        final @Nullable Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            log.at(Level.SEVERE).withCause(
                new IllegalArgumentException("Failed to obtain BukkitPlayer for player: " + player.asString())).log();
            return false;
        }

        return bukkitPlayer.hasPermission(permissionNode);
    }

    @Override
    public boolean isOp(IPPlayer player)
    {
        final @Nullable Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            log.at(Level.SEVERE).withCause(
                new IllegalArgumentException("Failed to obtain BukkitPlayer for player: " + player.asString())).log();
            return false;
        }
        return bukkitPlayer.isOp();
    }
}
