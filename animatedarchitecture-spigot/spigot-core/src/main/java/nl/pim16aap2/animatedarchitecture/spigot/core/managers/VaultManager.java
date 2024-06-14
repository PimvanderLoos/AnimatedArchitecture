package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IFakePlayer;
import nl.pim16aap2.jcalculator.JCalculator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Manages all interactions with Vault.
 */
@Singleton
@Flogger
public final class VaultManager implements IRestartable, IEconomyManager, IPermissionsManagerSpigot, IDebuggable
{
    private final Map<StructureType, Double> flatPrices;
    private final Permission perms;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final IConfig config;
    private final StructureTypeManager structureTypeManager;
    private final IExecutor executor;

    private boolean economyEnabled = false;
    private @Nullable Economy economy = null;

    @Inject
    public VaultManager(
        ILocalizer localizer,
        ITextFactory textFactory,
        IConfig config,
        StructureTypeManager structureTypeManager,
        DebuggableRegistry debuggableRegistry,
        IExecutor executor)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.config = config;
        this.structureTypeManager = structureTypeManager;
        this.executor = executor;

        flatPrices = new HashMap<>();
        economyEnabled = setupEconomy();
        perms = setupPermissions();

        debuggableRegistry.registerDebuggable(this);
    }

    @Override
    public boolean buyStructure(IPlayer player, IWorld world, StructureType type, int blockCount)
    {
        if (!economyEnabled)
            return true;

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Failed to obtain Spigot player: '%s'", player.getUUID());
            return false;
        }

        final OptionalDouble priceOpt = getPrice(type, blockCount);
        if (priceOpt.isEmpty())
            return true;

        final double price = priceOpt.getAsDouble();
        if (withdrawPlayer(spigotPlayer, world.worldName(), price))
        {
            player.sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.money_withdrawn"),
                TextType.SUCCESS,
                arg -> arg.highlight(price))
            );
            return true;
        }

        player.sendMessage(textFactory.newText().append(
            localizer.getMessage("creator.base.error.insufficient_funds"),
            TextType.ERROR,
            arg -> arg.highlight(localizer.getMessage(type.getLocalizationKey())),
            arg -> arg.highlight(price))
        );

        log.atFine().log(
            "Player '%s' does not have enough money to buy structure of type '%s' of size %d! Price: %f",
            player.asString(),
            type.getSimpleName(),
            blockCount,
            price
        );

        return false;
    }

    @Override
    public boolean isEconomyEnabled()
    {
        return economyEnabled;
    }

    /**
     * Tries to get a flat price from the config for a {@link StructureType}. Useful in case the price is set to zero,
     * so the plugin won't have to parse the formula every time if it is disabled.
     *
     * @param type
     *     The {@link StructureType}.
     */
    private void getFlatPrice(StructureType type)
    {
        Util.parseDouble(config.getPrice(type)).ifPresent(price -> flatPrices.put(type, price));
    }

    @Override
    public boolean hasPermission(Player player, String permission)
    {
        if (executor.isMainThread() &&
            (player instanceof IFakePlayer || !player.isOnline()))
        {
            throw new RuntimeException(String.format(
                "Failed to check permission '%s' for player '%s'! " +
                    "Cannot check permissions for offline players on the main thread! Online: %b, Fake: %b",
                permission,
                player.getName(),
                player.isOnline(),
                player instanceof IFakePlayer)
            );
        }

        final boolean result = perms.playerHas(player.getWorld().getName(), player, permission);
        log.atFine().log("Player '%s' has permission '%s': %b", player.getName(), permission, result);
        return result;
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(World world, OfflinePlayer player, String permission)
    {
        return CompletableFuture.supplyAsync(() -> perms.playerHas(world.getName(), player, permission));
    }

    /**
     * Evaluates the price formula given a specific blockCount using {@link JCalculator}
     *
     * @param formula
     *     The formula of the price.
     * @param blockCount
     *     The number of blocks in the structure.
     * @return The price of the structure given the formula and the blockCount variable.
     */
    private double evaluateFormula(String formula, int blockCount)
    {
        try
        {
            return JCalculator.getResult(formula, new String[]{"blockCount"}, new double[]{blockCount});
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to determine structure creation price! Please contact pim16aap2! "
                    + "Include this: '%s' and stacktrace:",
                formula
            );
            return 0.0d;
        }
    }

    @Override
    public OptionalDouble getPrice(StructureType type, int blockCount)
    {
        if (!economyEnabled)
            return OptionalDouble.empty();

        // TODO: Store flat prices as OptionalDoubles.
        final double price = flatPrices.getOrDefault(type, evaluateFormula(config.getPrice(type), blockCount));

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
            log.atWarning().log(
                "Economy not enabled! Could not subtract %f from the balance of player: %s!",
                amount,
                player
            );
            return defaultValue;
        }

        try
        {
            return economy.has(player, amount);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to check balance of player %s! Please contact pim16aap2!", player);
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
            log.atWarning().log(
                "Economy not enabled! Could not subtract %f from the balance of player: %s in world: %s!",
                amount,
                player,
                worldName
            );
            return defaultValue;
        }

        try
        {
            if (has(player, amount))
                return economy
                    .withdrawPlayer(player, worldName, amount)
                    .type
                    .equals(EconomyResponse.ResponseType.SUCCESS);
            return false;
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to subtract %f money from player %s! Please contact pim16aap2!",
                amount,
                player
            );
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
     * Initialize the economy dependency. Assumes Vault is installed on this server.
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
     * Initialize the "permissions" dependency. Assumes Vault is installed on this server.
     *
     * @return True if the initialization process was successful.
     */
    private Permission setupPermissions()
    {
        try
        {
            final RegisteredServiceProvider<Permission> permissionProvider =
                Objects.requireNonNull(Bukkit.getServer().getServicesManager().getRegistration(Permission.class));

            return Objects.requireNonNull(permissionProvider.getProvider());
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to initialize permissions!", e);
        }
    }

    @Override
    public void initialize()
    {
        for (final StructureType type : structureTypeManager.getEnabledStructureTypes())
            getFlatPrice(type);
    }

    @Override
    public void shutDown()
    {
        flatPrices.clear();
    }

    @Override
    public OptionalInt getMaxPermissionSuffix(Player player, String permissionBase)
    {
        final int permissionBaseLength = permissionBase.length();
        final Set<PermissionAttachmentInfo> playerPermissions = player.getEffectivePermissions();
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
    public OptionalInt getMaxPermissionSuffix(IPlayer player, String permissionBase)
    {
        final @Nullable Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
            return OptionalInt.empty();
        return getMaxPermissionSuffix(bukkitPlayer, permissionBase);
    }

    @Override
    public boolean hasPermission(IPlayer player, String permissionNode)
    {
        final @Nullable Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
            return false;

        return bukkitPlayer.hasPermission(permissionNode);
    }

    @Override
    public boolean hasBypassPermissionsForAttribute(Player player, StructureAttribute structureAttribute)
    {
        return player.isOp() || player.hasPermission(structureAttribute.getAdminPermissionNode());
    }

    @Override
    public boolean hasBypassPermissionsForAttribute(IPlayer player, StructureAttribute structureAttribute)
    {
        final @Nullable Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null)
            return false;
        return hasBypassPermissionsForAttribute(bukkitPlayer, structureAttribute);
    }

    @Override
    public boolean isOp(@Nullable Player player)
    {
        return player != null && player.isOp();
    }

    @Override
    public boolean isOp(IPlayer player)
    {
        return isOp(getBukkitPlayer(player));
    }

    @Override
    public boolean hasPermissionToCreateStructure(ICommandSender sender, StructureType type)
    {
        return sender
            .getPlayer()
            .map(player -> hasPermission(player, type.getCreationPermission()))
            .orElse(true);
    }

    private @Nullable Player getBukkitPlayer(IPlayer player)
    {
        final @Nullable Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (bukkitPlayer == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Failed to obtain BukkitPlayer for player: '%s'",
                player.asString()
            );
            return null;
        }
        return bukkitPlayer;
    }

    @Override
    public String getDebugInformation()
    {
        return "Economy Enabled: " + economyEnabled + "\n"
            + "Flat prices map: " + flatPrices;
    }
}
