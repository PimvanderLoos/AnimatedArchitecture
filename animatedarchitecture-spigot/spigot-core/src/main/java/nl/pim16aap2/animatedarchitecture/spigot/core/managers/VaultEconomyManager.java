package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import com.google.common.flogger.StackSize;
import lombok.CustomLog;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigot;
import nl.pim16aap2.jcalculator.JCalculator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Handles Vault economy integration for structure purchase costs.
 */
@CustomLog
public final class VaultEconomyManager implements IRestartable, IEconomyManager, IDebuggable
{
    private final Map<StructureType, Double> flatPrices = new HashMap<>();
    private final IConfig config;
    private final StructureTypeManager structureTypeManager;
    private final @Nullable Economy economy;

    /**
     * Creates a Vault economy manager.
     *
     * @param config
     *     The plugin configuration.
     * @param structureTypeManager
     *     The manager that exposes enabled structure types.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @param restartableHolder
     *     The holder that manages restartable services.
     */
    public VaultEconomyManager(
        IConfig config,
        StructureTypeManager structureTypeManager,
        DebuggableRegistry debuggableRegistry,
        RestartableHolder restartableHolder)
    {
        this.config = config;
        this.structureTypeManager = structureTypeManager;
        this.economy = setupEconomy();

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    @Override
    public boolean buyStructure(IPlayer player, IWorld world, StructureType type, int blockCount)
    {
        if (!isEconomyEnabled())
            return true;

        final Player spigotPlayer = PlayerFactorySpigot.unwrapPlayer(player);
        if (spigotPlayer == null)
        {
            log.atError().withStackTrace(StackSize.FULL).log("Failed to obtain Spigot player: '%s'", player.getUUID());
            return false;
        }

        final OptionalDouble priceOpt = getPrice(type, blockCount);
        if (priceOpt.isEmpty())
            return true;

        final double price = priceOpt.getAsDouble();
        if (withdrawPlayer(spigotPlayer, world.worldName(), price))
        {
            player.sendSuccess(
                "creator.base.money_withdrawn",
                arg -> arg.highlight(price)
            );
            return true;
        }

        player.sendError(
            "creator.base.error.insufficient_funds",
            arg -> arg.localizedHighlight(type),
            arg -> arg.highlight(price)
        );

        log.atDebug().log(
            "Player '%s' does not have enough money to buy structure of type '%s' of size %d! Price: %f",
            player.asString(),
            type.getKey(),
            blockCount,
            price
        );

        return false;
    }

    @Override
    public boolean isEconomyEnabled()
    {
        return economy != null;
    }

    @Override
    public OptionalDouble getPrice(StructureType type, int blockCount)
    {
        if (!isEconomyEnabled())
            return OptionalDouble.empty();

        final double price = flatPrices.getOrDefault(type, evaluateFormula(config.priceFormula(type), blockCount));
        return price <= 0 ? OptionalDouble.empty() : OptionalDouble.of(price);
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

    /**
     * Tries to cache a flat structure price from the configuration.
     *
     * @param type
     *     The structure type to cache.
     */
    private void getFlatPrice(StructureType type)
    {
        MathUtil.parseDouble(config.priceFormula(type)).ifPresent(price -> flatPrices.put(type, price));
    }

    /**
     * Evaluates a price formula for the given block count.
     *
     * @param formula
     *     The formula to evaluate.
     * @param blockCount
     *     The number of blocks in the structure.
     * @return The evaluated price.
     */
    private double evaluateFormula(String formula, int blockCount)
    {
        try
        {
            return JCalculator.getResult(formula, new String[]{"blockCount"}, new double[]{blockCount});
        }
        catch (Exception e)
        {
            log.atError().withCause(e).log(
                "Failed to determine structure creation priceFormula! Please contact pim16aap2! "
                    + "Include this: '%s' and stacktrace:",
                formula
            );
            return 0.0d;
        }
    }

    /**
     * Checks if a player has a minimum balance.
     *
     * @param player
     *     The player whose balance to check.
     * @param amount
     *     The required balance.
     * @return True if the player has enough money.
     */
    private boolean has(OfflinePlayer player, double amount)
    {
        final boolean defaultValue = true;
        if (economy == null)
        {
            log.atWarn().log(
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
            log.atError().withCause(e).log("Failed to check balance of player %s! Please contact pim16aap2!", player);
        }
        return defaultValue;
    }

    /**
     * Withdraws money from a player's balance in a world.
     *
     * @param player
     *     The player to charge.
     * @param worldName
     *     The world name for the transaction.
     * @param amount
     *     The amount to withdraw.
     * @return True if the withdrawal succeeded.
     */
    private boolean withdrawPlayer(OfflinePlayer player, String worldName, double amount)
    {
        final boolean defaultValue = true;
        if (economy == null)
        {
            log.atWarn().log(
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
                    .type == EconomyResponse.ResponseType.SUCCESS;
            return false;
        }
        catch (Exception e)
        {
            log.atError().withCause(e).log(
                "Failed to subtract %f money from player %s! Please contact pim16aap2!",
                amount,
                player
            );
        }
        return defaultValue;
    }

    /**
     * Withdraws money from a player's balance in a world.
     *
     * @param player
     *     The player to charge.
     * @param worldName
     *     The world name for the transaction.
     * @param amount
     *     The amount to withdraw.
     * @return True if the withdrawal succeeded.
     */
    private boolean withdrawPlayer(Player player, String worldName, double amount)
    {
        return withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), worldName, amount);
    }

    /**
     * Initializes the Vault economy provider.
     *
     * @return The Vault economy provider, or null when unavailable.
     */
    private @Nullable Economy setupEconomy()
    {
        try
        {
            final RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

            if (economyProvider == null)
            {
                log.atInfo().log("Vault is installed, but no Vault economy provider is registered.");
                return null;
            }

            final Economy provider = economyProvider.getProvider();
            log.atInfo().log("Using Vault economy provider: %s", provider.getName());
            return provider;
        }
        catch (Exception e)
        {
            log.atError().withCause(e).log("Failed to initialize Vault economy!");
            return null;
        }
    }

    @Override
    public String getDebugInformation()
    {
        return "Economy backend: Vault\n"
            + "Vault economy provider: " + (economy == null ? "Disabled" : economy.getName()) + "\n"
            + "Flat prices map: " +
            StringUtil.formatCollection(
                flatPrices.entrySet(),
                entry -> String.format("%s: %f", entry.getKey(), entry.getValue())
            );
    }
}
