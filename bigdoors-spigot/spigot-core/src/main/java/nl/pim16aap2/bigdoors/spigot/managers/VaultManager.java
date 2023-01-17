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
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Constants;
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
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Manages all interactions with Vault.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class VaultManager implements IRestartable, IEconomyManager, IPermissionsManager
{
    private final Map<MovableType, Double> flatPrices;
    private boolean economyEnabled = false;
    private boolean permissionsEnabled = false;
    private @Nullable Economy economy = null;
    private @Nullable Permission perms = null;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final IConfigLoader configLoader;
    private final MovableTypeManager movableTypeManager;

    @Inject
    public VaultManager(
        ILocalizer localizer, ITextFactory textFactory, IConfigLoader configLoader,
        MovableTypeManager movableTypeManager)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.configLoader = configLoader;
        this.movableTypeManager = movableTypeManager;

        flatPrices = new HashMap<>();
        if (isVaultInstalled())
        {
            economyEnabled = setupEconomy();
            permissionsEnabled = setupPermissions();
        }
    }

    @Override
    public boolean buyMovable(IPPlayer player, IPWorld world, MovableType type, int blockCount)
    {
        if (!economyEnabled)
            return true;

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            log.atSevere().withCause(
                new NullPointerException("Failed to obtain Spigot player: " + player.getUUID())).log();
            return false;
        }

        final OptionalDouble priceOpt = getPrice(type, blockCount);
        if (priceOpt.isEmpty())
            return true;

        final double price = priceOpt.getAsDouble();
        if (withdrawPlayer(spigotPlayer, world.worldName(), price))
        {
            player.sendInfo(textFactory, localizer.getMessage("creator.base.money_withdrawn", Double.toString(price)));
            return true;
        }

        player.sendError(textFactory,
                         localizer.getMessage("creator.base.error.insufficient_funds",
                                              localizer.getMessage(type.getLocalizationKey()), Double.toString(price)));
        return false;
    }

    @Override
    public boolean isEconomyEnabled()
    {
        return economyEnabled;
    }

    /**
     * Tries to get a flat price from the config for a {@link MovableType}. Useful in case the price is set to zero, so
     * the plugin won't have to parse the formula every time if it is disabled.
     *
     * @param type
     *     The {@link MovableType}.
     */
    private void getFlatPrice(MovableType type)
    {
        Util.parseDouble(configLoader.getPrice(type)).ifPresent(price -> flatPrices.put(type, price));
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
     *     The number of blocks in the movable.
     * @return The price of the movable given the formula and the blockCount variable.
     */
    private double evaluateFormula(String formula, int blockCount)
    {
        try
        {
            return JCalculator.getResult(formula, new String[]{"blockCount"}, new double[]{blockCount});
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e)
               .log("Failed to determine movable creation price! Please contact pim16aap2! "
                        + "Include this: '%s' and stacktrace:", formula);
            return 0.0d;
        }
    }

    @Override
    public OptionalDouble getPrice(MovableType type, int blockCount)
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
            log.atWarning().log("Economy not enabled! Could not subtract %f from the balance of player: %s!",
                                amount, player);
            return defaultValue;
        }

        try
        {
            return economy.has(player, amount);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e)
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
            log.atWarning()
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
            log.atSevere().withCause(e)
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
            log.atSevere().withCause(e).log();
            return false;
        }
    }

    /**
     * Initialize the "permissions" dependency. Assumes Vault is installed on this server. See
     * {@link #isVaultInstalled()}.
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
            log.atSevere().withCause(e).log();
            return false;
        }
    }

    @Override
    public void initialize()
    {
        for (final MovableType type : movableTypeManager.getEnabledMovableTypes())
            getFlatPrice(type);
    }

    @Override
    public void shutDown()
    {
        flatPrices.clear();
    }

    @Override
    public OptionalInt getMaxPermissionSuffix(IPPlayer player, String permissionBase)
    {
        final @Nullable Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            log.atSevere().withCause(
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
            log.atSevere().withCause(
                new IllegalArgumentException("Failed to obtain BukkitPlayer for player: " + player.asString())).log();
            return false;
        }

        return bukkitPlayer.hasPermission(permissionNode);
    }

    @Override
    public boolean hasBypassPermissionsForAttribute(IPPlayer player, MovableAttribute movableAttribute)
    {
        final @Nullable Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
            return false;

        return bukkitPlayer.isOp() ||
            bukkitPlayer.hasPermission(
                Constants.ATTRIBUTE_BYPASS_PERMISSION_PREFIX + movableAttribute.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isOp(IPPlayer player)
    {
        final @Nullable Player bukkitPlayer = getBukkitPlayer(player);
        return bukkitPlayer != null && bukkitPlayer.isOp();
    }

    private @Nullable Player getBukkitPlayer(IPPlayer player)
    {
        final @Nullable Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            log.atSevere().withCause(
                new IllegalArgumentException("Failed to obtain BukkitPlayer for player: " + player.asString())).log();
            return null;
        }
        return bukkitPlayer;
    }
}
