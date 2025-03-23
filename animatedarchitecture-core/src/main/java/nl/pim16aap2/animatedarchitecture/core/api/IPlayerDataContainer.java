package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.util.IBitFlag;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Represents a container for player data.
 */
interface IPlayerDataContainer
{
    /**
     * Gets the name of this player.
     *
     * @return The name of this player.
     */
    String getName();

    /**
     * Gets the UUID of this player.
     *
     * @return The UUID of this player.
     */
    UUID getUUID();

    /**
     * Gets the player as a String in the format '"playerName" (playerUUID)'.
     *
     * @return The player as a String.
     */
    default String asString()
    {
        return String.format("'%s' (%s)", getName(), getUUID());
    }

    /**
     * Checks if this player has a bypass permission node that allows them to bypass the protection hooks.
     *
     * @return True if this player has permission to bypass the protection hooks.
     */
    boolean hasProtectionBypassPermission();

    /**
     * Gets the value of the {@link Limit} for this player.
     * <p>
     * Note that this does not take the global limit into account.
     *
     * @param limit
     *     The {@link Limit} to get the value of.
     * @return The value of the limit for this player.
     */
    OptionalInt getLimit(Limit limit);

    /**
     * Checks if this player is an OP or not.
     *
     * @return True if the player is an OP.
     */
    boolean isOp();

    /**
     * Gets a the {@link PlayerData} that represents this player. This may or may not be a new object.
     *
     * @return The {@link PlayerData} that represents this player.
     */
    default PlayerData getPlayerData()
    {
        return new PlayerData(
            getUUID(),
            getName(),
            LimitContainer.of(this),
            isOp(),
            hasProtectionBypassPermission()
        );
    }

    default long getPermissionsFlag()
    {
        long ret = 0;
        for (final PermissionFlag flag : PermissionFlag.getVALUES())
            ret = PermissionFlag.setFlag(this, ret, flag);
        return ret;
    }

    /**
     * Enum containing all binary entries to pack into a single flag.
     */
    enum PermissionFlag
    {
        OP(1, IPlayerDataContainer::isOp),
        BYPASS(2, IPlayerDataContainer::hasProtectionBypassPermission),
        ;

        @Getter
        private static final List<PermissionFlag> VALUES = List.of(PermissionFlag.values());

        private final int val;
        private final Predicate<IPlayerDataContainer> fun;

        PermissionFlag(int val, Predicate<IPlayerDataContainer> fun)
        {
            this.val = val;
            this.fun = fun;
        }

        static long setFlag(IPlayerDataContainer playerDataContainer, long currentValue, PermissionFlag flag)
        {
            final boolean result = flag.fun.test(playerDataContainer);
            return result ? IBitFlag.setFlag(flag.val, currentValue) : IBitFlag.unsetFlag(flag.val, currentValue);
        }

        public static boolean hasFlag(PermissionFlag flag, long currentValue)
        {
            return IBitFlag.hasFlag(flag.val, currentValue);
        }
    }
}
