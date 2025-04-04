package nl.pim16aap2.animatedarchitecture.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.OptionalInt;

/**
 * Manages the limits of players.
 * <p>
 * It checks the global limit, any admin bypass permission, and the player's personal limit.
 */
@Singleton
public class LimitsManager
{
    private final IPermissionsManager permissionsManager;
    private final IConfig config;

    @Inject
    public LimitsManager(IPermissionsManager permissionsManager, IConfig config)
    {
        this.permissionsManager = permissionsManager;
        this.config = config;
    }

    /**
     * Gets the value of the {@link Limit} for the given player. It checks the global limit, any admin bypass
     * permission, and the player's personal limit.
     *
     * @param player
     *     The player for whom to check the limit.
     * @param limit
     *     The {@link Limit} to check.
     * @return The value of the limit for the given player, or an empty OptionalInt if none was found.
     * <p>
     * If there is a global limit in place (see {@link Limit#getGlobalLimit(IConfig)}), the returned value cannot exceed
     * this. Not even admins and OPs can bypass this limit.
     * <p>
     * If the player has a player limit as well as a global limit, the lowest value of the two will be used.
     */
    public OptionalInt getLimit(IPlayer player, Limit limit)
    {
        final boolean hasBypass =
            player.isOnline() && permissionsManager.hasPermission(player, limit.getAdminPermission());

        final OptionalInt globalLimit = filterNegative(limit.getGlobalLimit(config));
        if (hasBypass)
            return globalLimit;

        final OptionalInt playerLimit = filterNegative(
            player.isOnline() ?
                permissionsManager.getMaxPermissionSuffix(player, limit.getUserPermission()) :
                player.getLimit(limit)
        );

        if (globalLimit.isPresent() && playerLimit.isPresent())
            return OptionalInt.of(Math.min(globalLimit.getAsInt(), playerLimit.getAsInt()));

        return globalLimit.isPresent() ? OptionalInt.of(globalLimit.getAsInt()) :
            playerLimit.isPresent() ? OptionalInt.of(playerLimit.getAsInt()) :
                OptionalInt.empty();
    }

    /**
     * Filters out negative values from the given {@link OptionalInt}.
     *
     * @param optionalInt
     *     The {@link OptionalInt} to filter.
     * @return The filtered {@link OptionalInt}. If the value was negative, this will return an empty
     * {@link OptionalInt}. If the value was positive or zero, this will return the original {@link OptionalInt}.
     */
    private static OptionalInt filterNegative(OptionalInt optionalInt)
    {
        return optionalInt.isPresent() && optionalInt.getAsInt() < 0 ? OptionalInt.empty() : optionalInt;
    }

    /**
     * Checks if a given value exceeds the limit for this player. For more info, see {@link #getLimit(IPlayer, Limit)}.
     *
     * @param player
     *     The player for whom to check the limit.
     * @param limit
     *     The {@link Limit} to check.
     * @param value
     *     The value to compare to the limit.
     * @return True if the given value exceeds the limit for this player. If value &lt;= limit, this will return false.
     */
    public boolean exceedsLimit(IPlayer player, Limit limit, int value)
    {
        final OptionalInt limitValue = getLimit(player, limit);
        return limitValue.isPresent() && value > limitValue.getAsInt();
    }
}
