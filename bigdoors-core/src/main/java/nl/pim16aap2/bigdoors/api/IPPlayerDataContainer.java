package nl.pim16aap2.bigdoors.api;

import lombok.Getter;
import nl.pim16aap2.bigdoors.util.IBitFlag;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Represents a container for player data.
 *
 * @author Pim
 */
interface IPPlayerDataContainer
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
     * Gets the limit for the size of structures this player can create/operate based on their permission.
     * <p>
     * Note that this does not take the global limit into account.
     *
     * @return The maximum structure size this player can make/operate measured in number of blocks.
     */
    int getStructureSizeLimit();

    /**
     * Gets the limit for the number of structures this player can own based on their permission.
     * <p>
     * Note that this does not take the global limit into account.
     *
     * @return The number of structures this player can own.
     */
    int getStructureCountLimit();

    /**
     * Checks if this player is an OP or not.
     *
     * @return True if the player is an OP.
     */
    boolean isOp();

    /**
     * Gets a the {@link PPlayerData} that represents this player. This may or may not be a new object.
     *
     * @return The {@link PPlayerData} that represents this player.
     */
    default PPlayerData getPPlayerData()
    {
        return new PPlayerData(getUUID(), getName(), getStructureSizeLimit(), getStructureCountLimit(),
                               isOp(), hasProtectionBypassPermission());
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
        OP(1, IPPlayerDataContainer::isOp),
        BYPASS(2, IPPlayerDataContainer::hasProtectionBypassPermission),
        ;

        @Getter
        private static final List<PermissionFlag> VALUES = List.of(PermissionFlag.values());

        private final int val;
        private final Function<IPPlayerDataContainer, Boolean> fun;

        PermissionFlag(int val, Function<IPPlayerDataContainer, Boolean> fun)
        {
            this.val = val;
            this.fun = fun;
        }

        public static long setFlag(IPPlayerDataContainer playerDataContainer, long currentValue, PermissionFlag flag)
        {
            final boolean result = flag.fun.apply(playerDataContainer);
            return result ? IBitFlag.setFlag(flag.val, currentValue) : IBitFlag.unsetFlag(flag.val, currentValue);
        }

        public static boolean hasFlag(PermissionFlag flag, long currentValue)
        {
            return IBitFlag.hasFlag(flag.val, currentValue);
        }
    }
}
